package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NativeEngineFilePickerServiceTest {

    @Test
    void detectsWindows() {
        assertEquals(
                NativeEngineFilePickerService.DesktopPlatform.WINDOWS,
                NativeEngineFilePickerService.detectPlatform("Windows 11", null));
    }

    @Test
    void detectsWslBeforeLinux() {
        assertEquals(
                NativeEngineFilePickerService.DesktopPlatform.WSL,
                NativeEngineFilePickerService.detectPlatform("Linux", "Ubuntu"));
    }

    @Test
    void detectsMacOs() {
        assertEquals(
                NativeEngineFilePickerService.DesktopPlatform.MACOS,
                NativeEngineFilePickerService.detectPlatform("Mac OS X", null));
    }

    @Test
    void detectsDarwinAsMacOs() {
        assertEquals(
                NativeEngineFilePickerService.DesktopPlatform.MACOS,
                NativeEngineFilePickerService.detectPlatform("Darwin", null));
    }

    @Test
    void treatsOtherSystemsAsLinuxDesktop() {
        assertEquals(
                NativeEngineFilePickerService.DesktopPlatform.LINUX,
                NativeEngineFilePickerService.detectPlatform("Linux", null));
    }
}
