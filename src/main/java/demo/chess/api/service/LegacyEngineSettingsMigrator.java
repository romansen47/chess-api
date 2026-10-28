package demo.chess.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileAssignmentsDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.dto.ManagedEngineConfigDto;
import demo.chess.api.dto.UciOptionDto;
import demo.chess.api.service.EngineSettingsRegistry.ManagedEngineDefinition;
import demo.chess.definitions.engines.EngineConfigType;
import demo.chess.definitions.engines.UciEngineDefinition;

/** Converts the pre-profile combined engine format into current registry data. */
final class LegacyEngineSettingsMigrator {

    EngineProfileAssignmentsDto migrate(
            List<ManagedEngineConfigDto> legacyConfigs,
            EngineSettingsRegistry registry) {
        LinkedHashMap<String, String> engineIdsByPath = new LinkedHashMap<>();
        Assignments assignments = new Assignments();

        for (ManagedEngineConfigDto legacy : legacyConfigs) {
            if (legacy.getEngine() == null || legacy.getEngine().isBlank()) continue;
            String path = legacy.getEngine().trim();
            String engineId = engineIdsByPath.get(path);
            if (engineId == null) {
                EngineDefinitionDto engineDto = engineDefinition(legacy, path);
                UciEngineDefinition definition = registry.definitionFromDto(engineDto);
                engineId = UUID.randomUUID().toString();
                registry.engines().put(engineId, new ManagedEngineDefinition(
                        engineId,
                        registry.displayEngineName(engineDto.getName(), definition),
                        definition));
                engineIdsByPath.put(path, engineId);
            }

            EngineProfileDto profileDto = profile(legacy, engineId);
            String profileId = legacy.getId() == null || legacy.getId().isBlank()
                    ? UUID.randomUUID().toString()
                    : legacy.getId();
            registry.profiles().put(profileId, registry.profileFromDto(profileId, profileDto));
            EngineConfigType type = legacy.getType() == null || legacy.getType().isBlank()
                    ? inferType(legacy)
                    : EngineConfigType.fromValue(legacy.getType());
            assignments.seed(profileId, type);
        }
        return assignments.toDto();
    }

    private EngineDefinitionDto engineDefinition(ManagedEngineConfigDto legacy, String path) {
        EngineDefinitionDto dto = new EngineDefinitionDto();
        dto.setEngine(path);
        dto.setEngineName(legacy.getEngineName());
        dto.setEngineAuthor(legacy.getEngineAuthor());
        dto.setName(legacy.getEngineName());
        LinkedHashMap<String, UciOptionDto> options = new LinkedHashMap<>();
        for (Map.Entry<String, UciOptionDto> entry : legacy.getOptions().entrySet()) {
            UciOptionDto option = entry.getValue();
            if (option == null) continue;
            options.put(entry.getKey(), new UciOptionDto(
                    option.getType(),
                    option.getDefaultValue(),
                    option.getDefaultValue(),
                    option.getMin(),
                    option.getMax(),
                    option.getVars()));
        }
        dto.setOptions(options);
        return dto;
    }

    private EngineProfileDto profile(ManagedEngineConfigDto legacy, String engineId) {
        EngineProfileDto dto = new EngineProfileDto();
        dto.setName(legacy.getName());
        dto.setEngineId(engineId);
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, UciOptionDto> entry : legacy.getOptions().entrySet()) {
            UciOptionDto option = entry.getValue();
            if (option != null && !"button".equalsIgnoreCase(option.getType())) {
                values.put(entry.getKey(), option.getValue());
            }
        }
        dto.setOptionValues(values);
        return dto;
    }

    private EngineConfigType inferType(ManagedEngineConfigDto dto) {
        String name = dto.getName();
        if (name != null) {
            String normalized = name.toLowerCase();
            if (normalized.contains("deep analysis") || normalized.contains("deep-analysis")) {
                return EngineConfigType.DEEP_ANALYSIS;
            }
            if (normalized.contains("evaluation")) return EngineConfigType.EVALUATION;
        }
        return EngineConfigType.PLAYER;
    }

    private static final class Assignments {
        private String white;
        private String black;
        private String evaluation;
        private String deepAnalysis;

        private void seed(String profileId, EngineConfigType type) {
            switch (type) {
                case PLAYER -> {
                    if (white == null) white = profileId;
                    if (black == null) black = profileId;
                }
                case EVALUATION -> {
                    if (evaluation == null) evaluation = profileId;
                }
                case DEEP_ANALYSIS -> {
                    if (deepAnalysis == null) deepAnalysis = profileId;
                }
            }
        }

        private EngineProfileAssignmentsDto toDto() {
            return new EngineProfileAssignmentsDto(white, black, evaluation, deepAnalysis);
        }
    }
}
