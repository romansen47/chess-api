package demo.chess.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class ChessApiApplicationContextTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    @Timeout(60)
    void applicationStartsWithProductionSpringWiring() throws Exception {
        Path home = Files.createDirectory(temporaryDirectory.resolve("home"));
        Path engines = Files.createDirectory(temporaryDirectory.resolve("engines"));

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("user.home", home.toString());
        properties.put(
                "chess.engine.config.file",
                temporaryDirectory.resolve("engine-configs.json").toString());
        properties.put(
                "chess.engine.discovery.directory",
                engines.toString());
        properties.put(
                "chess.opening.library.file",
                temporaryDirectory.resolve("openings.json").toString());
        properties.put(
                "chess.database.path",
                temporaryDirectory.resolve("chess.db").toString());

        Map<String, String> previousValues = setSystemProperties(properties);
        try (ConfigurableApplicationContext context =
                new SpringApplicationBuilder(ChessApiApplication.class)
                        .headless(true)
                        .web(WebApplicationType.SERVLET)
                        .properties(
                                "server.port=0",
                                "spring.main.banner-mode=off")
                        .run()) {
            assertTrue(context.isActive());
        } finally {
            restoreSystemProperties(previousValues);
        }
    }

    private Map<String, String> setSystemProperties(Map<String, String> properties) {
        Map<String, String> previousValues = new LinkedHashMap<>();
        properties.forEach((name, value) -> {
            previousValues.put(name, System.getProperty(name));
            System.setProperty(name, value);
        });
        return previousValues;
    }

    private void restoreSystemProperties(Map<String, String> previousValues) {
        previousValues.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }
}
