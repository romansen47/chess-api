package demo.chess.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Verifies that reusable profiles contain only user-managed UCI values. */
class EngineProfileDtoTest {

    @Test
    void removesSystemManagedChess960ValueFromProfile() {
        EngineProfileDto profile = new EngineProfileDto();
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("Hash", "256");
        values.put("uci_chess960", "false");

        profile.setOptionValues(values);

        assertEquals(Map.of("Hash", "256"), profile.getOptionValues());
        assertFalse(profile.getOptionValues().containsKey("uci_chess960"));
    }
}
