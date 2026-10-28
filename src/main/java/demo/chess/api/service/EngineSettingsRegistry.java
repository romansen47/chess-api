package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.dto.UciOptionDto;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciOption;
import demo.chess.definitions.engines.UciOptionType;
import demo.chess.definitions.engines.UciSystemOptions;

/** Owns engine/profile models, validation and DTO conversion. */
final class EngineSettingsRegistry {

    private final LinkedHashMap<String, ManagedEngineDefinition> engines = new LinkedHashMap<>();
    private final LinkedHashMap<String, ManagedProfile> profiles = new LinkedHashMap<>();

    LinkedHashMap<String, ManagedEngineDefinition> engines() {
        return engines;
    }

    LinkedHashMap<String, ManagedProfile> profiles() {
        return profiles;
    }

    UciEngineDefinition definitionFromDto(EngineDefinitionDto dto) {
        if (dto.getEngine() == null || dto.getEngine().isBlank()) {
            throw new IllegalArgumentException("Engine path must not be blank");
        }
        LinkedHashMap<String, UciOption> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOptionDto> entry : dto.getOptions().entrySet()) {
            UciOptionDto source = entry.getValue();
            if (source == null) continue;
            UciOptionType type = UciOptionType.fromUciValue(source.getType());
            String configuredDefault = type == UciOptionType.BUTTON ? null : source.getDefaultValue();
            options.put(entry.getKey(), new UciOption(
                    type,
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

    ManagedProfile profileFromDto(String id, EngineProfileDto dto) {
        if (dto.getEngineId() == null || dto.getEngineId().isBlank()) {
            throw new IllegalArgumentException("An engine must be selected before a profile can be created");
        }
        ManagedEngineDefinition engine = requireEngine(dto.getEngineId().trim());
        for (String incomingName : dto.getOptionValues().keySet()) {
            if (UciSystemOptions.isSystemManaged(incomingName)) continue;
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
            UciOption option = entry.getValue();
            if (!option.isConfigurable() || UciSystemOptions.isSystemManaged(entry.getKey())) continue;
            String value = dto.getOptionValues().containsKey(entry.getKey())
                    ? dto.getOptionValues().get(entry.getKey())
                    : option.getDefaultValue();
            UciOption validator = option.copy();
            validator.setValue(value);
            normalizedValues.put(entry.getKey(), validator.getValue());
        }

        String name = dto.getName() == null || dto.getName().isBlank()
                ? engine.name + " Profile"
                : dto.getName().trim();
        return new ManagedProfile(id, name, engine.id, normalizedValues);
    }

    UciEngineConfig resolveRuntimeConfig(ManagedProfile profile, int depth, int moveTimeSeconds) {
        ManagedEngineDefinition engine = engineForProfile(profile);
        return engine.definition.createRuntimeConfig(
                Math.max(0, depth),
                Math.max(0, moveTimeSeconds),
                profile.optionValues);
    }

    EngineDefinitionDto toDto(ManagedEngineDefinition managed) {
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

    EngineProfileDto toDto(ManagedProfile managed) {
        EngineProfileDto dto = new EngineProfileDto();
        dto.setId(managed.id);
        dto.setName(managed.name);
        dto.setEngineId(managed.engineId);
        dto.setOptionValues(managed.optionValues);
        return dto;
    }

    LinkedHashMap<String, String> defaultOptionValues(UciEngineDefinition definition) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, UciOption> entry : definition.getOptions().entrySet()) {
            if (entry.getValue().isConfigurable()
                    && !UciSystemOptions.isSystemManaged(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue().getDefaultValue());
            }
        }
        return result;
    }

    ManagedEngineDefinition requireEngine(String id) {
        ManagedEngineDefinition result = engines.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown engine id: " + id);
        return result;
    }

    ManagedProfile requireProfile(String id) {
        ManagedProfile result = profiles.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown engine profile id: " + id);
        return result;
    }

    ManagedProfile validProfile(String id) {
        return id == null ? null : profiles.get(id);
    }

    ManagedEngineDefinition engineForProfile(ManagedProfile profile) {
        return requireEngine(profile.engineId);
    }

    ManagedEngineDefinition findEngineByPath(String enginePath) {
        if (enginePath == null || enginePath.isBlank()) return null;
        for (ManagedEngineDefinition engine : engines.values()) {
            if (sameEnginePath(engine.definition.getEngine(), enginePath)) return engine;
        }
        return null;
    }

    boolean sameEnginePath(String first, String second) {
        return canonicalEnginePath(first).equals(canonicalEnginePath(second));
    }

    String displayEngineName(String requestedName, UciEngineDefinition definition) {
        return requestedName != null && !requestedName.isBlank()
                ? requestedName.trim()
                : definition.getEngineName();
    }

    String fallbackEngineName(String path) {
        if (path == null || path.isBlank()) return "Engine";
        try {
            Path fileName = Path.of(path).getFileName();
            return fileName == null ? path : fileName.toString();
        } catch (Exception ignored) {
            return path;
        }
    }

    private String canonicalEnginePath(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            Path path = Path.of(value.trim()).toAbsolutePath().normalize();
            return Files.exists(path) ? path.toRealPath().toString() : path.toString();
        } catch (Exception ignored) {
            return value.trim();
        }
    }

    static final class ManagedEngineDefinition {
        final String id;
        final String name;
        final UciEngineDefinition definition;

        ManagedEngineDefinition(String id, String name, UciEngineDefinition definition) {
            this.id = id;
            this.name = name;
            this.definition = definition;
        }
    }

    static final class ManagedProfile {
        final String id;
        final String name;
        final String engineId;
        final LinkedHashMap<String, String> optionValues;

        ManagedProfile(String id, String name, String engineId, Map<String, String> optionValues) {
            this.id = id;
            this.name = name;
            this.engineId = engineId;
            this.optionValues = new LinkedHashMap<>(optionValues);
        }
    }
}
