package demo.chess.api.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnAnnotationParser;
import demo.chess.notation.PgnMoveAnnotation;
import demo.chess.save.GameSaver;

/**
 * Converts ChessAnalysisTool diagnostic exports into normal annotated PGN.
 *
 * <p>The generic chess PGN parser deliberately remains unaware of application-
 * specific diagnostic metadata. This sanitizer keeps only standard persistable
 * annotations, most importantly [%eval ...], and drops diagnostic fields such
 * as depth, classifier data and principal variations.</p>
 */
final class ChessAnalysisDiagnosticPgnSanitizer {

    private static final String DIAGNOSTIC_FORMAT_PREFIX =
            "ChessAnalysisTool-Diagnostic-";
    private static final Pattern BRACE_COMMENT = Pattern.compile(
            "\\{([^}]*)}",
            Pattern.DOTALL);
    private static final Pattern SEMICOLON_COMMENT = Pattern.compile(
            "(?m);([^\\r\\n]*)");
    private static final Pattern EVAL_TAG = Pattern.compile(
            "(?i)\\[%eval\\s+[^\\]]+]");

    private final GameLoader gameLoader = new GameLoader();
    private final PgnAnnotationParser annotationParser = new PgnAnnotationParser();
    private final GameSaver gameSaver = new GameSaver();

    String sanitize(String content) throws IOException, NoMoveFoundException {
        Map<String, String> tags = new LinkedHashMap<>(
                gameLoader.parsePgnTags(content));
        String format = tags.get("AnalysisFormat");
        if (format == null || !format.startsWith(DIAGNOSTIC_FORMAT_PREFIX)) {
            return content;
        }

        ChessStartingPosition startingPosition =
                gameLoader.parsePgnStartingPosition(content);
        String parserInput = keepOnlyPersistableCommentTags(content);
        Map<Integer, PgnMoveAnnotation> annotations =
                annotationParser.parse(parserInput, startingPosition);

        Simulation simulation = Simulation.createSimulation(startingPosition);
        gameLoader.loadGame(gameLoader.parsePgnMoveList(content), simulation);

        tags.remove("AnalysisFormat");
        return gameSaver.toPgn(simulation.getMoveList(), tags, annotations);
    }

    private String keepOnlyPersistableCommentTags(String content) {
        String braceSanitized = rewriteComments(content, BRACE_COMMENT);
        return rewriteComments(braceSanitized, SEMICOLON_COMMENT);
    }

    private String rewriteComments(
            String content,
            Pattern commentPattern) {
        Matcher matcher = commentPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String evaluations = evaluationTags(matcher.group(1));
            String replacement = evaluations.isEmpty()
                    ? ""
                    : "{ " + evaluations + " }";
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String evaluationTags(String comment) {
        Matcher matcher = EVAL_TAG.matcher(comment == null ? "" : comment);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(matcher.group());
        }

        return result.toString();
    }
}
