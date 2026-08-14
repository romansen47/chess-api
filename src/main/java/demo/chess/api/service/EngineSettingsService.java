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
import demo.chess.api.dto.ManagedEngineConfigDto;
import demo.chess.api.dto.UciOptionDto;
import demo.chess.definitions.engines.Engine;
import demo.chess.definitions.engines.EngineConfigType;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineInspector;
import demo.chess.definitions.engines.UciOption;
import demo.chess.definitions.engines.UciOptionType;

/**
 * Central registry for named, reusable UCI engine configurations.
 *
 * A configuration is created from one concrete executable by running a UCI
 * handshake once. Both the executable and the configuration type are immutable;
 * another engine or another purpose therefore means creating another config.
 */
@Service
public class EngineSettingsService {

    private static final Log logger = LogFactory.getLog(EngineSettingsService.class);
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    private final String defaultEnginePath;
    private final ObjectMapper objectMapper;
    private final Path storePath;
    private final LinkedHashMap<String, ManagedConfig> configs = new LinkedHashMap<>();

    private String defaultPlayerConfigId;
    private String defaultEvaluationConfigId;
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
        this.evaluationConfigId = resolveConfigId(
                evaluationConfigId,
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION);
    }

    public synchronized EngineConfigOverviewDto getOverview() {
        List<ManagedEngineConfigDto> result = configs.values().stream()
                .map(this::toDto)
                .sorted(Comparator
                        .comparing(ManagedEngineConfigDto::getType)
                        .thenComparing(ManagedEngineConfigDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new EngineConfigOverviewDto(result, evaluationConfigId, defaultPlayerConfigId, version);
    }

    public synchronized ManagedEngineConfigDto inspectEngine(
            String enginePath,
            String requestedName,
            String typeValue) {
        EngineConfigType type = EngineConfigType.fromValue(typeValue);
        try {
            UciEngineConfig inspected = UciEngineInspector.inspect(enginePath, type);
            ManagedEngineConfigDto result = toDto(new ManagedConfig(
                    null,
                    displayName(requestedName, inspected),
                    inspected));
            result.setId(null);
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not inspect UCI engine at " + enginePath + ": " + e.getMessage(), e);
        }
    }

    public synchronized ManagedEngineConfigDto createConfig(ManagedEngineConfigDto incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Engine configuration must not be null");
        }
        if (incoming.getId() != null && !incoming.getId().isBlank()) {
            throw new IllegalArgumentException("A new engine configuration must not already have an id");
        }

        UciEngineConfig config = configFromDto(incoming);
        String id = UUID.randomUUID().toString();
        ManagedConfig managed = new ManagedConfig(id, displayName(incoming.getName(), config), config);
        configs.put(id, managed);
        version++;
        persistStore();
        return toDto(managed);
    }

    public synchronized ManagedEngineConfigDto updateConfig(String id, ManagedEngineConfigDto incoming) {
        ManagedConfig existing = requireManagedConfig(id);
        if (incoming == null) {
            throw new IllegalArgumentException("Engine configuration must not be null");
        }
        if (incoming.getEngine() == null || !existing.config.getEngine().equals(incoming.getEngine().trim())) {
            throw new IllegalArgumentException(
                    "The engine of an existing configuration is immutable. Create a new configuration for another engine.");
        }
        EngineConfigType incomingType = EngineConfigType.fromValue(incoming.getType());
        if (existing.config.getType() != incomingType) {
            throw new IllegalArgumentException(
                    "The type of an existing configuration is immutable. Create a new configuration for another purpose.");
        }

        UciEngineConfig updated = existing.config.copy();
        updated.setDepth(incoming.getDepth());
        updated.setMoveTimeSeconds(incoming.getMoveTimeSeconds());

        for (Map.Entry<String, UciOption> entry : updated.getOptions().entrySet()) {
            if (!entry.getValue().isConfigurable()) {
                continue;
            }
            UciOptionDto incomingOption = incoming.getOptions().get(entry.getKey());
            if (incomingOption != null) {
                updated.setOptionValue(entry.getKey(), incomingOption.getValue());
            }
        }

        ManagedConfig replacement = new ManagedConfig(
                existing.id,
                displayName(incoming.getName(), updated),
                updated);
        configs.put(existing.id, replacement);
        version++;
        if (existing.id.equals(whitePlayerConfigId)) {
            whitePlayerVersion++;
        }
        if (existing.id.equals(blackPlayerConfigId)) {
            blackPlayerVersion++;
        }
        if (existing.id.equals(evaluationConfigId)) {
            evaluationVersion++;
        }
        persistStore();
        return toDto(replacement);
    }

    public synchronized void deleteConfig(String id) {
        ManagedConfig existing = requireManagedConfig(id);
        if (existing.id.equals(defaultPlayerConfigId) || existing.id.equals(defaultEvaluationConfigId)) {
            throw new IllegalArgumentException("Default engine configurations cannot be deleted");
        }
        if (existing.id.equals(whitePlayerConfigId)
                || existing.id.equals(blackPlayerConfigId)
                || existing.id.equals(evaluationConfigId)) {
            throw new IllegalArgumentException("Engine configuration is currently in use and cannot be deleted");
        }
        configs.remove(existing.id);
        version++;
        persistStore();
    }

    public synchronized String setEvaluationConfigId(String configId) {
        String resolved = resolveConfigId(
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
        String resolvedWhite = resolveConfigId(
                whiteConfigId,
                defaultPlayerConfigId,
                EngineConfigType.PLAYER);
        String resolvedBlack = resolveConfigId(
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
        return resolveConfigId(configId, defaultPlayerConfigId, EngineConfigType.PLAYER);
    }

    public synchronized String getDefaultPlayerConfigId() {
        return defaultPlayerConfigId;
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
        return requireManagedConfig(id).config.copy();
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return requireManagedConfig(whitePlayerConfigId, EngineConfigType.PLAYER).config.copy();
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return requireManagedConfig(blackPlayerConfigId, EngineConfigType.PLAYER).config.copy();
    }

    public synchronized UciEngineConfig toEvaluationEngineConfig() {
        return requireManagedConfig(evaluationConfigId, EngineConfigType.EVALUATION).config.copy();
    }

    public synchronized String getWhitePlayerEnginePath() {
        return requireManagedConfig(whitePlayerConfigId, EngineConfigType.PLAYER).config.getEngine();
    }

    public synchronized String getBlackPlayerEnginePath() {
        return requireManagedConfig(blackPlayerConfigId, EngineConfigType.PLAYER).config.getEngine();
    }

    public synchronized String getEvaluationEnginePath() {
        return requireManagedConfig(evaluationConfigId, EngineConfigType.EVALUATION).config.getEngine();
    }

    public synchronized String getWhitePlayerEngineName() {
        return requireManagedConfig(whitePlayerConfigId, EngineConfigType.PLAYER).config.getEngineName();
    }

    public synchronized String getBlackPlayerEngineName() {
        return requireManagedConfig(blackPlayerConfigId, EngineConfigType.PLAYER).config.getEngineName();
    }

    public synchronized String getEvaluationEngineName() {
        return requireManagedConfig(evaluationConfigId, EngineConfigType.EVALUATION).config.getEngineName();
    }

    public synchronized String getEngineName(String enginePath) {
        if (enginePath != null) {
            for (ManagedConfig managed : configs.values()) {
                if (managed.config.getEngine().equals(enginePath.trim())) {
                    return managed.config.getEngineName();
                }
            }
        }
        try {
            return UciEngineInspector.inspect(enginePath, EngineConfigType.PLAYER).getEngineName();
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

    private UciEngineConfig configFromDto(ManagedEngineConfigDto dto) {
        if (dto.getEngine() == null || dto.getEngine().isBlank()) {
            throw new IllegalArgumentException("Engine path must not be blank");
        }
        EngineConfigType configType = EngineConfigType.fromValue(dto.getType());

        LinkedHashMap<String, UciOption> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOptionDto> entry : dto.getOptions().entrySet()) {
            UciOptionDto source = entry.getValue();
            if (source == null) {
                continue;
            }
            UciOptionType optionType = UciOptionType.fromUciValue(source.getType());
            options.put(entry.getKey(), new UciOption(
                    optionType,
                    source.getDefaultValue(),
                    source.getValue(),
                    source.getMin(),
                    source.getMax(),
                    source.getVars()));
        }

        UciEngineConfig result = new UciEngineConfig(
                configType,
                dto.getEngine(),
                dto.getEngineName(),
                dto.getEngineAuthor(),
                options);
        result.setDepth(dto.getDepth());
        result.setMoveTimeSeconds(dto.getMoveTimeSeconds());
        return result;
    }

    private ManagedEngineConfigDto toDto(ManagedConfig managed) {
        ManagedEngineConfigDto dto = new ManagedEngineConfigDto();
        dto.setId(managed.id);
        dto.setName(managed.name);
        dto.setType(managed.config.getType().name());
        dto.setEngine(managed.config.getEngine());
        dto.setEngineName(managed.config.getEngineName());
        dto.setEngineAuthor(managed.config.getEngineAuthor());
        dto.setDepth(managed.config.getDepth());
        dto.setMoveTimeSeconds(managed.config.getMoveTimeSeconds());

        LinkedHashMap<String, UciOptionDto> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOption> entry : managed.config.getOptions().entrySet()) {
            UciOption option = entry.getValue();
            options.put(entry.getKey(), new UciOptionDto(
                    option.getType().toUciValue(),
                    option.getDefaultValue(),
                    option.getValue(),
                    option.getMin(),
                    option.getMax(),
                    option.getVars()));
        }
        dto.setOptions(options);
        return dto;
    }

    private void ensureDefaults() {
        boolean hasPlayerDefault = hasConfigOfType(defaultPlayerConfigId, EngineConfigType.PLAYER);
        boolean hasEvaluationDefault = hasConfigOfType(defaultEvaluationConfigId, EngineConfigType.EVALUATION);
        if (hasPlayerDefault && hasEvaluationDefault) {
            evaluationConfigId = resolveConfigId(
                    evaluationConfigId,
                    defaultEvaluationConfigId,
                    EngineConfigType.EVALUATION);
            return;
        }

        UciEngineConfig inspectedPlayer;
        try {
            inspectedPlayer = UciEngineInspector.inspect(defaultEnginePath, EngineConfigType.PLAYER);
        } catch (Exception e) {
            logger.warn("Could not inspect default UCI engine at " + defaultEnginePath
                    + ". Starting with an empty option map: " + e.getMessage());
            inspectedPlayer = new UciEngineConfig(
                    EngineConfigType.PLAYER,
                    defaultEnginePath,
                    fallbackEngineName(defaultEnginePath),
                    "",
                    Map.of());
        }

        if (!hasPlayerDefault) {
            UciEngineConfig playerConfig = inspectedPlayer.copy();
            setOptionIfPresent(playerConfig, "Threads", "8");
            setOptionIfPresent(playerConfig, "Hash", "1024");
            setOptionIfPresent(playerConfig, "MultiPV", "1");

            defaultPlayerConfigId = UUID.randomUUID().toString();
            configs.put(defaultPlayerConfigId, new ManagedConfig(
                    defaultPlayerConfigId,
                    inspectedPlayer.getEngineName() + " · Player",
                    playerConfig));
        }

        if (!hasEvaluationDefault) {
            UciEngineConfig evaluationConfig = copyAsType(inspectedPlayer, EngineConfigType.EVALUATION);
            setOptionIfPresent(evaluationConfig, "Threads", "2");
            setOptionIfPresent(evaluationConfig, "Hash", "256");
            setOptionIfPresent(evaluationConfig, "MultiPV", "3");

            defaultEvaluationConfigId = UUID.randomUUID().toString();
            configs.put(defaultEvaluationConfigId, new ManagedConfig(
                    defaultEvaluationConfigId,
                    inspectedPlayer.getEngineName() + " · Evaluation",
                    evaluationConfig));
        }

        evaluationConfigId = resolveConfigId(
                evaluationConfigId,
                defaultEvaluationConfigId,
                EngineConfigType.EVALUATION);
        persistStore();
    }

    private UciEngineConfig copyAsType(UciEngineConfig source, EngineConfigType type) {
        UciEngineConfig result = new UciEngineConfig(
                type,
                source.getEngine(),
                source.getEngineName(),
                source.getEngineAuthor(),
                source.getOptions());
        result.setDepth(source.getDepth());
        result.setMoveTimeSeconds(source.getMoveTimeSeconds());
        return result;
    }

    private void setOptionIfPresent(UciEngineConfig config, String name, String value) {
        if (config.getOption(name) == null || !config.getOption(name).isConfigurable()) {
            return;
        }
        try {
            config.setOptionValue(name, value);
        } catch (IllegalArgumentException e) {
            logger.debug("Could not set default UCI option " + name + "=" + value + ": " + e.getMessage());
        }
    }

    private void loadStore() {
        if (!Files.isRegularFile(storePath)) {
            return;
        }
        try {
            EngineConfigStoreDto store = objectMapper.readValue(storePath.toFile(), EngineConfigStoreDto.class);
            defaultPlayerConfigId = store.getDefaultPlayerConfigId();
            defaultEvaluationConfigId = store.getDefaultEvaluationConfigId();
            evaluationConfigId = store.getEvaluationConfigId();

            for (ManagedEngineConfigDto dto : store.getConfigs()) {
                if (dto.getId() == null || dto.getId().isBlank()) {
                    continue;
                }
                if (dto.getType() == null || dto.getType().isBlank()) {
                    dto.setType(inferLegacyType(dto).name());
                }
                UciEngineConfig config = configFromDto(dto);
                configs.put(dto.getId(), new ManagedConfig(
                        dto.getId(),
                        displayName(dto.getName(), config),
                        config));
            }
        } catch (Exception e) {
            logger.warn("Could not load engine config store " + storePath + ": " + e.getMessage());
            configs.clear();
            defaultPlayerConfigId = null;
            defaultEvaluationConfigId = null;
            evaluationConfigId = null;
        }
    }

    private EngineConfigType inferLegacyType(ManagedEngineConfigDto dto) {
        if (dto.getId() != null
                && (dto.getId().equals(defaultEvaluationConfigId) || dto.getId().equals(evaluationConfigId))) {
            return EngineConfigType.EVALUATION;
        }
        String name = dto.getName();
        if (name != null && name.toLowerCase().contains("evaluation")) {
            return EngineConfigType.EVALUATION;
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
            List<ManagedEngineConfigDto> dtoConfigs = new ArrayList<>();
            for (ManagedConfig managed : configs.values()) {
                dtoConfigs.add(toDto(managed));
            }
            store.setConfigs(dtoConfigs);
            store.setDefaultPlayerConfigId(defaultPlayerConfigId);
            store.setDefaultEvaluationConfigId(defaultEvaluationConfigId);
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

    private ManagedConfig requireManagedConfig(String id) {
        ManagedConfig result = configs.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Unknown engine config id: " + id);
        }
        return result;
    }

    private ManagedConfig requireManagedConfig(String id, EngineConfigType type) {
        ManagedConfig result = requireManagedConfig(id);
        if (result.config.getType() != type) {
            throw new IllegalArgumentException(
                    "Engine config " + id + " is " + result.config.getType() + ", expected " + type);
        }
        return result;
    }

    private boolean hasConfigOfType(String id, EngineConfigType type) {
        ManagedConfig managed = id == null ? null : configs.get(id);
        return managed != null && managed.config.getType() == type;
    }

    private String resolveConfigId(String requested, String fallback, EngineConfigType type) {
        if (hasConfigOfType(requested, type)) {
            return requested;
        }
        if (hasConfigOfType(fallback, type)) {
            return fallback;
        }
        for (ManagedConfig managed : configs.values()) {
            if (managed.config.getType() == type) {
                return managed.id;
            }
        }
        throw new IllegalStateException("No " + type + " engine configurations available");
    }

    private String displayName(String requestedName, UciEngineConfig config) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        return config.getEngineName();
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

    private static final class ManagedConfig {
        private final String id;
        private final String name;
        private final UciEngineConfig config;

        private ManagedConfig(String id, String name, UciEngineConfig config) {
            this.id = id;
            this.name = name;
            this.config = config;
        }
    }
}
