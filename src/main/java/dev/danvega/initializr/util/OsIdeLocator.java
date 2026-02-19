package dev.danvega.initializr.util;

import dev.danvega.initializr.util.IdeLauncher.DetectedIde;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Platform-specific IDE detection and launching.
 */
public sealed interface OsIdeLocator permits MacOsIdeLocator, WindowsIdeLocator {

    List<DetectedIde> detectIdes();

    void launch(DetectedIde ide, Path projectDir) throws IOException;

    static OsIdeLocator current() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.startsWith("windows")) {
            return new WindowsIdeLocator();
        }
        return new MacOsIdeLocator();
    }
}
