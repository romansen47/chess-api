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
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineConfigStoreDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.dto.ManagedEngineConfigDto;
import demo.chess.api.dto.UciOptionDto;
import demo.chess.definitions.engines.Engine;
import demo.chess.definitions.engines.EngineConfigType;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciEngineInspector;
import demo.chess.definitions.engines.UciOption;
import demo.chess.definitions.engines.UciOptionType;

/**
 * Registry for UCI engines and reusable profiles.
 *
 * An engine definition owns the executable path, UCI identity and option schema.
 * A profile references exactly one engine definition and stores only contextual
 * search settings and concrete option values. Runtime UciEngineConfig instances
 * are resolved from that pair on demand.
 */
@Service
public class EngineSettingsService {

    private static final Log logger = LogFactory.getLog(EngineSettingsService.class);
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    private final String defaultEnginePath;
    private final ObjectMapper objectMapper;
    private final Path storePath;
    private final LinkedHashMap<String, ManagedEngineDefinition> engines = new LinkedHashMap<>();
    private final LinkedHashMap<String, ManagedProfile> profiles = new LinkedHashMap<>();

    private String defaultPlayerConfigId;
    private String defaultEvaluationConfigId;
    private String defaultDeepAnalysisConfigId;
    private String evaluationConfigId;
    private String whitePlayerConfigId;
    private String blackPlayerConfigId;

    private long version = 1L;
    private long whitePlayerVersion = 1L;
    private long blackPlayerVersion = 1L;
    private long evaluationVersion = 1L;

    public EngineSettingsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.defaultEnginePath = "/usr/games/" + Engine.STOCKFISH_16.path();
        this.storePath = resolveStorePath();

        loadStore();
        ensureDefaults();

        this.whitePlayerConfigId = defaultPlayerConfigId;
        this.blackPlayerConfigId = defaultPlayerConfigId;
        this.evaluationConfigId = resolveProfileId(
                evaluationConfigId,
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION);
    }

    public synchronized EngineConfigOverviewDto resetToFallbackDefaults() {
        engines.clear();
        profiles.clear();

        defaultPlayerConfigId = null;
        defaultEvaluationConfigId = null;
        defaultDeepAnalysisConfigId = null;
        evaluationConfigId = null;
        whitePlayerConfigId = null;
        blackPlayerConfigId = null;

        ensureDefaults();

        whitePlayerConfigId = defaultPlayerConfigId;
        blackPlayerConfigId = defaultPlayerConfigId;
        evaluationConfigId = defaultEvaluationConfigId;

        whitePlayerVersion++;
        blackPlayerVersion++;
        evaluationVersion++;
        version++;

        persistStore();
        logger.info("Reset engine settings to fallback engine " + defaultEnginePath + " with UCI defaults");
        return getOverview();
    }

    public synchronized EngineConfigOverviewDto getOverview() {
        List<EngineDefinitionDto> engineDtos = engines.values().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(EngineDefinitionDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<EngineProfileDto> profileDtos = profiles.values().stream()
                .map(this::toDto)
                .sorted(Comparator
                        .comparing(EngineProfileDto::getType)
                        .thenComparing(EngineProfileDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new EngineConfigOverviewDto(
                engineDtos,
                profileDtos,
                evaluationConfigId,
                defaultPlayerConfigId,
                defaultDeepAnalysisConfigId,
                version);
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
                || !existing.definition.getEngine().equals(incoming.getEngine().trim())) {
            throw new IllegalArgumentException(
                    "The executable of an existing engine is immutable. Define another engine instead.");
        }

        ManagedEngineDefinition replacement = new ManagedEngineDefinition(
                existing.id,
                displayEngineName(incoming.getName(), existing.definition),
                existing.definition.copy());
        engines.put(existing.id, replacement);
        version++;
        persistStore();
        return toDto(replacement);
    }

    public synchronized void deleteEngine(String id) {
        ManagedEngineDefinition existing = requireEngine(id);
        boolean used = profiles.values().stream().anyMatch(profile -> profile.engineId.equals(existing.id));
        if (used) {
            throw new IllegalArgumentException(
                    "Engine is referenced by at least one profile and cannot be deleted");
        }
        engines.remove(existing.id);
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
        if (incoming == null) {
            throw new IllegalArgumentException("Engine profile must not be null");
        }
        if (incoming.getEngineId() == null || !existing.engineId.equals(incoming.getEngineId().trim())) {
            throw new IllegalArgumentException(
                    "The engine of an existing profile is immutable. Create a new profile for another engine.");
        }
        EngineConfigType incomingType = EngineConfigType.fromValue(incoming.getType());
        if (existing.type != incomingType) {
            throw new IllegalArgumentException(
                    "The purpose of an existing profile is immutable. Create a new profile for another purpose.");
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
        if (existing.id.equals(defaultPlayerConfigId)
                || existing.id.equals(defaultEvaluationConfigId)
                || existing.id.equals(defaultDeepAnalysisConfigId)) {
            throw new IllegalArgumentException("Default engine profiles cannot be deleted");
        }
        if (existing.id.equals(whitePlayerConfigId)
                || existing.id.equals(blackPlayerConfigId)
                || existing.id.equals(evaluationConfigId)) {
            throw new IllegalArgumentException("Engine profile is currently in use and cannot be deleted");
        }
        profiles.remove(existing.id);
        version++;
        persistStore();
    }

    public synchronized String setEvaluationConfigId(String configId) {
        String resolved = resolveProfileId(
                configId,
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION);
        if (!resolved.equals(evaluationConfigId)) {
            evaluationConfigId = resolved;
            evaluationVersion++;
            version++;
            persistStore();
        }
        return evaluationConfigId;
    }

    public synchronized void setPlayerConfigIds(String whiteConfigId, String blackConfigId) {
        String resolvedWhite = resolveProfileId(
                whiteConfigId,
                defaultPlayerConfigId,
                EngineConfigType.PLAYER);
        String resolvedBlack = resolveProfileId(
                blackConfigId,
                defaultPlayerConfigId,
                EngineConfigType.PLAYER);

        if (!resolvedWhite.equals(whitePlayerConfigId)) {
            whitePlayerConfigId = resolvedWhite;
            whitePlayerVersion++;
        }
        if (!resolvedBlack.equals(blackPlayerConfigId)) {
            blackPlayerConfigId = resolvedBlack;
            blackPlayerVersion++;
        }
    }

    public synchronized String normalizePlayerConfigId(String configId) {
        return resolveProfileId(configId, defaultPlayerConfigId, EngineConfigType.PLAYER);
    }

    public synchronized String normalizeDeepAnalysisConfigId(String configId) {
        return resolveProfileId(
                configId,
                defaultDeepAnalysisConfigId,
                EngineConfigType.DEEP_ANALYSIS);
    }

    public synchronized String getDefaultPlayerConfigId() {
        return defaultPlayerConfigId;
    }

    public synchronized String getDefaultDeepAnalysisConfigId() {
        return defaultDeepAnalysisConfigId;
    }

    public synchronized String getWhitePlayerConfigId() {
        return whitePlayerConfigId;
    }

    public synchronized String getBlackPlayerConfigId() {
        return blackPlayerConfigId;
    }

    public synchronized String getEvaluationConfigId() {
        return evaluationConfigId;
    }

    public synchronized UciEngineConfig getConfig(String id) {
        return resolveRuntimeConfig(requireProfile(id));
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return resolveRuntimeConfig(requireProfile(whitePlayerConfigId, EngineConfigType.PLAYER));
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return resolveRuntimeConfig(requireProfile(blackPlayerConfigId, EngineConfigType.PLAYER));
    }

    public synchronized UciEngineConfig toEvaluationEngineConfig() {
        return resolveRuntimeConfig(requireProfile(evaluationConfigId, EngineConfigType.EVALUATION));
    }

    public synchronized UciEngineConfig getDeepAnalysisConfig(String configId) {
        String resolved = normalizeDeepAnalysisConfigId(configId);
        return resolveRuntimeConfig(requireProfile(resolved, EngineConfigType.DEEP_ANALYSIS));
    }

    public synchronized String getWhitePlayerEnginePath() {
        return engineForProfile(requireProfile(whitePlayerConfigId, EngineConfigType.PLAYER))
                .definition.getEngine();
    }

    public synchronized String getBlackPlayerEnginePath() {
        return engineForProfile(requireProfile(blackPlayerConfigId, EngineConfigType.PLAYER))
                .definition.getEngine();
    }

    public synchronized String getEvaluationEnginePath() {
        return engineForProfile(requireProfile(evaluationConfigId, EngineConfigType.EVALUATION))
                .definition.getEngine();
    }

    public synchronized String getWhitePlayerEngineName() {
        return engineForProfile(requireProfile(whitePlayerConfigId, EngineConfigType.PLAYER))
                .definition.getEngineName();
    }

    public synchronized String getBlackPlayerEngineName() {
        return engineForProfile(requireProfile(blackPlayerConfigId, EngineConfigType.PLAYER))
                .definition.getEngineName();
    }

    public synchronized String getEvaluationEngineName() {
        return engineForProfile(requireProfile(evaluationConfigId, EngineConfigType.EVALUATION))
                .definition.getEngineName();
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
            String initialValue = optionType == UciOptionType.BUTTON ? null : source.getDefaultValue();
            options.put(entry.getKey(), new UciOption(
                    optionType,
                    source.getDefaultValue(),
                    initialValue,
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
        EngineConfigType type = EngineConfigType.fromValue(dto.getType());
        int depth = Math.max(0, dto.getDepth());
        int moveTimeSeconds = Math.max(0, dto.getMoveTimeSeconds());
        if (type == EngineConfigType.DEEP_ANALYSIS && depth == 0) {
            moveTimeSeconds = Math.max(1, moveTimeSeconds);
        }

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
                ? engine.name + " · " + typeLabel(type)
                : dto.getName().trim();
        return new ManagedProfile(
                id,
                name,
                type,
                engine.id,
                depth,
                moveTimeSeconds,
                normalizedValues);
    }

    private UciEngineConfig resolveRuntimeConfig(ManagedProfile profile) {
        ManagedEngineDefinition engine = engineForProfile(profile);
        return engine.definition.createRuntimeConfig(
                profile.type,
                profile.depth,
                profile.moveTimeSeconds,
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
        dto.setType(managed.type.name());
        dto.setEngineId(managed.engineId);
        dto.setDepth(managed.depth);
        dto.setMoveTimeSeconds(managed.moveTimeSeconds);
        dto.setOptionValues(managed.optionValues);
        return dto;
    }

    private void ensureDefaults() {
        ManagedEngineDefinition defaultEngine = findEngineByPath(defaultEnginePath);
        if (defaultEngine == null) {
            UciEngineDefinition definition;
            try {
                definition = UciEngineInspector.inspect(defaultEnginePath);
            } catch (Exception e) {
                logger.warn("Could not inspect default UCI engine at " + defaultEnginePath
                        + ". Creating a definition without UCI options: " + e.getMessage());
                definition = new UciEngineDefinition(
                        defaultEnginePath,
                        fallbackEngineName(defaultEnginePath),
                        "",
                        Map.of());
            }
            String engineId = UUID.randomUUID().toString();
            defaultEngine = new ManagedEngineDefinition(
                    engineId,
                    definition.getEngineName(),
                    definition);
            engines.put(engineId, defaultEngine);
        }

        defaultPlayerConfigId = ensureDefaultProfile(
                defaultPlayerConfigId,
                EngineConfigType.PLAYER,
                defaultEngine,
                "Player");
        defaultEvaluationConfigId = ensureDefaultProfile(
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION,
                defaultEngine,
                "Evaluation");
        defaultDeepAnalysisConfigId = ensureDefaultProfile(
                defaultDeepAnalysisConfigId,
                EngineConfigType.DEEP_ANALYSIS,
                defaultEngine,
                "Deep Analysis");

        evaluationConfigId = resolveProfileId(
                evaluationConfigId,
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION);
        persistStore();
    }

    private String ensureDefaultProfile(
            String currentId,
            EngineConfigType type,
            ManagedEngineDefinition defaultEngine,
            String suffix) {
        if (hasProfileOfType(currentId, type)) {
            return currentId;
        }
        for (ManagedProfile profile : profiles.values()) {
            if (profile.type == type) {
                return profile.id;
            }
        }

        EngineProfileDto dto = new EngineProfileDto();
        dto.setName(defaultEngine.name + " · " + suffix);
        dto.setType(type.name());
        dto.setEngineId(defaultEngine.id);
        dto.setDepth(0);
        dto.setMoveTimeSeconds(type == EngineConfigType.DEEP_ANALYSIS ? 5 : 0);

        dto.setOptionValues(defaultOptionValues(defaultEngine.definition));

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
            defaultPlayerConfigId = store.getDefaultPlayerConfigId();
            defaultEvaluationConfigId = store.getDefaultEvaluationConfigId();
            defaultDeepAnalysisConfigId = store.getDefaultDeepAnalysisConfigId();
            evaluationConfigId = store.getEvaluationConfigId();

            boolean hasModernData = !store.getEngines().isEmpty() || !store.getProfiles().isEmpty();
            if (hasModernData) {
                loadModernStore(store);
            } else if (!store.getConfigs().isEmpty()) {
                migrateLegacyConfigs(store.getConfigs());
                logger.info("Migrated legacy combined engine configs to engine definitions and profiles");
            }
        } catch (Exception e) {
            logger.warn("Could not load engine config store " + storePath + ": " + e.getMessage());
            engines.clear();
            profiles.clear();
            defaultPlayerConfigId = null;
            defaultEvaluationConfigId = null;
            defaultDeepAnalysisConfigId = null;
            evaluationConfigId = null;
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
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping invalid engine profile " + dto.getId() + ": " + e.getMessage());
            }
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
            profileDto.setType(legacy.getType() == null || legacy.getType().isBlank()
                    ? inferLegacyType(legacy).name()
                    : legacy.getType());
            profileDto.setEngineId(engineId);
            profileDto.setDepth(legacy.getDepth());
            profileDto.setMoveTimeSeconds(legacy.getMoveTimeSeconds());

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
        }
    }

    private EngineConfigType inferLegacyType(ManagedEngineConfigDto dto) {
        if (dto.getId() != null
                && (dto.getId().equals(defaultEvaluationConfigId) || dto.getId().equals(evaluationConfigId))) {
            return EngineConfigType.EVALUATION;
        }
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
            store.setDefaultPlayerConfigId(defaultPlayerConfigId);
            store.setDefaultEvaluationConfigId(defaultEvaluationConfigId);
            store.setDefaultDeepAnalysisConfigId(defaultDeepAnalysisConfigId);
            store.setEvaluationConfigId(evaluationConfigId);

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

    private ManagedProfile requireProfile(String id, EngineConfigType type) {
        ManagedProfile result = requireProfile(id);
        if (result.type != type) {
            throw new IllegalArgumentException(
                    "Engine profile " + id + " is " + result.type + ", expected " + type);
        }
        return result;
    }

    private ManagedEngineDefinition engineForProfile(ManagedProfile profile) {
        return requireEngine(profile.engineId);
    }

    private ManagedEngineDefinition findEngineByPath(String enginePath) {
        if (enginePath == null) {
            return null;
        }
        String normalized = enginePath.trim();
        for (ManagedEngineDefinition engine : engines.values()) {
            if (engine.definition.getEngine().equals(normalized)) {
                return engine;
            }
        }
        return null;
    }

    private boolean hasProfileOfType(String id, EngineConfigType type) {
        ManagedProfile profile = id == null ? null : profiles.get(id);
        return profile != null && profile.type == type;
    }

    private String resolveProfileId(String requested, String fallback, EngineConfigType type) {
        if (hasProfileOfType(requested, type)) {
            return requested;
        }
        if (hasProfileOfType(fallback, type)) {
            return fallback;
        }
        for (ManagedProfile profile : profiles.values()) {
            if (profile.type == type) {
                return profile.id;
            }
        }
        throw new IllegalStateException("No " + type + " engine profiles available");
    }

    private void bumpRuntimeVersions(String profileId) {
        if (profileId.equals(whitePlayerConfigId)) {
            whitePlayerVersion++;
        }
        if (profileId.equals(blackPlayerConfigId)) {
            blackPlayerVersion++;
        }
        if (profileId.equals(evaluationConfigId)) {
            evaluationVersion++;
        }
    }

    private String displayEngineName(String requestedName, UciEngineDefinition definition) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return definition.getEngineName();
    }

    private String typeLabel(EngineConfigType type) {
        return switch (type) {
            case PLAYER -> "Player";
            case EVALUATION -> "Evaluation";
            case DEEP_ANALYSIS -> "Deep Analysis";
        };
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
        private final EngineConfigType type;
        private final String engineId;
        private final int depth;
        private final int moveTimeSeconds;
        private final LinkedHashMap<String, String> optionValues;

        private ManagedProfile(
                String id,
                String name,
                EngineConfigType type,
                String engineId,
                int depth,
                int moveTimeSeconds,
                Map<String, String> optionValues) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.engineId = engineId;
            this.depth = depth;
            this.moveTimeSeconds = moveTimeSeconds;
            this.optionValues = new LinkedHashMap<>(optionValues);
        }
    }
}
