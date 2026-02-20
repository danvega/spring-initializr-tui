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

    private static final Color BLUE = Color.rgb(137, 180, 250);
    private static final Color CYAN = Color.rgb(137, 220, 235);
    private static final Color YELLOW = Color.rgb(249, 226, 175);
    private static final Color SURFACE2 = Color.rgb(108, 112, 134);

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
        String currentFileName = fileNames.get(currentFileIndex);
        String title = currentFileName + "  (" + (currentFileIndex + 1) + "/" + fileNames.size() + ")";

        Element contentArea = renderHighlightedContent(visibleLines);

        String scrollInfo = getScrollInfo(visibleLines);
        int percent = getScrollPercent(visibleLines);
        String percentStr = percent + "%";

        return column(
                panel(title,
                        contentArea
                ).rounded().borderColor(BLUE),
                row(
                        text("  " + scrollInfo + "  ").fg(Color.DARK_GRAY),
                        lineGauge((double) percent / 100.0)
                                .fg(BLUE)
                                .fill(3),
                        text("  " + percentStr + "  ").fg(Color.DARK_GRAY)
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
        var contentElements = new ArrayList<Element>();
        int end = Math.min(lines.length, scrollOffset + visibleLines);
        String currentFileName = fileNames.get(currentFileIndex);
        FileType fileType = detectFileType(currentFileName);

        for (int i = scrollOffset; i < end; i++) {
            String lineNum = String.format("%4d ", i + 1);
            var parts = new ArrayList<Element>();
            parts.add(text(lineNum).fg(Color.DARK_GRAY));
            switch (fileType) {
                case XML -> addXmlParts(lines[i], parts);
                case GRADLE -> addGradleParts(lines[i], parts);
                case JAVA -> addJavaParts(lines[i], parts);
                case PROPERTIES -> addPropertiesParts(lines[i], parts);
                default -> parts.add(text(lines[i]).fg(Color.WHITE));
            }
            contentElements.add(row(parts.toArray(Element[]::new)));
        }

        return column(contentElements.toArray(Element[]::new));
    }

    private void addXmlParts(String line, ArrayList<Element> parts) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("<!--")) {
            parts.add(text(line).fg(SURFACE2).italic());
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
                parts.add(text(trimmed.substring(lastEnd, tagMatcher.start())).fg(Color.WHITE));
            }
            parts.add(text(tagMatcher.group(1)).fg(BLUE));

            String attrPart = tagMatcher.group(2);
            if (!attrPart.isEmpty()) {
                Matcher attrMatcher = XML_ATTR.matcher(attrPart);
                int attrLastEnd = 0;
                while (attrMatcher.find()) {
                    if (attrMatcher.start() > attrLastEnd) {
                        parts.add(text(attrPart.substring(attrLastEnd, attrMatcher.start())));
                    }
                    parts.add(text(attrMatcher.group(1)).fg(CYAN));
                    parts.add(text("="));
                    parts.add(text(attrMatcher.group(2)).fg(YELLOW));
                    attrLastEnd = attrMatcher.end();
                }
                if (attrLastEnd < attrPart.length()) {
                    parts.add(text(attrPart.substring(attrLastEnd)));
                }
            }

            parts.add(text(tagMatcher.group(3)).fg(BLUE));
            lastEnd = tagMatcher.end();
        }

        if (matched) {
            if (lastEnd < trimmed.length()) {
                parts.add(text(trimmed.substring(lastEnd)).fg(Color.WHITE));
            }
            return;
        }

        parts.add(text(line).fg(Color.WHITE));
    }

    private void addGradleParts(String line, ArrayList<Element> parts) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("//")) {
            parts.add(text(line).fg(SURFACE2).italic());
            return;
        }
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
            parts.add(text(line).fg(SURFACE2).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);

            if (c == '\'' || c == '"') {
                int end = trimmed.indexOf(c, i + 1);
                if (end == -1) end = trimmed.length() - 1;
                parts.add(text(trimmed.substring(i, end + 1)).fg(YELLOW));
                i = end + 1;
                continue;
            }

            if (c == '/' && i + 1 < trimmed.length() && trimmed.charAt(i + 1) == '/') {
                parts.add(text(trimmed.substring(i)).fg(SURFACE2).italic());
                i = trimmed.length();
                continue;
            }

            if (Character.isLetter(c)) {
                int end = i;
                while (end < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end)) || trimmed.charAt(end) == '_')) {
                    end++;
                }
                String word = trimmed.substring(i, end);
                if (GRADLE_KEYWORDS.contains(word)) {
                    parts.add(text(word).fg(BLUE));
                } else {
                    parts.add(text(word).fg(Color.WHITE));
                }
                i = end;
                continue;
            }

            parts.add(text(String.valueOf(c)).fg(Color.WHITE));
            i++;
        }
    }

    private void addJavaParts(String line, ArrayList<Element> parts) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("//")) {
            parts.add(text(line).fg(SURFACE2).italic());
            return;
        }
        if (trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("*/")) {
            parts.add(text(line).fg(SURFACE2).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        // Annotations
        if (trimmed.startsWith("@")) {
            int end = 0;
            while (end < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end)) || trimmed.charAt(end) == '@')) {
                end++;
            }
            parts.add(text(trimmed.substring(0, end)).fg(CYAN));
            if (end < trimmed.length()) {
                parts.add(text(trimmed.substring(end)).fg(Color.WHITE));
            }
            return;
        }

        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);

            if (c == '"') {
                int end = trimmed.indexOf('"', i + 1);
                if (end == -1) end = trimmed.length() - 1;
                parts.add(text(trimmed.substring(i, end + 1)).fg(YELLOW));
                i = end + 1;
                continue;
            }

            if (c == '/' && i + 1 < trimmed.length() && trimmed.charAt(i + 1) == '/') {
                parts.add(text(trimmed.substring(i)).fg(SURFACE2).italic());
                i = trimmed.length();
                continue;
            }

            if (Character.isLetter(c)) {
                int end = i;
                while (end < trimmed.length() && (Character.isLetterOrDigit(trimmed.charAt(end)) || trimmed.charAt(end) == '_')) {
                    end++;
                }
                String word = trimmed.substring(i, end);
                if (JAVA_KEYWORDS.contains(word)) {
                    parts.add(text(word).fg(BLUE));
                } else {
                    parts.add(text(word).fg(Color.WHITE));
                }
                i = end;
                continue;
            }

            parts.add(text(String.valueOf(c)).fg(Color.WHITE));
            i++;
        }
    }

    private void addPropertiesParts(String line, ArrayList<Element> parts) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());

        if (trimmed.startsWith("#")) {
            parts.add(text(line).fg(SURFACE2).italic());
            return;
        }

        if (!indent.isEmpty()) {
            parts.add(text(indent));
        }

        int eq = trimmed.indexOf('=');
        if (eq > 0) {
            parts.add(text(trimmed.substring(0, eq)).fg(CYAN));
            parts.add(text("=").fg(Color.WHITE));
            parts.add(text(trimmed.substring(eq + 1)).fg(YELLOW));
        } else {
            int colon = trimmed.indexOf(':');
            if (colon > 0 && !trimmed.startsWith("---")) {
                parts.add(text(trimmed.substring(0, colon)).fg(CYAN));
                parts.add(text(":").fg(Color.WHITE));
                parts.add(text(trimmed.substring(colon + 1)).fg(YELLOW));
            } else {
                parts.add(text(trimmed).fg(Color.WHITE));
            }
        }
    }
}
