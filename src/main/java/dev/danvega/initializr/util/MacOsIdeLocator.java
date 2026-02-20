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

        checkPathCommand("nvim", "Neovim", ides);

        return ides;
    }

    @Override
    public void launch(DetectedIde ide, Path projectDir) throws IOException {
        ProcessBuilder pb;
        if ("nvim".equals(ide.command())) {
            pb = buildNvimCommand(ide, projectDir);
        } else {
            pb = new ProcessBuilder(ide.command(), projectDir.toString());
        }
        pb.inheritIO();
        pb.start();
    }

    private ProcessBuilder buildNvimCommand(DetectedIde ide, Path projectDir) {
        // 1. Try xdg-terminal-exec (modern XDG standard, Wayland-friendly)
        if (commandExists("xdg-terminal-exec")) {
            return new ProcessBuilder("setsid", "xdg-terminal-exec",
                    ide.command(), projectDir.toString(), ">/dev/null", "2>&1", "< /dev/null &");
        }

        // 3. Try x-terminal-emulator (Debian/Ubuntu alternatives system)
        if (commandExists("x-terminal-emulator")) {
            return new ProcessBuilder("setsid", "x-terminal-emulator", "-e",
                    ide.command(), projectDir.toString(), ">/dev/null 2>&1", "< /dev/null &");
        }

        throw new IllegalStateException(
                "No terminal emulator found. Please install xdg-terminal-exec " +
                        "or x-terminal-emulator");
    }

    private boolean commandExists(String command) {
        try {
            return new ProcessBuilder("which", command)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
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
