# Chess API

`chess-api` is the Spring Boot application layer of the Chess Analysis Tool. It connects the React frontend with the chess core, UCI engines, analysis services, and the embedded chess database. Chess rules themselves belong to the `chess` core module; this module coordinates use cases and maps core objects to stable HTTP DTOs.

## Architectural responsibilities

The API layer owns application state and orchestration: the current live game, an optionally imported analysis game, engine/profile selection, analysis sessions, database workflows, and REST DTOs. It must not reimplement chess rules that already exist in the core.

The intended dependency direction is:

```text
REST controller
    -> application service
        -> DTO mapper / policy component
        -> chess core / chess-database / engine integration
```

Controllers should remain thin. In particular, they should not contain Chess960 castling rules, UCI encoding rules, board traversal logic, executable probing, or engine fallback policy. Those concerns have dedicated application/core components.

### DTO boundary

REST DTOs are transport objects and deliberately contain primitive/string representations rather than exposing mutable core objects. `GameSettingsDto` carries the Scharnagl `startingPositionId`; `UciGameDto` additionally carries `initialFen` because a move list without its initial position cannot reconstruct a Chess960 game.

`PossibleMovesResponse` currently exposes two views for compatibility:

- `targets`: legacy interaction targets used by older frontend code;
- `moves`: the canonical rich move descriptors including UCI and Chess960 castling metadata.

For Chess960 castling, the rich descriptor exposes the king destination and the rook's source/destination explicitly. New API code should use these fields instead of inferring castling geometry from coordinate distances.

## Chess960 and game context

The API treats classical chess as Chess960 Scharnagl position **518**, matching the core. A new game is created from `startingPositionId`, and imported PGNs derive their start position from their Chess960/FEN tags.

Start-position context must survive every replay boundary:

```text
new game / PGN import
        -> core Game
        -> AnalysisGameContext (ChessStartingPosition + moves)
        -> AnalysisGameReplayService
        -> snapshot / replay / assessment / variation / database lookup
        -> frontend DTO (startingPositionId + initialFen)
```

`UciGameService` owns the distinction between the live game and an imported analysis game. It exposes the selected history to analysis code as one atomic `AnalysisGameContext`; consumers must not read a move list and recreate a bare `Simulation.createSimulation()`, because that factory intentionally means classical position 518. `AnalysisGameReplayService` is the canonical reconstruction boundary for historical positions and temporary variations and always starts from the context's real Chess960 position. Imported game, PGN tags, optional database id and annotations remain grouped in `ImportedGameContext` so those values cannot drift independently. `UciGameMoveMapper` performs per-ply DTO reconstruction for snapshots.

The API never uses `Move.toString()` as a protocol contract. UCI-facing code goes through the core `UciMoveCodec`; this matters especially for Chess960 castling, where protocol coordinates can differ from the king's board destination. Computer-engine move responses follow the same rule.

## Engine capabilities and fallback policy

The application supports multiple UCI engine definitions and reusable engine profiles. Profiles can be assigned independently to White, Black, live evaluation, and deep analysis. Engine configuration is persisted by default in `~/.chess/engine-configs.json`; the location can be overridden with the `chess.engine.config.file` system property.

Native-engine handling is split into two layers:

- `NativeEngineProbe` performs OS/file checks, a real UCI handshake, and variant-capability checks for one executable;
- `EngineAvailabilityService` applies feature/role policy and exposes capability results.

A responsive UCI executable is **not automatically Chess960-capable**. For a non-518 game, the native executable must advertise the standard `UCI_Chess960` check option. Otherwise its capability result is `CHESS960_UNSUPPORTED`: it may still be used for classical chess, but it is not a candidate for that Chess960 operation.

Capability checks are contextual:

- White/Black player and live-evaluation roles are checked against the current live game's starting position;
- Deep Analysis is checked against the currently selected analysis game, which may be an imported PGN with a different Chess960 starting position.

Deep-analysis profile selection is isolated in `DeepAnalysisProfileResolver`. Resolution order is deterministic: explicitly requested profile, configured deep-analysis default, then the remaining configured native profiles. Each candidate is probed against the selected starting position, so a healthy classical-only engine is skipped in favor of a later Chess960-capable native engine when necessary.

**Browser Stockfish is a live-evaluation fallback only.** It is intentionally outside native deep-analysis resolution. If no configured native engine can be used for the selected variant, deep analysis is unavailable and the frontend must not enable "Analyse starten" merely because Browser Stockfish works.

`AnalysisReplayService` resolves native capability against `UciGameService.getAnalysisStartingPosition()` before creating a replay. Native process startup is delegated to `DeepAnalysisEngineFactory`. Move-quality classification receives canonical UCI produced by the core codec, so Chess960 castling is classified against the same move representation emitted by the engine.

### System-managed UCI options

`UCI_Chess960` is protocol/game state, not a reusable profile preference. The engine definition keeps it because its presence is a capability signal, but `EngineProfileDto` excludes it from profile option values and ignores it when received from older clients or persisted stores. Default profiles omit it as well. The core engine adapter sets the option from the active `Game` immediately before a search.

This boundary prevents a stored profile default such as `UCI_Chess960=false` from overwriting the runtime mode between two searches on the same persistent engine process.

## Engine discovery

Automatic discovery scans the configured engine directory, `/usr/games` by default. On a normal Windows installation this project convention corresponds to `C:\usr\games`. The directory can be overridden with the `chess.engine.discovery.directory` system property.

For safety, automatic discovery does **not** execute every file in that directory. It only considers regular executable files whose filename contains `stockfish` or `lc0`, case-insensitively. A candidate is accepted only after it successfully responds to a UCI handshake. Duplicate paths resolving to the same executable are ignored.

Other UCI engines, or engines stored elsewhere, can be added explicitly. The application includes a native engine-file selection workflow for local graphical environments, including Windows/WSL handling. Explicitly selected engines are validated as executable files and inspected through UCI before being registered.

Keep engine-specific companion files with the executable when required. Lc0 distributions commonly depend on a neural-network weights file and may also need platform-specific runtime libraries such as DLL files. Moving only the executable can therefore leave an otherwise valid engine unable to start.

## PGN import, export and snapshots

PGN import resolves the starting position before replaying moves. Chess960 imports therefore remain tied to their initial FEN throughout analysis. The same resolved position is passed to annotation parsing and diagnostic-PGN sanitization; auxiliary PGN processing must never recreate a standard-518 board while walking Chess960 SAN. Export delegates notation and setup-tag generation to the core `GameSaver`, rather than reconstructing Chess960 PGN rules in the API.

A snapshot contains:

- current compact board position;
- complete per-ply UCI/SAN history;
- side to move and player names;
- optional database identity and annotations;
- `startingPositionId` and `initialFen`.

These fields form one replay contract with the frontend. New features that create a simulated/replayed game must preserve the same starting-position context instead of silently assuming position 518.

## Chess database

Database-library uploads are PGN files. Bulk imports run asynchronously and expose progress and cancellation state. The backend is configured to accept very large multipart uploads, but actual import time depends on library size, storage speed, and the number of games and positions. Large collections can take many minutes or several hours to import.

The embedded SQLite database is stored by default at `~/.chess/database/chess.db`. Override this with the `chess.database.path` system property when necessary. Database import is isolated: staged games and position statistics are published only after a successful import; failed or cancelled imports are cleaned up.

Position statistics are Chess960-aware. The API passes both the starting-position id and canonical UCI history into `chess-database`, so two identical-looking move prefixes from different initial positions do not share database statistics accidentally.

## Coding rules for new API work

When extending the application, keep these boundaries intact:

1. Chess legality, castling geometry, SAN and UCI semantics belong to `chess`.
2. Controllers validate HTTP shape and delegate; they do not implement domain rules.
3. DTO mapping belongs in mapper/serializer components, not controllers.
4. Imported/replay state must carry its Chess960 starting position end-to-end.
5. Browser evaluation capability and native deep-analysis capability are separate concepts.
6. Engine executable health, advertised variant capability and engine/profile selection policy are separate concerns.
7. System-managed UCI options are not profile preferences.
8. A fallback candidate must satisfy the capability requirements of the selected game, not merely answer `uci`.
9. Public API compatibility fields may remain temporarily, but new code should use the explicit canonical representation documented above.

## Build and tests

The module uses Spring Boot 3.3.5 and Java 21. Its Maven build also runs `npm ci` and `npm run build` in the sibling `chess-frontend` project and packages the resulting frontend into the Spring Boot application. For a complete build, use the parent `chess-project` repository with its submodules initialized.

Chess960-related changes should be verified at several boundaries: settings/start-game creation, rich legal-move DTOs, PGN import/snapshot replay, native-engine capability/fallback, analysis replay, and the full parent Maven reactor. Capability tests deliberately distinguish a responsive classical-only fake UCI engine from one that advertises `UCI_Chess960`. The parent CI additionally type-checks and tests the frontend, runs the browser Stockfish smoke test, and builds the production bundle.
