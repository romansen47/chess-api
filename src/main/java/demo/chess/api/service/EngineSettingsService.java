package demo.chess.api.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineConfigStoreDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileAssignmentsDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.service.EngineSettingsRegistry.ManagedEngineDefinition;
import demo.chess.api.service.EngineSettingsRegistry.ManagedProfile;
import demo.chess.definitions.engines.EngineConfigType;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciEngineInspector;

/** Coordinates engine/profile CRUD, assignments, runtime versions and persistence. */
@Service
public class EngineSettingsService {

    private static final Log logger = LogFactory.getLog(EngineSettingsService.class);
    private final EngineDiscoveryService engineDiscoveryService;
    private final String defaultEnginePath;
    private final EngineSettingsRegistry registry = new EngineSettingsRegistry();
    private final EngineSettingsRepository store;
    private final LegacyEngineSettingsMigrator legacyMigrator = new LegacyEngineSettingsMigrator();
    private final EngineSettingsVersions versions = new EngineSettingsVersions();
    private final LinkedHashMap<String, ManagedEngineDefinition> engines = registry.engines();
    private final LinkedHashMap<String, ManagedProfile> profiles = registry.profiles();

    private String fallbackProfileId;
    private String defaultWhitePlayerProfileId;
    private String defaultBlackPlayerProfileId;
    private String defaultEvaluationProfileId;
    private String defaultDeepAnalysisProfileId;

    public EngineSettingsService(ObjectMapper objectMapper, EngineDiscoveryService engineDiscoveryService) {
        this(new EngineSettingsStore(objectMapper), engineDiscoveryService);
    }

    EngineSettingsService(
            EngineSettingsRepository store,
            EngineDiscoveryService engineDiscoveryService) {
        this.store = Objects.requireNonNull(store, "store");
        this.engineDiscoveryService = Objects.requireNonNull(
                engineDiscoveryService,
                "engineDiscoveryService");
        this.defaultEnginePath = engineDiscoveryService.getPreferredEnginePath();

        boolean storedConfigurationLoaded = loadStore();

        // Automatic discovery is a first-install and recovery convenience.
        // A successfully loaded configuration, even an intentionally empty one,
        // is a persisted user choice and must survive application restarts.
        if (!storedConfigurationLoaded && engines.isEmpty() && profiles.isEmpty()) {
            discoverSystemEnginesInternal();
        }

        repairFallbackAndAssignments();
        persistStore();
    }

    public synchronized EngineConfigOverviewDto resetToFallbackDefaults() {
        engines.clear();
        profiles.clear();

        fallbackProfileId = null;
        defaultWhitePlayerProfileId = null;
        defaultBlackPlayerProfileId = null;
        defaultEvaluationProfileId = null;
        defaultDeepAnalysisProfileId = null;

        discoverSystemEnginesInternal();
        repairFallbackAndAssignments();

        versions.allRuntimeRolesChanged();

        persistStore();
        logger.info("Reset engine settings and rediscovered system UCI engines in "
                + engineDiscoveryService.getDiscoveryDirectory());
        return getOverview();
    }

    public synchronized EngineConfigOverviewDto discoverSystemEngines() {
        String previousWhite = defaultWhitePlayerProfileId;
        String previousBlack = defaultBlackPlayerProfileId;
        String previousEvaluation = defaultEvaluationProfileId;

        int addedEngines = discoverSystemEnginesInternal();
        if (addedEngines > 0) {
            repairFallbackAndAssignments();

            versions.configurationChanged(
                    !Objects.equals(previousWhite, defaultWhitePlayerProfileId),
                    !Objects.equals(previousBlack, defaultBlackPlayerProfileId),
                    !Objects.equals(previousEvaluation, defaultEvaluationProfileId));
            persistStore();
        }
        return getOverview();
    }

    public synchronized List<EngineDefinitionDto> discoverEngineCandidates() {
        return engineDiscoveryService.discover().stream()
                .filter(definition -> findEngineByPath(definition.getEngine()) == null)
                .map(definition -> {
                    EngineDefinitionDto dto = toDto(new ManagedEngineDefinition(
                            null,
                            definition.getEngineName(),
                            definition));
                    dto.setId(null);
                    return dto;
                })
                .sorted(Comparator.comparing(EngineDefinitionDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized EngineConfigOverviewDto getOverview() {
        List<EngineDefinitionDto> engineDtos = engines.values().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(EngineDefinitionDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<EngineProfileDto> profileDtos = profiles.values().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(EngineProfileDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new EngineConfigOverviewDto(
                engineDtos,
                profileDtos,
                currentAssignments(),
                fallbackProfileId,
                versions.configuration());
    }

    public synchronized EngineProfileAssignmentsDto updateDefaultAssignments(EngineProfileAssignmentsDto incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Default profile assignments must not be null");
        }

        String white = resolveProfileId(incoming.getWhitePlayerProfileId(), fallbackProfileId);
        String black = resolveProfileId(incoming.getBlackPlayerProfileId(), fallbackProfileId);
        String evaluation = resolveProfileId(incoming.getEvaluationProfileId(), fallbackProfileId);
        String deepAnalysis = resolveProfileId(incoming.getDeepAnalysisProfileId(), fallbackProfileId);

        boolean whiteChanged = !Objects.equals(white, defaultWhitePlayerProfileId);
        boolean blackChanged = !Objects.equals(black, defaultBlackPlayerProfileId);
        boolean evaluationChanged = !Objects.equals(evaluation, defaultEvaluationProfileId);
        boolean deepAnalysisChanged = !Objects.equals(deepAnalysis, defaultDeepAnalysisProfileId);
        if (whiteChanged) {
            defaultWhitePlayerProfileId = white;
        }
        if (blackChanged) {
            defaultBlackPlayerProfileId = black;
        }
        if (evaluationChanged) {
            defaultEvaluationProfileId = evaluation;
        }
        if (deepAnalysisChanged) {
            defaultDeepAnalysisProfileId = deepAnalysis;
        }

        if (whiteChanged || blackChanged || evaluationChanged || deepAnalysisChanged) {
            versions.configurationChanged(whiteChanged, blackChanged, evaluationChanged);
            persistStore();
        }
        return currentAssignments();
    }

    public synchronized EngineDefinitionDto inspectEngineDefinition(String enginePath, String requestedName) {
        try {
            UciEngineDefinition inspected = UciEngineInspector.inspect(enginePath);
            EngineDefinitionDto result = toDto(new ManagedEngineDefinition(
                    null,
                    displayEngineName(requestedName, inspected),
                    inspected));
            result.setId(null);
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not inspect UCI engine at " + enginePath + ": " + e.getMessage(), e);
        }
    }

    public synchronized EngineDefinitionDto createEngine(EngineDefinitionDto incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Engine definition must not be null");
        }
        if (incoming.getId() != null && !incoming.getId().isBlank()) {
            throw new IllegalArgumentException("A new engine definition must not already have an id");
        }

        UciEngineDefinition definition = definitionFromDto(incoming);
        if (findEngineByPath(definition.getEngine()) != null) {
            throw new IllegalArgumentException("An engine with this executable path is already defined");
        }

        String id = UUID.randomUUID().toString();
        ManagedEngineDefinition managed = new ManagedEngineDefinition(
                id,
                displayEngineName(incoming.getName(), definition),
                definition);
        engines.put(id, managed);
        versions.configurationChanged();
        persistStore();
        return toDto(managed);
    }

    public synchronized EngineDefinitionDto updateEngine(String id, EngineDefinitionDto incoming) {
        ManagedEngineDefinition existing = requireEngine(id);
        if (incoming == null) {
            throw new IllegalArgumentException("Engine definition must not be null");
        }
        if (incoming.getEngine() == null
                || !sameEnginePath(existing.definition.getEngine(), incoming.getEngine().trim())) {
            throw new IllegalArgumentException(
                    "The executable of an existing engine is immutable. Define another engine instead.");
        }

        UciEngineDefinition replacementDefinition = definitionFromDto(incoming);
        ManagedEngineDefinition replacement = new ManagedEngineDefinition(
                existing.id,
                displayEngineName(incoming.getName(), replacementDefinition),
                replacementDefinition);
        engines.put(existing.id, replacement);
        versions.configurationChanged();
        persistStore();
        return toDto(replacement);
    }

    public synchronized void deleteEngine(String id) {
        ManagedEngineDefinition existing = requireEngine(id);

        List<String> associatedProfileIds = profiles.values().stream()
                .filter(profile -> profile.engineId.equals(existing.id))
                .map(profile -> profile.id)
                .toList();

        boolean whiteChanged = associatedProfileIds.contains(defaultWhitePlayerProfileId);
        boolean blackChanged = associatedProfileIds.contains(defaultBlackPlayerProfileId);
        boolean evaluationChanged = associatedProfileIds.contains(defaultEvaluationProfileId);

        associatedProfileIds.forEach(profiles::remove);
        engines.remove(existing.id);

        // Deleting an engine is a cascading operation. Any defaults or fallback
        // that pointed to one of its profiles are repaired against the remaining
        // registry. Deleting the last engine intentionally leaves a valid empty
        // configuration instead of inventing a synthetic executable.
        repairFallbackAndAssignments();

        versions.configurationChanged(whiteChanged, blackChanged, evaluationChanged);
        persistStore();
    }

    public synchronized EngineProfileDto createProfile(EngineProfileDto incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Engine profile must not be null");
        }
        if (incoming.getId() != null && !incoming.getId().isBlank()) {
            throw new IllegalArgumentException("A new engine profile must not already have an id");
        }

        String id = UUID.randomUUID().toString();
        ManagedProfile profile = profileFromDto(id, incoming);
        profiles.put(id, profile);
        versions.configurationChanged();
        persistStore();
        return toDto(profile);
    }

    public synchronized EngineProfileDto updateProfile(String id, EngineProfileDto incoming) {
        ManagedProfile existing = requireProfile(id);
        if (incoming == null) {
            throw new IllegalArgumentException("Engine profile must not be null");
        }
        if (incoming.getEngineId() == null || !existing.engineId.equals(incoming.getEngineId().trim())) {
            throw new IllegalArgumentException(
                    "The engine of an existing profile is immutable. Create a new profile for another engine.");
        }

        ManagedProfile replacement = profileFromDto(existing.id, incoming);
        profiles.put(existing.id, replacement);
        versions.profileChanged(
                existing.id,
                defaultWhitePlayerProfileId,
                defaultBlackPlayerProfileId,
                defaultEvaluationProfileId);
        persistStore();
        return toDto(replacement);
    }

    public synchronized void deleteProfile(String id) {
        ManagedProfile existing = requireProfile(id);
        if (existing.id.equals(fallbackProfileId)) {
            throw new IllegalArgumentException("The fallback profile cannot be deleted");
        }
        if (isAssigned(existing.id)) {
            throw new IllegalArgumentException(
                    "Engine profile is assigned under Defaults and cannot be deleted");
        }
        profiles.remove(existing.id);
        versions.configurationChanged();
        persistStore();
    }

    public synchronized String normalizeProfileId(String profileId) {
        return resolveProfileId(profileId, fallbackProfileId);
    }

    public synchronized String normalizeDeepAnalysisProfileId(String profileId) {
        return resolveProfileId(profileId, defaultDeepAnalysisProfileId);
    }

    public synchronized String getDefaultWhitePlayerProfileId() {
        return defaultWhitePlayerProfileId;
    }

    public synchronized String getDefaultBlackPlayerProfileId() {
        return defaultBlackPlayerProfileId;
    }

    public synchronized String getDefaultEvaluationProfileId() {
        return defaultEvaluationProfileId;
    }

    public synchronized String getDefaultDeepAnalysisProfileId() {
        return defaultDeepAnalysisProfileId;
    }

    public synchronized UciEngineConfig getConfig(String id) {
        return resolveRuntimeConfig(requireProfile(id), 0, 0);
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return resolveRuntimeConfig(requireProfile(defaultWhitePlayerProfileId), 0, 0);
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return resolveRuntimeConfig(requireProfile(defaultBlackPlayerProfileId), 0, 0);
    }

    public synchronized UciEngineConfig toEvaluationEngineConfig() {
        return resolveRuntimeConfig(requireProfile(defaultEvaluationProfileId), 0, 0);
    }

    public synchronized UciEngineConfig getDeepAnalysisConfig(
            String profileId,
            int depth,
            int moveTimeSeconds) {
        String resolved = normalizeDeepAnalysisProfileId(profileId);
        int safeDepth = Math.max(0, depth);
        int safeMoveTimeSeconds = safeDepth > 0 ? 0 : Math.max(1, moveTimeSeconds);
        return resolveRuntimeConfig(requireProfile(resolved), safeDepth, safeMoveTimeSeconds);
    }

    public synchronized String getWhitePlayerEnginePath() {
        return engineForProfile(requireProfile(defaultWhitePlayerProfileId)).definition.getEngine();
    }

    public synchronized String getBlackPlayerEnginePath() {
        return engineForProfile(requireProfile(defaultBlackPlayerProfileId)).definition.getEngine();
    }

    public synchronized String getEvaluationEnginePath() {
        return engineForProfile(requireProfile(defaultEvaluationProfileId)).definition.getEngine();
    }

    public synchronized String getWhitePlayerEngineName() {
        return engineForProfile(requireProfile(defaultWhitePlayerProfileId)).definition.getEngineName();
    }

    public synchronized String getBlackPlayerEngineName() {
        return engineForProfile(requireProfile(defaultBlackPlayerProfileId)).definition.getEngineName();
    }

    public synchronized String getEvaluationEngineName() {
        return engineForProfile(requireProfile(defaultEvaluationProfileId)).definition.getEngineName();
    }

    public synchronized String getEngineName(String enginePath) {
        ManagedEngineDefinition managed = findEngineByPath(enginePath);
        if (managed != null) {
            return managed.definition.getEngineName();
        }
        try {
            return UciEngineInspector.inspect(enginePath).getEngineName();
        } catch (Exception e) {
            return fallbackEngineName(enginePath);
        }
    }

    public synchronized long getVersion() {
        return versions.configuration();
    }

    public synchronized long getWhitePlayerVersion() {
        return versions.whitePlayer();
    }

    public synchronized long getBlackPlayerVersion() {
        return versions.blackPlayer();
    }

    public synchronized long getEvaluationVersion() {
        return versions.evaluation();
    }

    public String getDefaultEnginePath() {
        return defaultEnginePath;
    }

    private EngineProfileAssignmentsDto currentAssignments() {
        return new EngineProfileAssignmentsDto(
                defaultWhitePlayerProfileId,
                defaultBlackPlayerProfileId,
                defaultEvaluationProfileId,
                defaultDeepAnalysisProfileId);
    }

    private UciEngineDefinition definitionFromDto(EngineDefinitionDto dto) {
        return registry.definitionFromDto(dto);
    }

    private ManagedProfile profileFromDto(String id, EngineProfileDto dto) {
        return registry.profileFromDto(id, dto);
    }

    private UciEngineConfig resolveRuntimeConfig(
            ManagedProfile profile,
            int depth,
            int moveTimeSeconds) {
        return registry.resolveRuntimeConfig(profile, depth, moveTimeSeconds);
    }

    private EngineDefinitionDto toDto(ManagedEngineDefinition managed) {
        return registry.toDto(managed);
    }

    private EngineProfileDto toDto(ManagedProfile managed) {
        return registry.toDto(managed);
    }

    private int discoverSystemEnginesInternal() {
        int added = 0;
        for (UciEngineDefinition definition : engineDiscoveryService.discover()) {
            if (findEngineByPath(definition.getEngine()) != null) {
                continue;
            }

            String engineId = UUID.randomUUID().toString();
            ManagedEngineDefinition managed = new ManagedEngineDefinition(
                    engineId,
                    definition.getEngineName(),
                    definition);
            engines.put(engineId, managed);
            createDefaultProfile(managed);
            added++;
        }
        return added;
    }

    private void repairFallbackAndAssignments() {
        if (engines.isEmpty()) {
            profiles.clear();
            fallbackProfileId = null;
            defaultWhitePlayerProfileId = null;
            defaultBlackPlayerProfileId = null;
            defaultEvaluationProfileId = null;
            defaultDeepAnalysisProfileId = null;
            return;
        }

        ManagedProfile fallback = validProfile(fallbackProfileId);
        ManagedEngineDefinition fallbackEngine = fallback == null ? null : engines.get(fallback.engineId);

        if (fallbackEngine == null) {
            fallbackEngine = findEngineByPath(defaultEnginePath);
        }
        if (fallbackEngine == null) {
            fallbackEngine = engines.values().iterator().next();
        }

        if (fallback == null || !fallback.engineId.equals(fallbackEngine.id)) {
            fallback = findDefaultProfileForEngine(fallbackEngine);
            if (fallback == null) {
                String id = createDefaultProfile(fallbackEngine);
                fallback = profiles.get(id);
            }
            fallbackProfileId = fallback.id;
        }

        defaultWhitePlayerProfileId = resolveProfileId(defaultWhitePlayerProfileId, fallbackProfileId);
        defaultBlackPlayerProfileId = resolveProfileId(defaultBlackPlayerProfileId, fallbackProfileId);
        defaultEvaluationProfileId = resolveProfileId(defaultEvaluationProfileId, fallbackProfileId);
        defaultDeepAnalysisProfileId = resolveProfileId(defaultDeepAnalysisProfileId, fallbackProfileId);
    }

    private ManagedProfile findDefaultProfileForEngine(ManagedEngineDefinition engine) {
        LinkedHashMap<String, String> defaults = defaultOptionValues(engine.definition);
        ManagedProfile firstForEngine = null;
        for (ManagedProfile profile : profiles.values()) {
            if (!profile.engineId.equals(engine.id)) {
                continue;
            }
            if (firstForEngine == null) {
                firstForEngine = profile;
            }
            if (profile.optionValues.equals(defaults)) {
                return profile;
            }
        }
        return firstForEngine;
    }

    private String createDefaultProfile(ManagedEngineDefinition engine) {
        EngineProfileDto dto = new EngineProfileDto();
        dto.setName(engine.name + " · Default");
        dto.setEngineId(engine.id);
        dto.setOptionValues(defaultOptionValues(engine.definition));

        String id = UUID.randomUUID().toString();
        profiles.put(id, profileFromDto(id, dto));
        return id;
    }

    private LinkedHashMap<String, String> defaultOptionValues(UciEngineDefinition definition) {
        return registry.defaultOptionValues(definition);
    }

    private boolean loadStore() {
        EngineConfigStoreDto stored = store.load();
        if (stored == null) return false;
        try {
            fallbackProfileId = stored.getFallbackProfileId();

            EngineProfileAssignmentsDto storedDefaults = stored.getDefaults();
            defaultWhitePlayerProfileId = storedDefaults.getWhitePlayerProfileId();
            defaultBlackPlayerProfileId = storedDefaults.getBlackPlayerProfileId();
            defaultEvaluationProfileId = storedDefaults.getEvaluationProfileId();
            defaultDeepAnalysisProfileId = storedDefaults.getDeepAnalysisProfileId();

            boolean hasModernData = !stored.getEngines().isEmpty() || !stored.getProfiles().isEmpty();
            if (hasModernData) {
                loadModernStore(stored);
            } else if (!stored.getLegacyConfigs().isEmpty()) {
                applyMissingAssignments(legacyMigrator.migrate(stored.getLegacyConfigs(), registry));
                logger.info("Migrated legacy combined engine configs to engine definitions and untyped profiles");
            }

            applyLegacyAssignments(stored);
            return true;
        } catch (Exception e) {
            logger.warn("Could not apply engine config store: " + e.getMessage());
            engines.clear();
            profiles.clear();
            fallbackProfileId = null;
            defaultWhitePlayerProfileId = null;
            defaultBlackPlayerProfileId = null;
            defaultEvaluationProfileId = null;
            defaultDeepAnalysisProfileId = null;
            return false;
        }
    }

    private void applyMissingAssignments(EngineProfileAssignmentsDto migrated) {
        if (defaultWhitePlayerProfileId == null) {
            defaultWhitePlayerProfileId = migrated.getWhitePlayerProfileId();
        }
        if (defaultBlackPlayerProfileId == null) {
            defaultBlackPlayerProfileId = migrated.getBlackPlayerProfileId();
        }
        if (defaultEvaluationProfileId == null) {
            defaultEvaluationProfileId = migrated.getEvaluationProfileId();
        }
        if (defaultDeepAnalysisProfileId == null) {
            defaultDeepAnalysisProfileId = migrated.getDeepAnalysisProfileId();
        }
    }

    private void loadModernStore(EngineConfigStoreDto store) {
        for (EngineDefinitionDto dto : store.getEngines()) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                continue;
            }
            UciEngineDefinition definition = definitionFromDto(dto);
            engines.put(dto.getId(), new ManagedEngineDefinition(
                    dto.getId(),
                    displayEngineName(dto.getName(), definition),
                    definition));
        }

        for (EngineProfileDto dto : store.getProfiles()) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                continue;
            }
            try {
                profiles.put(dto.getId(), profileFromDto(dto.getId(), dto));
                seedAssignmentFromLegacyProfile(dto);
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping invalid engine profile " + dto.getId() + ": " + e.getMessage());
            }
        }
    }

    private void seedAssignmentFromLegacyProfile(EngineProfileDto dto) {
        if (dto.getLegacyType() == null || dto.getLegacyType().isBlank() || dto.getId() == null) {
            return;
        }
        EngineConfigType type;
        try {
            type = EngineConfigType.fromValue(dto.getLegacyType());
        } catch (IllegalArgumentException e) {
            return;
        }
        switch (type) {
            case PLAYER -> {
                if (defaultWhitePlayerProfileId == null) {
                    defaultWhitePlayerProfileId = dto.getId();
                }
                if (defaultBlackPlayerProfileId == null) {
                    defaultBlackPlayerProfileId = dto.getId();
                }
            }
            case EVALUATION -> {
                if (defaultEvaluationProfileId == null) {
                    defaultEvaluationProfileId = dto.getId();
                }
            }
            case DEEP_ANALYSIS -> {
                if (defaultDeepAnalysisProfileId == null) {
                    defaultDeepAnalysisProfileId = dto.getId();
                }
            }
        }
    }

    private void applyLegacyAssignments(EngineConfigStoreDto store) {
        String legacyPlayer = validProfileId(store.getLegacyDefaultPlayerConfigId());
        String legacyEvaluation = validProfileId(store.getLegacyEvaluationConfigId());
        if (legacyEvaluation == null) {
            legacyEvaluation = validProfileId(store.getLegacyDefaultEvaluationConfigId());
        }
        String legacyDeep = validProfileId(store.getLegacyDefaultDeepAnalysisConfigId());

        if (defaultWhitePlayerProfileId == null && legacyPlayer != null) {
            defaultWhitePlayerProfileId = legacyPlayer;
        }
        if (defaultBlackPlayerProfileId == null && legacyPlayer != null) {
            defaultBlackPlayerProfileId = legacyPlayer;
        }
        if (defaultEvaluationProfileId == null && legacyEvaluation != null) {
            defaultEvaluationProfileId = legacyEvaluation;
        }
        if (defaultDeepAnalysisProfileId == null && legacyDeep != null) {
            defaultDeepAnalysisProfileId = legacyDeep;
        }
    }

    private void persistStore() {
        EngineConfigStoreDto snapshot = new EngineConfigStoreDto();
        snapshot.setEngines(engines.values().stream().map(this::toDto).toList());
        snapshot.setProfiles(profiles.values().stream().map(this::toDto).toList());
        snapshot.setDefaults(currentAssignments());
        snapshot.setFallbackProfileId(fallbackProfileId);
        store.save(snapshot);
    }

    private ManagedEngineDefinition requireEngine(String id) {
        return registry.requireEngine(id);
    }

    private ManagedProfile requireProfile(String id) {
        return registry.requireProfile(id);
    }

    private ManagedProfile validProfile(String id) {
        return registry.validProfile(id);
    }

    private ManagedEngineDefinition engineForProfile(ManagedProfile profile) {
        return registry.engineForProfile(profile);
    }

    private ManagedEngineDefinition findEngineByPath(String enginePath) {
        return registry.findEngineByPath(enginePath);
    }

    private boolean sameEnginePath(String first, String second) {
        return registry.sameEnginePath(first, second);
    }

    private String resolveProfileId(String requested, String fallback) {
        String requestedId = validProfileId(requested);
        if (requestedId != null) {
            return requestedId;
        }
        String fallbackId = validProfileId(fallback);
        if (fallbackId != null) {
            return fallbackId;
        }
        if (!profiles.isEmpty()) {
            return profiles.values().iterator().next().id;
        }
        return null;
    }

    private String validProfileId(String id) {
        return id != null && profiles.containsKey(id) ? id : null;
    }

    private boolean isAssigned(String profileId) {
        return Objects.equals(profileId, defaultWhitePlayerProfileId)
                || Objects.equals(profileId, defaultBlackPlayerProfileId)
                || Objects.equals(profileId, defaultEvaluationProfileId)
                || Objects.equals(profileId, defaultDeepAnalysisProfileId);
    }

    private String displayEngineName(String requestedName, UciEngineDefinition definition) {
        return registry.displayEngineName(requestedName, definition);
    }

    private String fallbackEngineName(String path) {
        return registry.fallbackEngineName(path);
    }
}
