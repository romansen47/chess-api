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
import java.util.List;
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
 * Windows uses the native Windows Forms file chooser through PowerShell. WSL
 * uses the same chooser and translates the selected Windows path back to a
 * Linux path. macOS uses AppleScript's native choose-file dialog. Linux
 * prefers zenity or kdialog and falls back to AWT only when a graphical Java
 * environment is available.
 */
@Service
public class NativeEngineFilePickerService {

    enum DesktopPlatform {
        WINDOWS,
        WSL,
        MACOS,
        LINUX
    }

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
     * Opens the native engine executable picker for the current platform.
     * @return the selected executable path, or {@code null} when cancelled
     */
    public synchronized String selectExecutable() {
        DesktopPlatform platform = detectPlatform(
                System.getProperty("os.name"),
                System.getenv("WSL_DISTRO_NAME"));

        try {
            return switch (platform) {
                case WINDOWS -> selectExecutableWithWindowsDialog(false);
                case WSL -> selectExecutableWithWindowsDialog(true);
                case MACOS -> selectExecutableWithMacDialog();
                case LINUX -> selectExecutableWithLinuxDialog();
            };
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not open the system file chooser: " + e.getMessage(), e);
        }
    }

    static DesktopPlatform detectPlatform(String osName, String wslDistroName) {
        if (wslDistroName != null && !wslDistroName.isBlank()) {
            return DesktopPlatform.WSL;
        }

        String normalizedOsName = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalizedOsName.contains("win")) {
            return DesktopPlatform.WINDOWS;
        }
        if (normalizedOsName.contains("mac") || normalizedOsName.contains("darwin")) {
            return DesktopPlatform.MACOS;
        }
        return DesktopPlatform.LINUX;
    }

    private String selectExecutableWithWindowsDialog(boolean runningInWsl) throws IOException {
        Path startDirectory = getStartDirectory();
        String pickerStartDirectory = runningInWsl
                ? runWslPath("-w", startDirectory.toString())
                : startDirectory.toString();

        String validateSelectedPath = runningInWsl ? "$false" : "$true";
        String script = String.join("; ",
                "Add-Type -AssemblyName System.Windows.Forms",
                "$dialog = New-Object System.Windows.Forms.OpenFileDialog",
                "$dialog.Title = 'Select UCI engine'",
                "$dialog.CheckFileExists = " + validateSelectedPath,
                "$dialog.CheckPathExists = " + validateSelectedPath,
                "$dialog.ValidateNames = " + validateSelectedPath,
                "$dialog.DereferenceLinks = " + validateSelectedPath,
                "$dialog.Multiselect = $false",
                "$dialog.Filter = 'All files (*.*)|*.*'",
                "$dialog.InitialDirectory = '" + escapePowerShellSingleQuoted(pickerStartDirectory) + "'",
                "if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { "
                        + "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                        + "Write-Output $dialog.FileName }"
        );

        Process process = startPowerShell(script);
        String output = waitForProcess(process, "Windows file chooser");
        String windowsPath = lastNonBlankLine(output);
        if (windowsPath == null) {
            return null;
        }

        if (runningInWsl) {
            return validateAndRemember(Path.of(convertWindowsPathToLinux(windowsPath)));
        }
        return validateAndRemember(Path.of(windowsPath));
    }

    private Process startPowerShell(String script) throws IOException {
        IOException firstFailure = null;
        for (String executable : List.of("powershell.exe", "pwsh.exe")) {
            try {
                return new ProcessBuilder(
                        executable,
                        "-NoProfile",
                        "-NonInteractive",
                        "-STA",
                        "-Command",
                        script)
                        .redirectErrorStream(true)
                        .start();
            } catch (IOException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }

        throw new IOException(
                "Neither powershell.exe nor pwsh.exe could be started",
                firstFailure);
    }

    private String selectExecutableWithMacDialog() throws IOException {
        Path startDirectory = getStartDirectory();
        String script = "set selectedFile to choose file with prompt \"Select UCI engine\" "
                + "default location POSIX file \""
                + escapeAppleScriptString(startDirectory.toString())
                + "\"\nPOSIX path of selectedFile";

        Process process = new ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true)
                .start();

        String output;
        int exitCode;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("macOS file selection was interrupted", e);
        }

        if (exitCode != 0) {
            String normalizedOutput = output.toLowerCase(Locale.ROOT);
            if (normalizedOutput.contains("user canceled") || normalizedOutput.contains("-128")) {
                return null;
            }
            throw new IOException("macOS file chooser exited with code " + exitCode
                    + (output.isBlank() ? "" : ": " + output));
        }

        String selectedPath = lastNonBlankLine(output);
        return selectedPath == null ? null : validateAndRemember(Path.of(selectedPath));
    }

    private String selectExecutableWithLinuxDialog() throws IOException {
        Path startDirectory = getStartDirectory();

        if (isCommandAvailable("zenity")) {
            String selectedPath = runLinuxPicker(
                    List.of(
                            "zenity",
                            "--file-selection",
                            "--title=Select UCI engine",
                            "--filename=" + startDirectory.toString() + "/"),
                    "zenity");
            return selectedPath == null ? null : validateAndRemember(Path.of(selectedPath));
        }

        if (isCommandAvailable("kdialog")) {
            String selectedPath = runLinuxPicker(
                    List.of(
                            "kdialog",
                            "--title",
                            "Select UCI engine",
                            "--getopenfilename",
                            startDirectory.toString()),
                    "kdialog");
            return selectedPath == null ? null : validateAndRemember(Path.of(selectedPath));
        }

        if (!GraphicsEnvironment.isHeadless()) {
            return selectExecutableWithAwtDialog();
        }

        throw new IllegalStateException(
                "No graphical Linux file chooser is available. Install zenity or kdialog, "
                        + "or enter the engine executable path manually.");
    }

    private String runLinuxPicker(List<String> command, String pickerName) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        int exitCode;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(pickerName + " file selection was interrupted", e);
        }

        if (exitCode == 1) {
            return null;
        }
        if (exitCode != 0) {
            throw new IOException(pickerName + " file chooser exited with code " + exitCode
                    + (output.isBlank() ? "" : ": " + output));
        }
        return lastNonBlankLine(output);
    }

    private boolean isCommandAvailable(String command) {
        String pathValue = System.getenv("PATH");
        if (pathValue == null || pathValue.isBlank()) {
            return false;
        }

        String separator = System.getProperty("path.separator", ":");
        for (String directory : pathValue.split(java.util.regex.Pattern.quote(separator))) {
            if (directory.isBlank()) {
                continue;
            }
            try {
                Path candidate = Path.of(directory, command);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed PATH entries and continue searching.
            }
        }
        return false;
    }

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

    private String waitForProcess(Process process, String description) throws IOException {
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(description + " exited with code " + exitCode
                        + (output.isBlank() ? "" : ": " + output));
            }
            return output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(description + " was interrupted", e);
        }
    }

    private String lastNonBlankLine(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        return output.lines()
                .filter(line -> !line.isBlank())
                .reduce((first, second) -> second)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .orElse(null);
    }

    private String selectExecutableWithAwtDialog() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "No graphical desktop is available for the system file chooser."
            );
        }

        AtomicReference<String> selectedPath = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Runnable pickerTask = () -> {
            Frame owner = null;
            try {
                owner = new Frame();
                FileDialog dialog = new FileDialog(owner, "Select UCI engine", FileDialog.LOAD);
                Path startDirectory = getStartDirectory();
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

    private Path getStartDirectory() {
        return Files.isDirectory(lastDirectory) ? lastDirectory : initialDirectory;
    }

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

    private String escapePowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }

    private String escapeAppleScriptString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
