package demo.chess.api.service;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

/**
 * Opens a native file chooser on the machine where the chess backend is
 * running and returns the selected executable path.
 *
 * A browser file input cannot provide an absolute local path to JavaScript.
 * Since this application is normally used locally, the backend can instead
 * open the operating system's file chooser and return the real path to the
 * selected executable.
 *
 * When running inside WSL we prefer the Windows OpenFileDialog. This gives the
 * user the normal Windows file manager while still returning a path that the
 * Linux backend can execute. Outside WSL, AWT's native FileDialog is used.
 */
@Service
public class NativeEngineFilePickerService {

    private final Path initialDirectory;
    private Path lastDirectory;

    /**
     * Creates a new NativeEngineFilePickerService instance.
     * @param engineDiscoveryService the engine discovery service
     */
    public NativeEngineFilePickerService(EngineDiscoveryService engineDiscoveryService) {
        Path discoveryDirectory = engineDiscoveryService.getDiscoveryDirectory();
        this.initialDirectory = Files.isDirectory(discoveryDirectory)
                ? discoveryDirectory
                : Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        this.lastDirectory = initialDirectory;
    }

    /**
     * Performs the select executable operation.
     * @return the result of the operation
     */
    public synchronized String selectExecutable() {
        if (isWsl()) {
            try {
                return selectExecutableWithWindowsDialog();
            } catch (IOException e) {
                if (GraphicsEnvironment.isHeadless()) {
                    throw new IllegalStateException(
                            "Could not open the Windows system file chooser from WSL: " + e.getMessage(), e);
                }
                // WSL interop can be disabled. In that case, fall back to the
                // graphical Java dialog when a DISPLAY/WSLg session exists.
            }
        }

        return selectExecutableWithAwtDialog();
    }

    /**
     * Performs the select executable with windows dialog operation.
     * @return the result of the operation
     */
    private String selectExecutableWithWindowsDialog() throws IOException {
        Path startDirectory = Files.isDirectory(lastDirectory) ? lastDirectory : initialDirectory;
        String windowsStartDirectory = runWslPath("-w", startDirectory.toString());

        String script = String.join("; ",
                "Add-Type -AssemblyName System.Windows.Forms",
                "$dialog = New-Object System.Windows.Forms.OpenFileDialog",
                "$dialog.Title = 'UCI-Engine auswählen'",
                // Windows can browse \\wsl.localhost but its own file validation
                // may reject Linux executables/symlinks as 'not found'. Therefore
                // the dialog only selects a path. Linux validates the real file
                // after the UNC path has been translated back into a WSL path.
                "$dialog.CheckFileExists = $false",
                "$dialog.CheckPathExists = $false",
                "$dialog.ValidateNames = $false",
                "$dialog.DereferenceLinks = $false",
                "$dialog.Multiselect = $false",
                "$dialog.Filter = 'Alle Dateien (*.*)|*.*'",
                "$dialog.InitialDirectory = '" + escapePowerShellSingleQuoted(windowsStartDirectory) + "'",
                "if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Write-Output $dialog.FileName }"
        );

        Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-STA",
                "-Command",
                script)
                .redirectErrorStream(true)
                .start();

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("PowerShell file chooser exited with code " + exitCode
                        + (output.isBlank() ? "" : ": " + output));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Windows file selection was interrupted", e);
        }

        if (output.isBlank()) {
            return null;
        }

        String windowsPath = output.lines()
                .filter(line -> !line.isBlank())
                .reduce((first, second) -> second)
                .orElse("")
                .trim();
        if (windowsPath.isBlank()) {
            return null;
        }

        String linuxPath = convertWindowsPathToLinux(windowsPath);
        return validateAndRemember(Path.of(linuxPath));
    }

    /**
     * Converts the windows path to linux.
     * @param windowsPath the windows path
     * @return the result of the operation
     */
    private String convertWindowsPathToLinux(String windowsPath) throws IOException {
        String distroName = System.getenv("WSL_DISTRO_NAME");
        if (distroName != null && !distroName.isBlank()) {
            String normalized = windowsPath.replace('/', '\\');
            String lower = normalized.toLowerCase(Locale.ROOT);
            String localhostPrefix = ("\\\\wsl.localhost\\" + distroName + "\\").toLowerCase(Locale.ROOT);
            String legacyPrefix = ("\\\\wsl$\\" + distroName + "\\").toLowerCase(Locale.ROOT);

            if (lower.startsWith(localhostPrefix)) {
                return "/" + normalized.substring(localhostPrefix.length()).replace('\\', '/');
            }
            if (lower.startsWith(legacyPrefix)) {
                return "/" + normalized.substring(legacyPrefix.length()).replace('\\', '/');
            }
        }

        return runWslPath("-u", windowsPath);
    }

    /**
     * Performs the run wsl path operation.
     * @param direction the direction
     * @param value the value
     * @return the result of the operation
     */
    private String runWslPath(String direction, String value) throws IOException {
        Process process = new ProcessBuilder("wslpath", direction, value)
                .redirectErrorStream(true)
                .start();
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.isBlank()) {
                throw new IOException("wslpath failed for '" + value + "'"
                        + (output.isBlank() ? "" : ": " + output));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Path conversion was interrupted", e);
        }
        return output;
    }

    /**
     * Performs the select executable with awt dialog operation.
     * @return the result of the operation
     */
    private String selectExecutableWithAwtDialog() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "No graphical desktop is available for the system file chooser. "
                            + "Start the application in a graphical desktop session."
            );
        }

        AtomicReference<String> selectedPath = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Runnable pickerTask = () -> {
            Frame owner = null;
            try {
                owner = new Frame();
                FileDialog dialog = new FileDialog(owner, "UCI-Engine auswählen", FileDialog.LOAD);
                Path startDirectory = Files.isDirectory(lastDirectory) ? lastDirectory : initialDirectory;
                dialog.setDirectory(startDirectory.toString());
                dialog.setFilenameFilter((directory, name) -> {
                    Path candidate = directory.toPath().resolve(name);
                    return Files.isRegularFile(candidate) && Files.isExecutable(candidate);
                });
                dialog.setVisible(true);

                String fileName = dialog.getFile();
                String directoryName = dialog.getDirectory();
                if (fileName == null || directoryName == null) {
                    return;
                }

                selectedPath.set(validateAndRemember(Path.of(directoryName, fileName)));
            } catch (IOException e) {
                failure.set(new IllegalStateException("Could not resolve selected engine path: " + e.getMessage(), e));
            } catch (RuntimeException e) {
                failure.set(e);
            } finally {
                if (owner != null) {
                    owner.dispose();
                }
            }
        };

        try {
            if (EventQueue.isDispatchThread()) {
                pickerTask.run();
            } else {
                EventQueue.invokeAndWait(pickerTask);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Engine file selection was interrupted", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Could not open the system file chooser", cause);
        }

        if (failure.get() != null) {
            throw failure.get();
        }
        return selectedPath.get();
    }

    /**
     * Validates the and remember.
     * @param selected the selected
     * @return the result of the operation
     */
    private String validateAndRemember(Path selected) throws IOException {
        Path normalized = selected.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Selected engine is not a regular file: " + normalized);
        }
        if (!Files.isExecutable(normalized)) {
            throw new IllegalArgumentException("Selected engine is not executable: " + normalized);
        }

        Path realPath = normalized.toRealPath();
        if (realPath.getParent() != null) {
            lastDirectory = realPath.getParent();
        }
        return realPath.toString();
    }

    /**
     * Returns whether the wsl.
     * @return true when the condition is satisfied; otherwise false
     */
    private boolean isWsl() {
        String distroName = System.getenv("WSL_DISTRO_NAME");
        return distroName != null && !distroName.isBlank();
    }

    /**
     * Performs the escape power shell single quoted operation.
     * @param value the value
     * @return the result of the operation
     */
    private String escapePowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }
}
