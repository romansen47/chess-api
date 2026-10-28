package demo.chess.api.service;

/** Derives the display hierarchy for one exact opening line name. */
final class OpeningClassification {

    private OpeningClassification() {
    }

    static Classification classify(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            return new Classification("Opening", "Opening");
        }

        int separator = normalized.indexOf(':');
        if (separator < 1) {
            separator = normalized.indexOf(',');
        }
        if (separator < 1 || separator >= normalized.length() - 1) {
            return new Classification(normalized, normalized);
        }

        String family = normalized.substring(0, separator).trim();
        String variation = normalized.substring(separator + 1).trim();
        if (family.isEmpty() || variation.isEmpty()) {
            return new Classification(normalized, normalized);
        }
        return new Classification(family, variation);
    }

    record Classification(String family, String variation) {
    }
}
