package demo.chess.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import demo.chess.api.dto.EngineConfigStoreDto;
import tools.jackson.databind.ObjectMapper;

/** Atomic JSON persistence for engine settings. */
final class EngineSettingsStore {

    private static final Log logger = LogFactory.getLog(EngineSettingsStore.class);
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    private final ObjectMapper objectMapper;
    private final Path path;

    EngineSettingsStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.path = resolvePath();
    }

    EngineConfigStoreDto load() {
        if (!Files.isRegularFile(path)) return null;
        try {
            return objectMapper.readValue(path.toFile(), EngineConfigStoreDto.class);
        } catch (Exception exception) {
            logger.warn("Could not load engine config store " + path + ": " + exception.getMessage());
            return null;
        }
    }

    void save(EngineConfigStoreDto store) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), store);
            try {
                Files.move(temporary, path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            logger.warn("Could not persist engine config store " + path + ": " + exception.getMessage());
        }
    }

    private Path resolvePath() {
        String configured = System.getProperty(STORE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".chess", "engine-configs.json")
                .toAbsolutePath()
                .normalize();
    }
}
