package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import demo.chess.notation.PgnAnnotationParser;
import demo.chess.notation.PgnMoveAnnotation;

class ChessAnalysisDiagnosticPgnSanitizerTest {

    private final ChessAnalysisDiagnosticPgnSanitizer sanitizer =
            new ChessAnalysisDiagnosticPgnSanitizer();
    private final PgnAnnotationParser annotationParser =
            new PgnAnnotationParser();

    @Test
    void leavesNormalPgnUntouched() throws Exception {
        String pgn = """
                [Event "Normal"]
                [Result "*"]

                1. e4 {Human comment. [%eval 0.35]} e5 *
                """;

        assertEquals(pgn, sanitizer.sanitize(pgn));
    }

    @Test
    void stripsDiagnosticMetadataAndKeepsEvaluation() throws Exception {
        String pgn = """
                [Event "ChessAnalysisTool diagnostic export"]
                [White "White"]
                [Black "Black"]
                [Result "*"]
                [AnalysisFormat "ChessAnalysisTool-Diagnostic-v2"]

                1. e4 { [%eval 0.31] [%depth 15] class=GOOD symbol=! prePv1Eval=0.20 prePv1Depth=15 prePv1="Nf3 Nc6 Bb5" postPv1="e5 Nf3 Nc6" }
                e5 { [%eval 0.10] [%depth 15] postPv1="Nf3 Nc6" } *
                """;

        String sanitized = sanitizer.sanitize(pgn);

        assertFalse(sanitized.contains("AnalysisFormat"));
        assertFalse(sanitized.contains("%depth"));
        assertFalse(sanitized.contains("prePv"));
        assertFalse(sanitized.contains("postPv"));
        assertFalse(sanitized.contains("class=GOOD"));

        Map<Integer, PgnMoveAnnotation> annotations =
                annotationParser.parse(sanitized);
        assertEquals("0.31", annotations.get(1).evaluation());
        assertNull(annotations.get(1).comment());
        assertEquals("0.10", annotations.get(2).evaluation());
        assertNull(annotations.get(2).comment());
        assertTrue(annotations.get(1).variations().isEmpty());
    }
    @Test
    void sanitizesChess960DiagnosticPgnWithItsDeclaredStartPosition() throws Exception {
        String pgn = """
                [Event "Chess960 diagnostic"]
                [Variant "Chess960"]
                [SetUp "1"]
                [FEN "rqnbknbr/pppppppp/8/8/8/8/PPPPPPPP/RQNBKNBR w HAha - 0 1"]
                [Result "*"]
                [AnalysisFormat "ChessAnalysisTool-Diagnostic-v2"]

                1. h3 c6 2. Bh2 { [%eval 0.20] [%depth 12] class=GOOD } Bc7 *
                """;

        String sanitized = sanitizer.sanitize(pgn);

        assertTrue(sanitized.contains("[Variant \"Chess960\"]"));
        assertTrue(sanitized.contains("[FEN \"rqnbknbr/pppppppp/8/8/8/8/PPPPPPPP/RQNBKNBR w HAha - 0 1\"]"));
        assertFalse(sanitized.contains("AnalysisFormat"));
        assertFalse(sanitized.contains("%depth"));
        assertFalse(sanitized.contains("class=GOOD"));

        Map<Integer, PgnMoveAnnotation> annotations = annotationParser.parse(sanitized);
        assertEquals("0.20", annotations.get(3).evaluation());
    }

}
