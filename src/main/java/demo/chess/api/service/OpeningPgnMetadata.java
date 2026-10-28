package demo.chess.api.service;

import java.util.Map;

/** Derives reusable-opening labels from PGN header metadata. */
final class OpeningPgnMetadata {

    private OpeningPgnMetadata() {
    }

    static String openingName(
            Map<String, String> tags,
            String fileName,
            int index,
            int totalGames) {
        String tagged = firstNonBlank(
                cleanTag(tags, "Opening"),
                cleanTag(tags, "Variation"),
                cleanTag(tags, "Event"));
        if (tagged != null) return tagged;

        String base = fileName == null ? "" : fileName.trim();
        if (!base.isEmpty()) {
            int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
            if (slash >= 0) base = base.substring(slash + 1);
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
        }
        if (base.isBlank()) base = "Opening";
        return totalGames > 1 ? base + " #" + index : base;
    }

    static String cleanTag(Map<String, String> tags, String requestedKey) {
        if (tags == null || requestedKey == null) return null;
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (!requestedKey.equalsIgnoreCase(entry.getKey())) continue;
            String value = entry.getValue();
            return value == null || value.isBlank() ? null : value.trim();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
