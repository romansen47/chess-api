# Chess API

`chess-api` is the Spring Boot application layer of the Chess Analysis Tool. It connects the React frontend with the chess core, UCI engines, analysis services, and the embedded chess database.

## Main capabilities

The API exposes operations for live engine evaluation, deep game analysis/replay, engine configuration and profiles, engine process management, normal game lifecycle and computer moves, PGN import/export, and local chess-database import/search/load workflows.

The application supports multiple UCI engine definitions and reusable engine profiles. Profiles can be assigned independently to White, Black, live evaluation, and deep analysis. Engine configuration is persisted by default in `~/.chess/engine-configs.json`; the location can be overridden with the `chess.engine.config.file` system property.

## Engine discovery

Automatic discovery scans the configured engine directory, `/usr/games` by default. On a normal Windows installation this project convention corresponds to `C:\usr\games`. The directory can be overridden with the `chess.engine.discovery.directory` system property.

For safety, automatic discovery does **not** execute every file in that directory. It only considers regular executable files whose filename contains `stockfish` or `lc0`, case-insensitively. A candidate is accepted only after it successfully responds to a UCI handshake. Duplicate paths resolving to the same executable are ignored.

Other UCI engines, or engines stored elsewhere, can be added explicitly. The application includes a native engine-file selection workflow for local graphical environments, including Windows/WSL handling. Explicitly selected engines are validated as executable files and inspected through UCI before being registered.

Keep engine-specific companion files with the executable when required. Lc0 distributions commonly depend on a neural-network weights file and may also need platform-specific runtime libraries such as DLL files. Moving only the executable can therefore leave an otherwise valid engine unable to start.

## Chess database

Database-library uploads are PGN files. Bulk imports run asynchronously and expose progress and cancellation state. The backend is configured to accept very large multipart uploads, but actual import time depends on library size, storage speed, and the number of games and positions. Large collections can take many minutes or several hours to import.

The embedded SQLite database is stored by default at `~/.chess/database/chess.db`. Override this with the `chess.database.path` system property when necessary. Database import is isolated: staged games and position statistics are published only after a successful import; failed or cancelled imports are cleaned up.

## Build

The module uses Spring Boot 3.3.5 and Java 21. Its Maven build also runs `npm ci` and `npm run build` in the sibling `chess-frontend` project and packages the resulting frontend into the Spring Boot application. For a complete build, use the parent `chess-project` repository with its submodules initialized.
