package demo.chess.api.service;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 */
@Service
public class NativeEngineFilePickerService {

    private final Path initialDirectory;
    private Path lastDirectory;

    public NativeEngineFilePickerService(EngineDiscoveryService engineDiscoveryService) {
        Path discoveryDirectory = engineDiscoveryService.getDiscoveryDirectory();
        this.initialDirectory = Files.isDirectory(discoveryDirectory)
                ? discoveryDirectory
                : Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        this.lastDirectory = initialDirectory;
    }

    /**
     * @return real path of the selected executable, or {@code null} when the
     *         chooser was cancelled
     */
    public synchronized String selectExecutable() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "No graphical desktop is available for the system file chooser. "
                            + "Start the application in a graphical desktop session (for WSL, WSLg/DISPLAY must be available)."
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

                Path selected = Path.of(directoryName, fileName).toAbsolutePath().normalize();
                if (!Files.isRegularFile(selected)) {
                    throw new IllegalArgumentException("Selected engine is not a regular file: " + selected);
                }
                if (!Files.isExecutable(selected)) {
                    throw new IllegalArgumentException("Selected engine is not executable: " + selected);
                }

                Path realPath = selected.toRealPath();
                selectedPath.set(realPath.toString());
                if (realPath.getParent() != null) {
                    lastDirectory = realPath.getParent();
                }
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
}
