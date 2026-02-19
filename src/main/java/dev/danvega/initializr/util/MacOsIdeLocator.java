package dev.danvega.initializr.util;

import dev.danvega.initializr.util.IdeLauncher.DetectedIde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * macOS IDE detection via /Applications bundles and PATH commands.
 */
public final class MacOsIdeLocator implements OsIdeLocator {

    @Override
    public List<DetectedIde> detectIdes() {
        var ides = new ArrayList<DetectedIde>();

        checkMacApp("/Applications/IntelliJ IDEA.app", "IntelliJ IDEA", ides);
        checkMacApp("/Applications/IntelliJ IDEA CE.app", "IntelliJ IDEA CE", ides);
        checkPathCommand("idea", "IntelliJ IDEA", ides);

        checkMacApp("/Applications/Visual Studio Code.app", "Visual Studio Code", ides);
        checkPathCommand("code", "Visual Studio Code", ides);

        checkMacApp("/Applications/Cursor.app", "Cursor", ides);
        checkPathCommand("cursor", "Cursor", ides);

        checkMacApp("/Applications/Eclipse.app", "Eclipse", ides);

        checkMacApp("/Applications/Apache NetBeans.app", "Apache NetBeans", ides);
        checkPathCommand("netbeans", "Apache NetBeans", ides);

        return ides;
    }

    @Override
    public void launch(DetectedIde ide, Path projectDir) throws IOException {
        ProcessBuilder pb;
        if (ide.path() != null && ide.path().toString().endsWith(".app")) {
            pb = new ProcessBuilder("open", "-a", ide.path().toString(), projectDir.toString());
        } else {
            pb = new ProcessBuilder(ide.command(), projectDir.toString());
        }
        pb.inheritIO();
        pb.start();
    }

    private void checkMacApp(String appPath, String name, List<DetectedIde> ides) {
        var path = Path.of(appPath);
        if (Files.exists(path)) {
            ides.add(new DetectedIde(name, null, path));
        }
    }

    private void checkPathCommand(String command, String name, List<DetectedIde> ides) {
        try {
            var process = new ProcessBuilder("which", command)
                    .redirectErrorStream(true)
                    .start();
            try {
                String cmdPath = new String(process.getInputStream().readAllBytes()).trim();
                int exit = process.waitFor();
                if (exit == 0) {
                    ides.add(new DetectedIde(name, command, Path.of(cmdPath)));
                }
            } finally {
                process.destroy();
            }
        } catch (IOException | InterruptedException _) {
        }
    }
}
