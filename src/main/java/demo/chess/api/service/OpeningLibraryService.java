package demo.chess.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.OpeningDto;
import demo.chess.api.dto.OpeningStoreDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;

/**
 * Persistent registry of reusable opening main lines.
 *
 * <p>The store keeps only stable source data. SAN, final position, side to move
 * and playability are derived through the normal chess core whenever a DTO is
 * produced.</p>
 */
@Service
public class OpeningLibraryService {

    private static final Log logger = LogFactory.getLog(OpeningLibraryService.class);
    private static final String STORE_PROPERTY = "chess.opening.library.file";

    private final ObjectMapper objectMapper;
    private final Path storePath;
    private final GameLoader gameLoader = new GameLoader();
    private final LinkedHashMap<String, ManagedOpening> openings = new LinkedHashMap<>();
    private final Map<String, OpeningDto> dtoCache = new HashMap<>();
    private long version = 1L;

    public OpeningLibraryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.storePath = resolveStorePath();
        loadStore();
    }

    public synchronized List<OpeningDto> getOpenings() {
        return openings.values().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(OpeningDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized OpeningDto getOpening(String id) {
        return toDto(requireOpening(id));
    }

    public synchronized List<OpeningDto> importPgn(String content, String fileName)
            throws NoMoveFoundException, IOException {
        List<String> games = gameLoader.splitPgnGames(content);
        if (games.isEmpty()) {
            throw new IllegalArgumentException("PGN contains no opening line");
        }

        Set<String> knownOpeningKeys = new HashSet<>();
        for (ManagedOpening existing : openings.values()) {
            knownOpeningKeys.add(openingKey(existing));
        }

        List<ManagedOpening> imported = new ArrayList<>();
        for (int index = 0; index < games.size(); index++) {
            String pgn = games.get(index);
            ChessStartingPosition startingPosition = gameLoader.parsePgnStartingPosition(pgn);
            List<String> moves = gameLoader.parsePgnMoveList(pgn);
            if (moves.isEmpty()) {
                throw new IllegalArgumentException("Opening " + (index + 1) + " contains no moves");
            }

            Map<String, String> tags = gameLoader.parsePgnTags(pgn);
            ManagedOpening opening = new ManagedOpening(
                    UUID.randomUUID().toString(),
                    OpeningPgnMetadata.openingName(tags, fileName, index + 1, games.size()),
                    OpeningPgnMetadata.cleanTag(tags, "ECO"),
                    startingPosition.getId(),
                    List.copyOf(moves));
            if (!knownOpeningKeys.add(openingKey(opening))) {
                continue;
            }
            toDto(opening);
            imported.add(opening);
        }

        if (imported.isEmpty()) {
            return List.of();
        }

        imported.forEach(opening -> openings.put(opening.id(), opening));
        version++;
        persistStore();
        return imported.stream().map(this::toDto).toList();
    }

    public synchronized OpeningDto renameOpening(String id, String requestedName) {
        ManagedOpening existing = requireOpening(id);
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Opening name must not be blank");
        }

        ManagedOpening replacement = new ManagedOpening(
                existing.id(),
                name,
                existing.eco(),
                existing.startingPositionId(),
                existing.moves());
        openings.put(existing.id(), replacement);
        dtoCache.remove(existing.id());
        version++;
        persistStore();
        return toDto(replacement);
    }

    public synchronized void deleteOpening(String id) {
        requireOpening(id);
        openings.remove(id);
        dtoCache.remove(id);
        version++;
        persistStore();
    }

    private OpeningDto toDto(ManagedOpening opening) {
        OpeningDto cached = dtoCache.get(opening.id());
        if (cached != null) {
            return cached;
        }
        try {
            ChessStartingPosition startingPosition = ChessStartingPosition.of(opening.startingPositionId());
            Game simulation = Simulation.createSimulation(startingPosition);
            List<String> sanMoves = new ArrayList<>();

            for (String uci : opening.moves()) {
                Move move = LegalMoveResolver.resolveUci(simulation, uci);
                sanMoves.add(PgnNotation.toDisplayNotation(simulation, move));
                simulation.apply(move);
            }

            boolean playable = simulation.getState() == null
                    && !TerminalPositionEvaluator.hasInsufficientMatingMaterial(simulation)
                    && !simulation.getPlayer().getValidMoves(simulation).isEmpty();

            OpeningClassification.Classification classification =
                    OpeningClassification.classify(opening.name());
            OpeningDto dto = new OpeningDto();
            dto.setId(opening.id());
            dto.setName(opening.name());
            dto.setFamily(classification.family());
            dto.setVariation(classification.variation());
            dto.setEco(opening.eco());
            dto.setStartingPositionId(startingPosition.getId());
            dto.setInitialFen(startingPosition.initialFen());
            dto.setMoves(opening.moves());
            dto.setSanMoves(sanMoves);
            dto.setPosition(BoardPositionSerializer.toPositionString(simulation));
            dto.setSideToMove(simulation.getPlayer().getColor().name().toLowerCase(Locale.ROOT));
            dto.setPlayable(playable);
            dtoCache.put(opening.id(), dto);
            return dto;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException("Stored opening is invalid: " + opening.name(), e);
        }
    }

    private String openingKey(ManagedOpening opening) {
        return opening.startingPositionId() + "|" + String.join(" ", opening.moves());
    }

    private ManagedOpening requireOpening(String id) {
        ManagedOpening opening = id == null ? null : openings.get(id);
        if (opening == null) {
            throw new IllegalArgumentException("Unknown opening id: " + id);
        }
        return opening;
    }

    private void loadStore() {
        if (!Files.isRegularFile(storePath)) return;
        try {
            OpeningStoreDto store = objectMapper.readValue(storePath.toFile(), OpeningStoreDto.class);
            version = Math.max(1L, store.getVersion());
            Set<String> loadedOpeningKeys = new HashSet<>();
            boolean duplicateRemoved = false;
            for (OpeningStoreDto.Entry entry : store.getOpenings()) {
                if (entry.getId() == null || entry.getId().isBlank()
                        || entry.getName() == null || entry.getName().isBlank()) {
                    continue;
                }
                ManagedOpening opening = new ManagedOpening(
                        entry.getId(),
                        entry.getName().trim(),
                        entry.getEco() == null || entry.getEco().isBlank() ? null : entry.getEco().trim(),
                        entry.getStartingPositionId(),
                        List.copyOf(entry.getMoves()));
                toDto(opening);
                if (!loadedOpeningKeys.add(openingKey(opening))) {
                    duplicateRemoved = true;
                    continue;
                }
                openings.put(opening.id(), opening);
            }
            if (duplicateRemoved) {
                persistStore();
            }
        } catch (Exception e) {
            logger.warn("Could not load opening library " + storePath + ": " + e.getMessage());
            openings.clear();
            dtoCache.clear();
            version = 1L;
        }
    }

    private void persistStore() {
        try {
            Path parent = storePath.getParent();
            if (parent != null) Files.createDirectories(parent);

            OpeningStoreDto store = new OpeningStoreDto();
            store.setVersion(version);
            List<OpeningStoreDto.Entry> entries = new ArrayList<>();
            for (ManagedOpening opening : openings.values()) {
                OpeningStoreDto.Entry entry = new OpeningStoreDto.Entry();
                entry.setId(opening.id());
                entry.setName(opening.name());
                entry.setEco(opening.eco());
                entry.setStartingPositionId(opening.startingPositionId());
                entry.setMoves(opening.moves());
                entries.add(entry);
            }
            store.setOpenings(entries);

            Path temporary = storePath.resolveSibling(storePath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), store);
            try {
                Files.move(temporary, storePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            logger.warn("Could not persist opening library " + storePath + ": " + e.getMessage());
        }
    }

    private Path resolveStorePath() {
        String configured = System.getProperty(STORE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".chess", "openings.json")
                .toAbsolutePath()
                .normalize();
    }

    private record ManagedOpening(
            String id,
            String name,
            String eco,
            int startingPositionId,
            List<String> moves) {
    }
}
