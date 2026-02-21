package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Project file explorer with syntax highlighting and scroll position tracking.
 * Supports browsing all files in the generated project ZIP.
 */
public class ExploreScreen {

    public enum BuildFileType {
        MAVEN("pom.xml", "maven-project"),
        GRADLE("build.gradle", "gradle-project"),
        GRADLE_KTS("build.gradle.kts", "gradle-project-kotlin");

        private final String fileName;
        private final String projectType;

        BuildFileType(String fileName, String projectType) {
            this.fileName = fileName;
            this.projectType = projectType;
        }

        public String getFileName() { return fileName; }
        public String getProjectType() { return projectType; }

        public static BuildFileType fromProjectType(String projectType) {
            for (var type : values()) {
                if (type.projectType.equals(projectType)) return type;
            }
            return MAVEN;
        }
    }

    private final List<String> fileNames;
    private final LinkedHashMap<String, String> files;
    private int currentFileIndex = 0;
    private String[] lines;
    private int scrollOffset = 0;

    // XML regex patterns
    private static final Pattern XML_TAG = Pattern.compile("(</?[a-zA-Z][a-zA-Z0-9:.-]*)([^>]*?)(/?>)");
    private static final Pattern XML_ATTR = Pattern.compile("([a-zA-Z][a-zA-Z0-9:.-]*)\\s*=\\s*(\"[^\"]*\"|'[^']*')");

    // Gradle keyword set
    private static final Set<String> GRADLE_KEYWORDS = Set.of(
            "plugins", "dependencies", "repositories", "java", "tasks",
            "implementation", "testImplementation", "runtimeOnly", "compileOnly",
            "api", "annotationProcessor", "developmentOnly",
            "id", "version", "apply", "group", "sourceCompatibility",
            "targetCompatibility", "mavenCentral", "jcenter",
            "buildscript", "allprojects", "subprojects", "ext",
            "sourceSets", "configurations", "springBoot", "bootJar", "bootRun"
    );

    // Java keyword set
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "package", "import", "class", "interface", "enum", "record",
            "public", "private", "protected", "static", "final", "abstract",
            "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "return", "if", "else", "for", "while", "do", "switch", "case", "default",
            "new", "this", "super", "extends", "implements", "throws", "throw",
            "try", "catch", "finally", "var", "null", "true", "false"
    );

    public ExploreScreen(LinkedHashMap<String, String> files) {
        this.files = files;
        this.fileNames = new ArrayList<>(files.keySet());
        loadCurrentFile();
    }

    private void loadCurrentFile() {
        String content = files.get(fileNames.get(currentFileIndex));
        this.lines = content.replace("\t", "  ").split("\n");
        this.scrollOffset = 0;
    }

    public void nextFile() {
        if (currentFileIndex < fileNames.size() - 1) {
            currentFileIndex++;
            loadCurrentFile();
        }
    }

    public void previousFile() {
        if (currentFileIndex > 0) {
            currentFileIndex--;
            loadCurrentFile();
        }
    }

    public void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    public void scrollDown() {
        if (scrollOffset < lines.length - 1) scrollOffset++;
    }

    public void pageUp() {
        scrollOffset = Math.max(0, scrollOffset - 20);
    }

    public void pageDown() {
        scrollOffset = Math.min(Math.max(0, lines.length - 1), scrollOffset + 20);
    }

    public String getScrollInfo(int visibleLines) {
        if (lines.length == 0) return "";
        int start = scrollOffset + 1;
        int end = Math.min(lines.length, scrollOffset + visibleLines);
        return String.format("Lines %d-%d of %d", start, end, lines.length);
    }

    public int getScrollPercent(int visibleLines) {
        if (lines.length <= visibleLines) return 100;
        int maxOffset = lines.length - visibleLines;
        if (maxOffset <= 0) return 100;
        return Math.min(100, (scrollOffset * 100) / maxOffset);
    }

    public Element render(int visibleLines) {
        var t = ThemeManager.current();
        String currentFileName = fileNames.get(currentFileIndex);
        String title = currentFileName + "  (" + (currentFileIndex + 1) + "/" + fileNames.size() + ")";

        Element contentArea = renderHighlightedContent(visibleLines);

        String scrollInfo = getScrollInfo(visibleLines);
        int percent = getScrollPercent(visibleLines);
        String percentStr = percent + "%";

        return column(
                panel(title,
                        contentArea
                ).rounded().borderColor(t.primary()),
                row(
                        text("  " + scrollInfo + "  ").fg(t.textDim()),
                        lineGauge((double) percent / 100.0)
                                .fg(t.primary())
                                .fill(3),
                        text("  " + percentStr + "  ").fg(t.textDim())
                ).length(1)
        );
    }

    private enum FileType { XML, GRADLE, JAVA, PROPERTIES, PLAIN }

    private FileType detectFileType(String fileName) {
        if (fileName.endsWith(".xml")) return FileType.XML;
        if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) return FileType.GRADLE;
        if (fileName.endsWith(".java") || fileName.endsWith(".kt")) return FileType.JAVA;
        if (fileName.endsWith(".properties") || fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return FileType.PROPERTIES;
        return FileType.PLAIN;
    }

    private Element renderHighlightedContent(int visibleLines) {
        var t = ThemeManager.current();
        var contentElements = new ArrayList<Element>();
        int end = Math.min(lines.length, scrollOffset + visibleLines);
        String currentFileName = fileNames.get(currentFileIndex);
        FileType fileType = detectFileType(currentFileName);

        for (int i = scrollOffset; i < end; i++) {
            String lineNum = String.format("%4d ", i + 1);
            var parts = new ArrayList<Element>();
            parts.add(text(lineNum).fg(t.textDim()));
            switch (fileType) {
                case XML -> addXmlParts(lines[i], parts);
                case GRADLE -> addGradleParts(lines[i], parts);
                case JAVA -> addJavaParts(lines[i], parts);
                case PROPERTIES -> addPropertiesParts(lines[i], parts);
                default -> parts.add(text(lines[i]).fg(t.text()));
            }
            contentElements.add(row(parts.toArray(Element[]::new)));
        }

        return column(contentElements.toArray(Element[]::new));
    }

    private void addXmlParts(String line, ArrayList<Element> parts) {
        var t = ThemeManager.current();
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("<!--")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        Matcher tagMatcher = XML_TAG.matcher(trimmed);
        int lastEnd = 0;
        boolean matched = false;

        while (tagMatcher.find()) {
            matched = true;
            if (tagMatcher.start() > lastEnd) {
                parts.add(text(trimmed.substring(lastEnd, tagMatcher.start())).fg(t.text()));
            }
            parts.add(text(tagMatcher.group(1)).fg(t.primary()));

            String attrPart = tagMatcher.group(2);
            if (!attrPart.isEmpty()) {
                Matcher attrMatcher = XML_ATTR.matcher(attrPart);
                int attrLastEnd = 0;
                while (attrMatcher.find()) {
                    if (attrMatcher.start() > attrLastEnd) {
                        parts.add(text(attrPart.substring(attrLastEnd, attrMatcher.start())));
                    }
                    parts.add(text(attrMatcher.group(1)).fg(t.secondary()));
                    parts.add(text("="));
                    parts.add(text(attrMatcher.group(2)).fg(t.accent()));
                    attrLastEnd = attrMatcher.end();
                }
                if (attrLastEnd < attrPart.length()) {
                    parts.add(text(attrPart.substring(attrLastEnd)));
                }
            }

            parts.add(text(tagMatcher.group(3)).fg(t.primary()));
            lastEnd = tagMatcher.end();
        }

        if (matched) {
            if (lastEnd < trimmed.length()) {
                parts.add(text(trimmed.substring(lastEnd)).fg(t.text()));
            }
            return;
        }

        parts.add(text(line).fg(t.text()));
    }

    private void addGradleParts(String line, ArrayList<Element> parts) {
        var t = ThemeManager.current();
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("//")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);

            if (c == '\'' || c == '"') {
                int end2 = trimmed.indexOf(c, i + 1);
                if (end2 == -1) end2 = trimmed.length() - 1;
                parts.add(text(trimmed.substring(i, end2 + 1)).fg(t.accent()));
                i = end2 + 1;
                continue;
            }

            if (c == '/' && i + 1 < trimmed.length() && trimmed.charAt(i + 1) == '/') {
                parts.add(text(trimmed.substring(i)).fg(t.syntaxComment()).italic());
                i = trimmed.length();
                continue;
            }

            if (Character.isLetter(c)) {
                int end2 = i;
                while (end2 < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end2)) || trimmed.charAt(end2) == '_')) {
                    end2++;
                }
                String word = trimmed.substring(i, end2);
                if (GRADLE_KEYWORDS.contains(word)) {
                    parts.add(text(word).fg(t.primary()));
                } else {
                    parts.add(text(word).fg(t.text()));
                }
                i = end2;
                continue;
            }

            parts.add(text(String.valueOf(c)).fg(t.text()));
            i++;
        }
    }

    private void addJavaParts(String line, ArrayList<Element> parts) {
        var t = ThemeManager.current();
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("//")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        // Annotations
        if (trimmed.startsWith("@")) {
            int end2 = 0;
            while (end2 < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end2)) || trimmed.charAt(end2) == '@')) {
                end2++;
            }
            parts.add(text(trimmed.substring(0, end2)).fg(t.secondary()));
            if (end2 < trimmed.length()) {
                parts.add(text(trimmed.substring(end2)).fg(t.text()));
            }
            return;
        }

        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);

            if (c == '"') {
                int end2 = trimmed.indexOf('"', i + 1);
                if (end2 == -1) end2 = trimmed.length() - 1;
                parts.add(text(trimmed.substring(i, end2 + 1)).fg(t.accent()));
                i = end2 + 1;
                continue;
            }

            if (c == '/' && i + 1 < trimmed.length() && trimmed.charAt(i + 1) == '/') {
                parts.add(text(trimmed.substring(i)).fg(t.syntaxComment()).italic());
                i = trimmed.length();
                continue;
            }

            if (Character.isLetter(c)) {
                int end2 = i;
                while (end2 < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end2)) || trimmed.charAt(end2) == '_')) {
                    end2++;
                }
                String word = trimmed.substring(i, end2);
                if (JAVA_KEYWORDS.contains(word)) {
                    parts.add(text(word).fg(t.primary()));
                } else {
                    parts.add(text(word).fg(t.text()));
                }
                i = end2;
                continue;
            }

            parts.add(text(String.valueOf(c)).fg(t.text()));
            i++;
        }
    }

    private void addPropertiesParts(String line, ArrayList<Element> parts) {
        var t = ThemeManager.current();
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("#")) {
            parts.add(text(line).fg(t.syntaxComment()).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        int eq = trimmed.indexOf('=');
        if (eq > 0) {
            parts.add(text(trimmed.substring(0, eq)).fg(t.secondary()));
            parts.add(text("=").fg(t.text()));
            parts.add(text(trimmed.substring(eq + 1)).fg(t.accent()));
        } else {
            int colon = trimmed.indexOf(':');
            if (colon > 0 && !trimmed.startsWith("---")) {
                parts.add(text(trimmed.substring(0, colon)).fg(t.secondary()));
                parts.add(text(":").fg(t.text()));
                parts.add(text(trimmed.substring(colon + 1)).fg(t.accent()));
            } else {
                parts.add(text(trimmed).fg(t.text()));
            }
        }
    }
}
