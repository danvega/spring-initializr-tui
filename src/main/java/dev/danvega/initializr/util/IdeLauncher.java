package dev.danvega.initializr.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects installed IDEs and launches them with a project directory.
 */
public class IdeLauncher {

    private static final OsIdeLocator LOCATOR = OsIdeLocator.current();

    public record DetectedIde(String name, String command, Path path) {}

    /**
     * Scan for installed IDEs on the current system.
     */
    public static List<DetectedIde> detectIdes() {
        return deduplicateIdes(LOCATOR.detectIdes());
    }

    /**
     * Launch an IDE with the given project directory.
     */
    public static void launch(DetectedIde ide, Path projectDir) throws IOException {
        LOCATOR.launch(ide, projectDir);
    }

    private static List<DetectedIde> deduplicateIdes(List<DetectedIde> ides) {
        var seen = new java.util.LinkedHashSet<String>();
        var result = new ArrayList<DetectedIde>();
        for (var ide : ides) {
            if (seen.add(ide.name())) {
                result.add(ide);
            }
        }
        return result;
    }
}
