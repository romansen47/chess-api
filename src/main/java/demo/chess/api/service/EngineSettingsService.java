package demo.chess.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineConfigStoreDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileAssignmentsDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.dto.ManagedEngineConfigDto;
import demo.chess.api.dto.UciOptionDto;
import demo.chess.definitions.engines.EngineConfigType;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciEngineInspector;
import demo.chess.definitions.engines.UciOption;
import demo.chess.definitions.engines.UciOptionType;

/**
 * Registry for UCI engines, reusable profiles and their use-case assignments.
 *
 * Engine definition: executable + UCI identity + option schema/defaults.
 * Profile: one engine + concrete UCI option values.
 * Assignment: which profile is used by White CPU, Black CPU, live evaluation
 *             and deep analysis by default.
 *
 * A profile intentionally has no PLAYER/EVALUATION/DEEP_ANALYSIS type. The same
 * profile may be assigned to any number of use cases.
 */
@Service
public class EngineSettingsService {

    private static final Log logger = LogFactory.getLog(EngineSettingsService.class);
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    private final EngineDiscoveryService engineDiscoveryService;
    private final String defaultEnginePath;
    private final ObjectMapper objectMapper;
    private final Path storePath;
    private final LinkedHashMap<String, ManagedEngineDefinition> engines = new LinkedHashMap<>();
    private final LinkedHashMap<String, ManagedProfile> profiles = new LinkedHashMap<>();

    private String fallbackProfileId;
    private String defaultWhitePlayerProfileId;
    private String defaultBlackPlayerProfileId;
    private String defaultEvaluationProfileId;
    private String defaultDeepAnalysisProfileId;

    private long version = 1L;
    private long whitePlayerVersion = 1L;
    private long blackPlayerVersion = 1L;
    private long evaluationVersion = 1L;

    public EngineSettingsService(ObjectMapper objectMapper, EngineDiscoveryService engineDiscoveryService) {
        this.objectMapper = objectMapper;
        this.engineDiscoveryService = engineDiscoveryService;
        this.defaultEnginePath = engineDiscoveryService.getPreferredEnginePath();
        this.storePath = resolveStorePath();

        loadStore();

        // Discovery is automatic only for an empty installation. A normal
        // restart never re-adds engines that a user deliberately removed.
        if (engines.isEmpty() && profiles.isEmpty()) {
            discoverSystemEnginesInternal();
        }

        ensureFallbackAndAssignments();
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
        ensureFallbackAndAssignments();

        whitePlayerVersion++;
        blackPlayerVersion++;
        evaluationVersion++;
        version++;

        persistStore();
        logger.info("Reset engine settings and rediscovered system UCI engines in "
                + engineDiscoveryService.getDiscoveryDirectory());
        return getOverview();
    }

    /**
     * Scans the configured system engine directory and adds only UCI engines
     * that are not already registered. Existing engines, profiles and default
     * assignments are left untouched.
     */
    public synchronized EngineConfigOverviewDto discoverSystemEngines() {
        int addedEngines = discoverSystemEnginesInternal();
        if (addedEngines > 0) {
            version++;
            persistStore();
        }
        return getOverview();
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
                version);
    }

    public synchronized EngineProfileAssignmentsDto updateDefaultAssignments(EngineProfileAssignmentsDto incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Default profile assignments must not be null");
        }

        String white = resolveProfileId(incoming.getWhitePlayerProfileId(), fallbackProfileId);
        String black = resolveProfileId(incoming.getBlackPlayerProfileId(), fallbackProfileId);
        String evaluation = resolveProfileId(incoming.getEvaluationProfileId(), fallbackProfileId);
        String deepAnalysis = resolveProfileId(incoming.getDeepAnalysisProfileId(), fallbackProfileId);

        boolean changed = false;
        if (!white.equals(defaultWhitePlayerProfileId)) {
            defaultWhitePlayerProfileId = white;
            whitePlayerVersion++;
            changed = true;
        }
        if (!black.equals(defaultBlackPlayerProfileId)) {
            defaultBlackPlayerProfileId = black;
            blackPlayerVersion++;
            changed = true;
        }
        if (!evaluation.equals(defaultEvaluationProfileId)) {
            defaultEvaluationProfileId = evaluation;
            evaluationVersion++;
            changed = true;
        }
        if (!deepAnalysis.equals(defaultDeepAnalysisProfileId)) {
            defaultDeepAnalysisProfileId = deepAnalysis;
            changed = true;
        }

        if (changed) {
            version++;
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
        version++;
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
        version++;
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
        // registry. If the last engine is deleted, the compatibility fallback
        // keeps the application operational.
        ensureFallbackAndAssignments();

        if (whiteChanged) {
            whitePlayerVersion++;
        }
        if (blackChanged) {
            blackPlayerVersion++;
        }
        if (evaluationChanged) {
            evaluationVersion++;
        }
        version++;
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
        version++;
        persistStore();
        return toDto(profile);
    }

    public synchronized EngineProfileDto updateProfile(String id, EngineProfileDto incoming) {
        ManagedProfile existing = requireProfile(id);
        if (existing.id.equals(fallbackProfileId)) {
            throw new IllegalArgumentException(
                    "The fallback profile is fixed to its engine UCI defaults. Duplicate it to customize it.");
        }
        if (incoming == null) {
            throw new IllegalArgumentException("Engine profile must not be null");
        }
        if (incoming.getEngineId() == null || !existing.engineId.equals(incoming.getEngineId().trim())) {
            throw new IllegalArgumentException(
                    "The engine of an existing profile is immutable. Create a new profile for another engine.");
        }

        ManagedProfile replacement = profileFromDto(existing.id, incoming);
        profiles.put(existing.id, replacement);
        version++;
        bumpRuntimeVersions(existing.id);
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
        version++;
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
        return version;
    }

    public synchronized long getWhitePlayerVersion() {
        return whitePlayerVersion;
    }

    public synchronized long getBlackPlayerVersion() {
        return blackPlayerVersion;
    }

    public synchronized long getEvaluationVersion() {
        return evaluationVersion;
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
        if (dto.getEngine() == null || dto.getEngine().isBlank()) {
            throw new IllegalArgumentException("Engine path must not be blank");
        }

        LinkedHashMap<String, UciOption> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOptionDto> entry : dto.getOptions().entrySet()) {
            UciOptionDto source = entry.getValue();
            if (source == null) {
                continue;
            }
            UciOptionType optionType = UciOptionType.fromUciValue(source.getType());
            String configuredDefault = optionType == UciOptionType.BUTTON ? null : source.getDefaultValue();
            options.put(entry.getKey(), new UciOption(
                    optionType,
                    configuredDefault,
                    configuredDefault,
                    source.getMin(),
                    source.getMax(),
                    source.getVars()));
        }

        return new UciEngineDefinition(
                dto.getEngine(),
                dto.getEngineName(),
                dto.getEngineAuthor(),
                options);
    }

    private ManagedProfile profileFromDto(String id, EngineProfileDto dto) {
        if (dto.getEngineId() == null || dto.getEngineId().isBlank()) {
            throw new IllegalArgumentException("An engine must be selected before a profile can be created");
        }
        ManagedEngineDefinition engine = requireEngine(dto.getEngineId().trim());

        for (String incomingName : dto.getOptionValues().keySet()) {
            UciOption option = engine.definition.getOption(incomingName);
            if (option == null) {
                throw new IllegalArgumentException(
                        "Profile contains option '" + incomingName
                                + "' which is not supported by engine " + engine.name);
            }
            if (!option.isConfigurable()) {
                throw new IllegalArgumentException(
                        "UCI button option '" + incomingName + "' cannot be stored in a profile");
            }
        }

        LinkedHashMap<String, String> normalizedValues = new LinkedHashMap<>();
        for (Map.Entry<String, UciOption> entry : engine.definition.getOptions().entrySet()) {
            UciOption definitionOption = entry.getValue();
            if (!definitionOption.isConfigurable()) {
                continue;
            }
            String value = dto.getOptionValues().containsKey(entry.getKey())
                    ? dto.getOptionValues().get(entry.getKey())
                    : definitionOption.getDefaultValue();
            UciOption validator = definitionOption.copy();
            validator.setValue(value);
            normalizedValues.put(entry.getKey(), validator.getValue());
        }

        String name = dto.getName() == null || dto.getName().isBlank()
                ? engine.name + " Profile"
                : dto.getName().trim();
        return new ManagedProfile(id, name, engine.id, normalizedValues);
    }

    private UciEngineConfig resolveRuntimeConfig(
            ManagedProfile profile,
            int depth,
            int moveTimeSeconds) {
        ManagedEngineDefinition engine = engineForProfile(profile);
        return engine.definition.createRuntimeConfig(
                Math.max(0, depth),
                Math.max(0, moveTimeSeconds),
                profile.optionValues);
    }

    private EngineDefinitionDto toDto(ManagedEngineDefinition managed) {
        EngineDefinitionDto dto = new EngineDefinitionDto();
        dto.setId(managed.id);
        dto.setName(managed.name);
        dto.setEngine(managed.definition.getEngine());
        dto.setEngineName(managed.definition.getEngineName());
        dto.setEngineAuthor(managed.definition.getEngineAuthor());

        LinkedHashMap<String, UciOptionDto> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOption> entry : managed.definition.getOptions().entrySet()) {
            UciOption option = entry.getValue();
            options.put(entry.getKey(), new UciOptionDto(
                    option.getType().toUciValue(),
                    option.getDefaultValue(),
                    option.getDefaultValue(),
                    option.getMin(),
                    option.getMax(),
                    option.getVars()));
        }
        dto.setOptions(options);
        return dto;
    }

    private EngineProfileDto toDto(ManagedProfile managed) {
        EngineProfileDto dto = new EngineProfileDto();
        dto.setId(managed.id);
        dto.setName(managed.name);
        dto.setEngineId(managed.engineId);
        dto.setOptionValues(managed.optionValues);
        return dto;
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

    private void ensureFallbackAndAssignments() {
        ManagedProfile fallback = validProfile(fallbackProfileId);
        ManagedEngineDefinition fallbackEngine = fallback == null ? null : engines.get(fallback.engineId);

        if (fallbackEngine == null) {
            fallbackEngine = findEngineByPath(defaultEnginePath);
        }
        if (fallbackEngine == null && !engines.isEmpty()) {
            fallbackEngine = engines.values().iterator().next();
        }
        if (fallbackEngine == null) {
            fallbackEngine = createCompatibilityFallbackEngine();
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

        persistStore();
    }

    private ManagedEngineDefinition createCompatibilityFallbackEngine() {
        UciEngineDefinition definition;
        try {
            definition = UciEngineInspector.inspect(defaultEnginePath);
        } catch (Exception e) {
            logger.warn("No responsive UCI engine was discovered in "
                    + engineDiscoveryService.getDiscoveryDirectory()
                    + ". Keeping the legacy fallback path " + defaultEnginePath
                    + " so the application can still start: " + e.getMessage());
            definition = new UciEngineDefinition(
                    defaultEnginePath,
                    fallbackEngineName(defaultEnginePath),
                    "",
                    Map.of());
        }

        String engineId = UUID.randomUUID().toString();
        ManagedEngineDefinition managed = new ManagedEngineDefinition(
                engineId,
                definition.getEngineName(),
                definition);
        engines.put(engineId, managed);
        return managed;
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
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, UciOption> entry : definition.getOptions().entrySet()) {
            if (entry.getValue().isConfigurable()) {
                result.put(entry.getKey(), entry.getValue().getDefaultValue());
            }
        }
        return result;
    }

    private void loadStore() {
        if (!Files.isRegularFile(storePath)) {
            return;
        }
        try {
            EngineConfigStoreDto store = objectMapper.readValue(storePath.toFile(), EngineConfigStoreDto.class);
            fallbackProfileId = store.getFallbackProfileId();

            EngineProfileAssignmentsDto storedDefaults = store.getDefaults();
            defaultWhitePlayerProfileId = storedDefaults.getWhitePlayerProfileId();
            defaultBlackPlayerProfileId = storedDefaults.getBlackPlayerProfileId();
            defaultEvaluationProfileId = storedDefaults.getEvaluationProfileId();
            defaultDeepAnalysisProfileId = storedDefaults.getDeepAnalysisProfileId();

            boolean hasModernData = !store.getEngines().isEmpty() || !store.getProfiles().isEmpty();
            if (hasModernData) {
                loadModernStore(store);
            } else if (!store.getLegacyConfigs().isEmpty()) {
                migrateLegacyConfigs(store.getLegacyConfigs());
                logger.info("Migrated legacy combined engine configs to engine definitions and untyped profiles");
            }

            applyLegacyAssignments(store);
        } catch (Exception e) {
            logger.warn("Could not load engine config store " + storePath + ": " + e.getMessage());
            engines.clear();
            profiles.clear();
            fallbackProfileId = null;
            defaultWhitePlayerProfileId = null;
            defaultBlackPlayerProfileId = null;
            defaultEvaluationProfileId = null;
            defaultDeepAnalysisProfileId = null;
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

    private void migrateLegacyConfigs(List<ManagedEngineConfigDto> legacyConfigs) {
        LinkedHashMap<String, String> engineIdsByPath = new LinkedHashMap<>();

        for (ManagedEngineConfigDto legacy : legacyConfigs) {
            if (legacy.getEngine() == null || legacy.getEngine().isBlank()) {
                continue;
            }
            String path = legacy.getEngine().trim();
            String engineId = engineIdsByPath.get(path);
            if (engineId == null) {
                EngineDefinitionDto engineDto = new EngineDefinitionDto();
                engineDto.setEngine(path);
                engineDto.setEngineName(legacy.getEngineName());
                engineDto.setEngineAuthor(legacy.getEngineAuthor());
                engineDto.setName(legacy.getEngineName());

                LinkedHashMap<String, UciOptionDto> definitionOptions = new LinkedHashMap<>();
                for (Map.Entry<String, UciOptionDto> entry : legacy.getOptions().entrySet()) {
                    UciOptionDto oldOption = entry.getValue();
                    if (oldOption == null) {
                        continue;
                    }
                    definitionOptions.put(entry.getKey(), new UciOptionDto(
                            oldOption.getType(),
                            oldOption.getDefaultValue(),
                            oldOption.getDefaultValue(),
                            oldOption.getMin(),
                            oldOption.getMax(),
                            oldOption.getVars()));
                }
                engineDto.setOptions(definitionOptions);

                UciEngineDefinition definition = definitionFromDto(engineDto);
                engineId = UUID.randomUUID().toString();
                engines.put(engineId, new ManagedEngineDefinition(
                        engineId,
                        displayEngineName(engineDto.getName(), definition),
                        definition));
                engineIdsByPath.put(path, engineId);
            }

            EngineProfileDto profileDto = new EngineProfileDto();
            profileDto.setName(legacy.getName());
            profileDto.setEngineId(engineId);

            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, UciOptionDto> entry : legacy.getOptions().entrySet()) {
                UciOptionDto option = entry.getValue();
                if (option != null && !"button".equalsIgnoreCase(option.getType())) {
                    values.put(entry.getKey(), option.getValue());
                }
            }
            profileDto.setOptionValues(values);

            String profileId = legacy.getId() == null || legacy.getId().isBlank()
                    ? UUID.randomUUID().toString()
                    : legacy.getId();
            profiles.put(profileId, profileFromDto(profileId, profileDto));

            EngineConfigType legacyType = legacy.getType() == null || legacy.getType().isBlank()
                    ? inferLegacyType(legacy)
                    : EngineConfigType.fromValue(legacy.getType());
            seedMigratedAssignment(profileId, legacyType);
        }
    }

    private void seedMigratedAssignment(String profileId, EngineConfigType type) {
        switch (type) {
            case PLAYER -> {
                if (defaultWhitePlayerProfileId == null) {
                    defaultWhitePlayerProfileId = profileId;
                }
                if (defaultBlackPlayerProfileId == null) {
                    defaultBlackPlayerProfileId = profileId;
                }
            }
            case EVALUATION -> {
                if (defaultEvaluationProfileId == null) {
                    defaultEvaluationProfileId = profileId;
                }
            }
            case DEEP_ANALYSIS -> {
                if (defaultDeepAnalysisProfileId == null) {
                    defaultDeepAnalysisProfileId = profileId;
                }
            }
        }
    }

    private EngineConfigType inferLegacyType(ManagedEngineConfigDto dto) {
        String name = dto.getName();
        if (name != null) {
            String normalized = name.toLowerCase();
            if (normalized.contains("deep analysis") || normalized.contains("deep-analysis")) {
                return EngineConfigType.DEEP_ANALYSIS;
            }
            if (normalized.contains("evaluation")) {
                return EngineConfigType.EVALUATION;
            }
        }
        return EngineConfigType.PLAYER;
    }

    private void persistStore() {
        try {
            Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            EngineConfigStoreDto store = new EngineConfigStoreDto();
            List<EngineDefinitionDto> engineDtos = new ArrayList<>();
            for (ManagedEngineDefinition managed : engines.values()) {
                engineDtos.add(toDto(managed));
            }
            List<EngineProfileDto> profileDtos = new ArrayList<>();
            for (ManagedProfile managed : profiles.values()) {
                profileDtos.add(toDto(managed));
            }
            store.setEngines(engineDtos);
            store.setProfiles(profileDtos);
            store.setDefaults(currentAssignments());
            store.setFallbackProfileId(fallbackProfileId);

            Path temporary = storePath.resolveSibling(storePath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), store);
            try {
                Files.move(temporary, storePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            logger.warn("Could not persist engine config store " + storePath + ": " + e.getMessage());
        }
    }

    private Path resolveStorePath() {
        String configured = System.getProperty(STORE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".chess", "engine-configs.json")
                .toAbsolutePath()
                .normalize();
    }

    private ManagedEngineDefinition requireEngine(String id) {
        ManagedEngineDefinition result = engines.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Unknown engine id: " + id);
        }
        return result;
    }

    private ManagedProfile requireProfile(String id) {
        ManagedProfile result = profiles.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Unknown engine profile id: " + id);
        }
        return result;
    }

    private ManagedProfile validProfile(String id) {
        return id == null ? null : profiles.get(id);
    }

    private ManagedEngineDefinition engineForProfile(ManagedProfile profile) {
        return requireEngine(profile.engineId);
    }

    private ManagedEngineDefinition findEngineByPath(String enginePath) {
        if (enginePath == null || enginePath.isBlank()) {
            return null;
        }
        for (ManagedEngineDefinition engine : engines.values()) {
            if (sameEnginePath(engine.definition.getEngine(), enginePath)) {
                return engine;
            }
        }
        return null;
    }

    private boolean sameEnginePath(String first, String second) {
        return canonicalEnginePath(first).equals(canonicalEnginePath(second));
    }

    private String canonicalEnginePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            Path path = Path.of(value.trim()).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                return path.toRealPath().toString();
            }
            return path.toString();
        } catch (Exception e) {
            return value.trim();
        }
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
        throw new IllegalStateException("No engine profiles available");
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

    private void bumpRuntimeVersions(String profileId) {
        if (profileId.equals(defaultWhitePlayerProfileId)) {
            whitePlayerVersion++;
        }
        if (profileId.equals(defaultBlackPlayerProfileId)) {
            blackPlayerVersion++;
        }
        if (profileId.equals(defaultEvaluationProfileId)) {
            evaluationVersion++;
        }
    }

    private String displayEngineName(String requestedName, UciEngineDefinition definition) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return definition.getEngineName();
    }

    private String fallbackEngineName(String path) {
        if (path == null || path.isBlank()) {
            return "Engine";
        }
        try {
            Path fileName = Path.of(path).getFileName();
            return fileName == null ? path : fileName.toString();
        } catch (Exception e) {
            return path;
        }
    }

    private static final class ManagedEngineDefinition {
        private final String id;
        private final String name;
        private final UciEngineDefinition definition;

        private ManagedEngineDefinition(String id, String name, UciEngineDefinition definition) {
            this.id = id;
            this.name = name;
            this.definition = definition;
        }
    }

    private static final class ManagedProfile {
        private final String id;
        private final String name;
        private final String engineId;
        private final LinkedHashMap<String, String> optionValues;

        private ManagedProfile(
                String id,
                String name,
                String engineId,
                Map<String, String> optionValues) {
            this.id = id;
            this.name = name;
            this.engineId = engineId;
            this.optionValues = new LinkedHashMap<>(optionValues);
        }
    }
}
