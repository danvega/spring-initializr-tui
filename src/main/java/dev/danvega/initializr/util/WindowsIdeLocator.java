package dev.danvega.initializr.util;

import dev.danvega.initializr.util.IdeLauncher.DetectedIde;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows IDE detection via Program Files and LOCALAPPDATA directories.
 */
public final class WindowsIdeLocator implements OsIdeLocator {

    private static final Path PROGRAM_FILES = Path.of("C:\\Program Files");
    private static final Path LOCAL_APP_DATA = Path.of(
            System.getenv("LOCALAPPDATA") != null
                    ? System.getenv("LOCALAPPDATA")
                    : System.getProperty("user.home") + "\\AppData\\Local");

    @Override
    public List<DetectedIde> detectIdes() {
        var ides = new ArrayList<DetectedIde>();

        detectIntelliJ(ides);
        checkPathCommand("idea", "IntelliJ IDEA", ides);

        checkExe(LOCAL_APP_DATA.resolve("Programs\\Microsoft VS Code\\Code.exe"), "Visual Studio Code", "code", ides);
        checkExe(PROGRAM_FILES.resolve("Microsoft VS Code\\Code.exe"), "Visual Studio Code", "code", ides);
        checkPathCommand("code", "Visual Studio Code", ides);

        checkExe(LOCAL_APP_DATA.resolve("Programs\\Cursor\\Cursor.exe"), "Cursor", "cursor", ides);
        checkPathCommand("cursor", "Cursor", ides);

        checkExe(PROGRAM_FILES.resolve("Eclipse\\eclipse.exe"), "Eclipse", "eclipse", ides);

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
            pb = new ProcessBuilder(
                    "cmd", "/c", "start", "\"\"",
                    ide.path().toString(),
                    projectDir.toString());
        }
        pb.inheritIO();
        pb.start();
    }

    private ProcessBuilder buildNvimCommand(DetectedIde ide, Path projectDir) {
        // 1. Windows Terminal (modern, preferred)
        if (commandExists("wt")) {
            return new ProcessBuilder(
                    "wt", "new-tab",
                    "--title", "Neovim",
                    ide.path().toString(), projectDir.toString());
        }

        // 2. PowerShell (available on all modern Windows)
        if (commandExists("pwsh")) {
            return new ProcessBuilder(
                    "pwsh", "-Command",
                    "Start-Process pwsh -ArgumentList '-NoExit', '-Command', '" +
                            ide.path().toString() + " \"" + projectDir.toString() + "\"'");
        }

        // 3. Fallback: plain cmd in a new window
        return new ProcessBuilder(
                "cmd", "/c", "start", "\"Neovim\"", "cmd", "/k",
                ide.path().toString(), projectDir.toString());
    }

    private boolean commandExists(String command) {
        try {
            return new ProcessBuilder("where", command)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private void detectIntelliJ(List<DetectedIde> ides) {
        Path[] searchRoots = {
                PROGRAM_FILES.resolve("JetBrains"),
                LOCAL_APP_DATA.resolve("JetBrains\\Toolbox\\apps")
        };

        for (Path root : searchRoots) {
            if (!Files.isDirectory(root))
                continue;
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root, "IntelliJ IDEA*")) {
                for (Path dir : dirs) {
                    Path exe = dir.resolve("bin\\idea64.exe");
                    if (Files.exists(exe)) {
                        String name = dir.getFileName().toString().contains("Community")
                                ? "IntelliJ IDEA CE"
                                : "IntelliJ IDEA";
                        ides.add(new DetectedIde(name, "idea", exe));
                    }
                }
            } catch (IOException _) {
            }
        }
    }

    private void checkExe(Path exePath, String name, String command, List<DetectedIde> ides) {
        if (Files.exists(exePath)) {
            ides.add(new DetectedIde(name, command, exePath));
        }
    }

    private void checkPathCommand(String command, String name, List<DetectedIde> ides) {
        try {
            var process = new ProcessBuilder("where", command)
                    .redirectErrorStream(true)
                    .start();
            try {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                int exit = process.waitFor();
                if (exit == 0 && !output.isEmpty()) {
                    String cmdPath = output.lines().findFirst().orElse(output);
                    ides.add(new DetectedIde(name, command, Path.of(cmdPath)));
                }
            } finally {
                process.destroy();
            }
        } catch (IOException | InterruptedException _) {
        }
    }
}
