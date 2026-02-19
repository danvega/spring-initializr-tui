package dev.danvega.initializr;

import dev.danvega.initializr.api.InitializrClient;
import dev.danvega.initializr.api.InitializrMetadata;
import dev.danvega.initializr.model.ProjectConfig;
import dev.danvega.initializr.ui.*;
import dev.danvega.initializr.util.ConfigStore;
import dev.danvega.initializr.util.IdeLauncher;
import dev.tamboui.style.Color;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Spring Initializr TUI — main application.
 * Interactive terminal client for start.spring.io built with TamboUI.
 */
public class SpringInitializrTui extends ToolkitApp {

    private static final Color SPRING_GREEN = Color.rgb(109, 179, 63);

    enum Screen { SPLASH, MAIN, EXPLORE, GENERATE, HELP }

    private volatile Screen currentScreen = Screen.SPLASH;
    private Screen previousScreen = Screen.MAIN;
    private final InitializrClient client = new InitializrClient();
    private final ConfigStore configStore = new ConfigStore();
    private final ProjectConfig config = new ProjectConfig();
    private final GenerateScreen generateScreen = new GenerateScreen();
    private final HelpScreen helpScreen = new HelpScreen();

    private volatile InitializrMetadata.Metadata metadata;
    private volatile MainScreen mainScreen;
    private volatile ExploreScreen exploreScreen;

    private volatile double splashProgress = 0.0;
    private volatile String splashMessage = "Connecting to start.spring.io...";

    @Override
    protected void onStart() {
        CompletableFuture.runAsync(() -> {
            try {
                splashProgress = 0.3;
                splashMessage = "Fetching metadata...";

                metadata = client.fetchMetadata();

                splashProgress = 0.7;
                splashMessage = "Loading dependencies...";

                config.applyDefaults(metadata);

                var prefs = configStore.load();
                config.setProjectType(prefs.getLastProjectType());
                config.setLanguage(prefs.getLastLanguage());
                config.setJavaVersion(prefs.getLastJavaVersion());
                config.setGroupId(prefs.getLastGroupId());
                config.setPackaging(prefs.getLastPackaging());
                config.setApplicationFormat(prefs.getLastApplicationFormat());

                mainScreen = new MainScreen(metadata, config);

                splashProgress = 1.0;
                splashMessage = "";

                Thread.sleep(1000);

                currentScreen = Screen.MAIN;
            } catch (Exception e) {
                splashMessage = "Failed to connect: " + e.getMessage();
            }
        });
    }

    @Override
    protected Element render() {
        Element content = switch (currentScreen) {
            case SPLASH -> new SplashScreen(splashProgress, splashMessage).render();
            case MAIN -> mainScreen != null ? mainScreen.render() : text("Loading...").fg(SPRING_GREEN);
            case EXPLORE -> exploreScreen != null ? renderExploreScreen() : text("Loading...").fg(SPRING_GREEN);
            case GENERATE -> generateScreen.render();
            case HELP -> helpScreen.render();
        };

        return column(content)
                .id("root")
                .focusable()
                .onKeyEvent(this::handleKeyEvent);
    }

    private EventResult handleKeyEvent(KeyEvent event) {
        if (event.isQuit()) {
            quit();
            return EventResult.HANDLED;
        }

        return switch (currentScreen) {
            case SPLASH -> EventResult.UNHANDLED;
            case MAIN -> handleMainScreenKey(event);
            case EXPLORE -> handleExploreScreenKey(event);
            case GENERATE -> handleGenerateScreenKey(event);
            case HELP -> handleHelpScreenKey(event);
        };
    }

    private EventResult handleMainScreenKey(KeyEvent event) {
        if (mainScreen == null) return EventResult.UNHANDLED;

        // Search mode handling
        if (mainScreen.isSearchMode()) {
            if (event.code() == KeyCode.ESCAPE) {
                mainScreen.exitSearchMode();
                return EventResult.HANDLED;
            }
            if (event.isDeleteBackward()) {
                mainScreen.deleteSearchChar();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                mainScreen.toggleDependency();
                return EventResult.HANDLED;
            }
            if (event.isUp()) {
                mainScreen.getDependencyPicker().moveUp();
                return EventResult.HANDLED;
            }
            if (event.isDown()) {
                mainScreen.getDependencyPicker().moveDown();
                return EventResult.HANDLED;
            }
            // Any printable character goes into search
            char c = event.character();
            if (c >= 32 && c < 127) {
                mainScreen.appendSearchChar(c);
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        // ? — Show help
        if (event.isChar('?') && !isTextFieldFocused()) {
            previousScreen = currentScreen;
            currentScreen = Screen.HELP;
            return EventResult.HANDLED;
        }

        // Alt+G or 'g' (when not editing text) — Generate
        if ((event.hasAlt() && event.isCharIgnoreCase('g'))
                || (event.isChar('g') && !isTextFieldFocused())) {
            startGeneration();
            return EventResult.HANDLED;
        }

        // Alt+E or 'e' (when not editing text) — Explore
        if ((event.hasAlt() && event.isCharIgnoreCase('e'))
                || (event.isChar('e') && !isTextFieldFocused())) {
            startExplore();
            return EventResult.HANDLED;
        }

        // x — Clear all dependencies (when not in a text field)
        if (event.isChar('x') && !isTextFieldFocused()) {
            mainScreen.clearDependencies();
            return EventResult.HANDLED;
        }

        // / — Enter search mode
        if (event.isChar('/')) {
            mainScreen.enterSearchMode();
            return EventResult.HANDLED;
        }

        // Tab / Shift+Tab — Navigate focus (check both semantic and direct key)
        if (event.isFocusNext() || event.isKey(KeyCode.TAB)) {
            if (event.hasShift()) {
                mainScreen.focusPrevious();
            } else {
                mainScreen.focusNext();
            }
            return EventResult.HANDLED;
        }
        if (event.isFocusPrevious()) {
            mainScreen.focusPrevious();
            return EventResult.HANDLED;
        }

        // Arrow keys — Up/Down also navigate between fields
        if (event.isUp()) {
            if (mainScreen.getFocusArea() == MainScreen.FocusArea.DEPENDENCIES) {
                if (mainScreen.getDependencyPicker().isAtTop()) {
                    mainScreen.focusPrevious();
                } else {
                    mainScreen.getDependencyPicker().moveUp();
                }
            } else {
                mainScreen.focusPrevious();
            }
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            if (mainScreen.getFocusArea() == MainScreen.FocusArea.DEPENDENCIES) {
                mainScreen.getDependencyPicker().moveDown();
            } else {
                mainScreen.focusNext();
            }
            return EventResult.HANDLED;
        }
        if (event.isLeft()) {
            mainScreen.cycleOption(-1);
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            mainScreen.cycleOption(1);
            return EventResult.HANDLED;
        }

        // Space — Toggle dependency
        if (event.isChar(' ')) {
            mainScreen.toggleDependency();
            return EventResult.HANDLED;
        }

        // Enter — Toggle dependency or confirm
        if (event.isConfirm()) {
            if (mainScreen.getFocusArea() == MainScreen.FocusArea.DEPENDENCIES) {
                mainScreen.toggleDependency();
            }
            return EventResult.HANDLED;
        }

        // Backspace — Delete char in text fields
        if (event.isDeleteBackward()) {
            mainScreen.handleBackspace();
            return EventResult.HANDLED;
        }

        // Character input for text fields
        if (isTextFieldFocused()) {
            char c = event.character();
            if (c >= 32 && c < 127) {
                mainScreen.handleChar(c);
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }

    private EventResult handleExploreScreenKey(KeyEvent event) {
        if (event.isChar('?')) {
            previousScreen = currentScreen;
            currentScreen = Screen.HELP;
            return EventResult.HANDLED;
        }
        if (event.isCancel()) {
            currentScreen = Screen.MAIN;
            return EventResult.HANDLED;
        }
        if (event.isLeft()) {
            exploreScreen.previousFile();
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            exploreScreen.nextFile();
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
            exploreScreen.scrollUp();
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            exploreScreen.scrollDown();
            return EventResult.HANDLED;
        }
        if (event.isPageUp()) {
            exploreScreen.pageUp();
            return EventResult.HANDLED;
        }
        if (event.isPageDown()) {
            exploreScreen.pageDown();
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            startGeneration();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleGenerateScreenKey(KeyEvent event) {
        if (event.isChar('?')) {
            previousScreen = currentScreen;
            currentScreen = Screen.HELP;
            return EventResult.HANDLED;
        }
        if (generateScreen.getState() == GenerateScreen.State.SUCCESS) {
            if (event.isUp()) {
                generateScreen.moveIdeUp();
                return EventResult.HANDLED;
            }
            if (event.isDown()) {
                generateScreen.moveIdeDown();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                launchIde();
                return EventResult.HANDLED;
            }
            if (event.isChar('g')) {
                currentScreen = Screen.MAIN;
                return EventResult.HANDLED;
            }
        }
        if (generateScreen.getState() == GenerateScreen.State.ERROR) {
            if (event.isChar('r')) {
                startGeneration();
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleHelpScreenKey(KeyEvent event) {
        if (event.isCancel() || event.isChar('?')) {
            currentScreen = previousScreen;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private boolean isTextFieldFocused() {
        return mainScreen != null && switch (mainScreen.getFocusArea()) {
            case GROUP, ARTIFACT, NAME, DESCRIPTION -> true;
            default -> false;
        };
    }

    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            ".jar", ".class", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".exe", ".bin"
    );

    private static final Set<String> SKIP_FILES = Set.of(
            "mvnw", "mvnw.cmd", "gradlew", "gradlew.bat"
    );

    private void startExplore() {
        CompletableFuture.runAsync(() -> {
            try {
                byte[] zipBytes = client.fetchProjectZip(config);
                var files = extractZipToMap(zipBytes, config.getProjectType());
                exploreScreen = new ExploreScreen(files);
                currentScreen = Screen.EXPLORE;
            } catch (Exception e) {
                splashMessage = "Explore failed: " + e.getMessage();
            }
        });
    }

    private LinkedHashMap<String, String> extractZipToMap(byte[] zipBytes, String projectType) throws IOException {
        // Determine the build file name to put first
        String buildFileName = switch (projectType) {
            case "gradle-project" -> "build.gradle";
            case "gradle-project-kotlin" -> "build.gradle.kts";
            default -> "pom.xml";
        };

        var allFiles = new TreeMap<String, String>();
        String buildFileContent = null;
        String buildFileKey = null;

        try (var zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String name = entry.getName();
                // Strip leading directory (e.g., "demo/pom.xml" -> "pom.xml")
                int slash = name.indexOf('/');
                String relativeName = slash >= 0 ? name.substring(slash + 1) : name;
                if (relativeName.isEmpty()) continue;

                // Skip binary files and wrapper scripts
                String simpleName = relativeName.contains("/")
                        ? relativeName.substring(relativeName.lastIndexOf('/') + 1)
                        : relativeName;
                if (SKIP_FILES.contains(simpleName)) continue;
                boolean skip = false;
                for (String ext : SKIP_EXTENSIONS) {
                    if (simpleName.toLowerCase().endsWith(ext)) { skip = true; break; }
                }
                if (skip) continue;

                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);

                if (simpleName.equals(buildFileName)) {
                    buildFileKey = relativeName;
                    buildFileContent = content;
                } else {
                    allFiles.put(relativeName, content);
                }
            }
        }

        // Convert application.properties to application.yml if YAML format selected
        if ("yaml".equals(config.getApplicationFormat())) {
            var converted = new TreeMap<String, String>();
            for (var entry2 : allFiles.entrySet()) {
                String key = entry2.getKey();
                if (key.endsWith("application.properties")) {
                    String ymlKey = key.replace("application.properties", "application.yml");
                    converted.put(ymlKey, propertiesToYaml(entry2.getValue()));
                } else {
                    converted.put(key, entry2.getValue());
                }
            }
            allFiles = converted;
        }

        // Build file first, then remaining sorted alphabetically
        var result = new LinkedHashMap<String, String>();
        if (buildFileKey != null) {
            result.put(buildFileKey, buildFileContent);
        }
        result.putAll(allFiles);
        return result;
    }

    private void startGeneration() {
        currentScreen = Screen.GENERATE;
        generateScreen.setGenerating(0.1, "Generating project...");

        CompletableFuture.runAsync(() -> {
            try {
                generateScreen.setGenerating(0.3, "Downloading project...");

                Path outputDir = Path.of("").toAbsolutePath();
                Path zipPath = client.generateProject(config, outputDir);

                generateScreen.setGenerating(0.6, "Extracting project...");

                Path projectDir = outputDir.resolve(config.getArtifactId());
                extractZip(zipPath, projectDir);
                Files.deleteIfExists(zipPath);

                generateScreen.setGenerating(0.8, "Detecting IDEs...");

                var ides = IdeLauncher.detectIdes();

                var prefs = configStore.load();
                prefs.setLastProjectType(config.getProjectType());
                prefs.setLastLanguage(config.getLanguage());
                prefs.setLastJavaVersion(config.getJavaVersion());
                prefs.setLastGroupId(config.getGroupId());
                prefs.setLastPackaging(config.getPackaging());
                prefs.setLastApplicationFormat(config.getApplicationFormat());
                configStore.addRecentDependencies(prefs, config.getSelectedDependencies());
                configStore.save(prefs);

                generateScreen.setSuccess(projectDir, ides);
            } catch (Exception e) {
                generateScreen.setError("Generation failed: " + e.getMessage());
            }
        });
    }

    private void launchIde() {
        var ide = generateScreen.getSelectedIde();
        var projectDir = generateScreen.getProjectDir();
        if (ide != null && projectDir != null) {
            try {
                IdeLauncher.launch(ide, projectDir);
                quit();
            } catch (IOException e) {
                generateScreen.setError("Failed to launch IDE: " + e.getMessage());
            }
        }
    }

    private void extractZip(Path zipPath, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        try (var zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(destDir)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        if ("yaml".equals(config.getApplicationFormat())) {
            convertPropertiesToYaml(destDir);
        }
    }

    private void convertPropertiesToYaml(Path projectDir) throws IOException {
        try (var stream = Files.walk(projectDir)) {
            var propsFiles = stream
                    .filter(p -> p.getFileName().toString().equals("application.properties"))
                    .toList();
            for (Path propsFile : propsFiles) {
                String content = Files.readString(propsFile);
                String yaml = propertiesToYaml(content);
                Path yamlFile = propsFile.resolveSibling("application.yml");
                Files.writeString(yamlFile, yaml);
                Files.delete(propsFile);
            }
        }
    }

    static String propertiesToYaml(String properties) {
        var lines = properties.lines().toList();
        var sb = new StringBuilder();
        String[] prevParts = new String[0];

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                sb.append(trimmed).append('\n');
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                sb.append(trimmed).append('\n');
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            String[] parts = key.split("\\.");

            // Find common prefix with previous key
            int common = 0;
            for (int i = 0; i < Math.min(parts.length - 1, prevParts.length - 1); i++) {
                if (parts[i].equals(prevParts[i])) common++;
                else break;
            }

            // Write each new nesting level
            for (int i = common; i < parts.length - 1; i++) {
                sb.append("  ".repeat(i)).append(parts[i]).append(":\n");
            }
            int indent = parts.length - 1;
            sb.append("  ".repeat(indent)).append(parts[parts.length - 1]).append(": ").append(value).append('\n');
            prevParts = parts;
        }
        return sb.toString();
    }

    private Element renderExploreScreen() {
        String summary = String.format(
                " Group: %s  Artifact: %s  Boot: %s  Java: %s  Dependencies: %d",
                config.getGroupId(), config.getArtifactId(),
                ProjectConfig.cleanBootVersion(config.getBootVersion()),
                config.getJavaVersion(), config.getSelectedCount()
        );

        return column(
                panel("",
                        row(
                                text("  SPRING INITIALIZR").fg(SPRING_GREEN).bold(),
                                spacer(),
                                text("Explore  ").fg(Color.CYAN)
                        )
                ).rounded().borderColor(SPRING_GREEN).length(3),
                row(text(summary).fg(Color.DARK_GRAY)).length(1),
                exploreScreen.render(26),
                row(
                        text("  "),
                        text("\u2190\u2192").fg(Color.WHITE), text(":files  ").fg(Color.DARK_GRAY),
                        text("\u2191\u2193").fg(Color.WHITE), text(":scroll  ").fg(Color.DARK_GRAY),
                        text("Enter").fg(Color.WHITE), text(":generate  ").fg(Color.DARK_GRAY),
                        text("Esc").fg(Color.WHITE), text(":back  ").fg(Color.DARK_GRAY),
                        spacer()
                ).length(1)
        );
    }

    public static void main(String[] args) throws Exception {
        new SpringInitializrTui().run();
    }
}
