package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import tools.jackson.databind.ObjectMapper;

class EngineSettingsServiceSpringWiringTest {

    private static final String CONFIG_FILE_PROPERTY = "chess.engine.config.file";
    private static final String DISCOVERY_DIRECTORY_PROPERTY =
            EngineDiscoveryService.DIRECTORY_PROPERTY;

    @TempDir
    Path temporaryDirectory;

    @Test
    void springCreatesServiceThroughProductionConstructor() throws Exception {
        Path discoveryDirectory = Files.createDirectory(
                temporaryDirectory.resolve("engines"));
        Path configFile = temporaryDirectory.resolve("engine-configs.json");

        String previousConfigFile = System.getProperty(CONFIG_FILE_PROPERTY);
        String previousDiscoveryDirectory = System.getProperty(
                DISCOVERY_DIRECTORY_PROPERTY);
        System.setProperty(CONFIG_FILE_PROPERTY, configFile.toString());
        System.setProperty(
                DISCOVERY_DIRECTORY_PROPERTY,
                discoveryDirectory.toString());

        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(
                    EngineDiscoveryService.class,
                    EngineDiscoveryService::new);
            context.register(EngineSettingsService.class);

            context.refresh();

            assertNotNull(context.getBean(EngineSettingsService.class));
        } finally {
            restoreProperty(CONFIG_FILE_PROPERTY, previousConfigFile);
            restoreProperty(
                    DISCOVERY_DIRECTORY_PROPERTY,
                    previousDiscoveryDirectory);
        }
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }
}
