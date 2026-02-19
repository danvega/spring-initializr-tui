package dev.danvega.initializr.ui;

import dev.danvega.initializr.api.InitializrMetadata;
import dev.danvega.initializr.model.ProjectConfig;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Main configuration screen with project form fields and dependency picker.
 */
public class MainScreen {

    private static final Color SPRING_GREEN = Color.rgb(109, 179, 63);
    private static final Color BRIGHT_GREEN = Color.rgb(143, 213, 96);
    private static final Color DIM_GRAY = Color.DARK_GRAY;

    public enum FocusArea { PROJECT_TYPE, LANGUAGE, BOOT_VERSION, GROUP, ARTIFACT, NAME, DESCRIPTION, PACKAGING, JAVA_VERSION, APPLICATION_FORMAT, DEPENDENCIES }

    private final InitializrMetadata.Metadata metadata;
    private final ProjectConfig config;
    private final DependencyPicker dependencyPicker;
    private final InitializrMetadata.SelectField appFormatField;
    private FocusArea focusArea = FocusArea.PROJECT_TYPE;
    private boolean searchMode = false;
    private StringBuilder searchBuffer = new StringBuilder();

    public MainScreen(InitializrMetadata.Metadata metadata, ProjectConfig config) {
        this.metadata = metadata;
        this.config = config;
        this.dependencyPicker = new DependencyPicker(
                metadata.dependencies() != null ? metadata.dependencies().values() : List.of(),
                config
        );
        this.appFormatField = metadata.applicationFormat() != null
                ? metadata.applicationFormat()
                : new InitializrMetadata.SelectField("single-select", "properties",
                        List.of(new InitializrMetadata.SelectOption("properties", "Properties"),
                                new InitializrMetadata.SelectOption("yaml", "YAML")));
    }

    public FocusArea getFocusArea() { return focusArea; }
    public DependencyPicker getDependencyPicker() { return dependencyPicker; }
    public boolean isSearchMode() { return searchMode; }

    public void enterSearchMode() {
        searchMode = true;
        searchBuffer.setLength(0);
        focusArea = FocusArea.DEPENDENCIES;
    }

    public void exitSearchMode() {
        searchMode = false;
        searchBuffer.setLength(0);
        dependencyPicker.setSearchQuery("");
    }

    public void appendSearchChar(char c) {
        searchBuffer.append(c);
        dependencyPicker.setSearchQuery(searchBuffer.toString());
    }

    public void deleteSearchChar() {
        if (!searchBuffer.isEmpty()) {
            searchBuffer.deleteCharAt(searchBuffer.length() - 1);
            dependencyPicker.setSearchQuery(searchBuffer.toString());
        }
    }

    public String getSearchQuery() {
        return searchBuffer.toString();
    }

    public void focusNext() {
        var areas = FocusArea.values();
        int next = (focusArea.ordinal() + 1) % areas.length;
        focusArea = areas[next];
        if (focusArea == FocusArea.DEPENDENCIES) {
            searchMode = false;
        }
    }

    public void focusPrevious() {
        var areas = FocusArea.values();
        int prev = (focusArea.ordinal() - 1 + areas.length) % areas.length;
        focusArea = areas[prev];
        if (focusArea == FocusArea.DEPENDENCIES) {
            searchMode = false;
        }
    }

    public void cycleOption(int direction) {
        switch (focusArea) {
            case PROJECT_TYPE -> cycleSelectField(metadata.type(), direction, config::getProjectType, config::setProjectType);
            case LANGUAGE -> cycleSelectField(metadata.language(), direction, config::getLanguage, config::setLanguage);
            case BOOT_VERSION -> cycleSelectField(metadata.bootVersion(), direction, config::getBootVersion, config::setBootVersion);
            case PACKAGING -> cycleSelectField(metadata.packaging(), direction, config::getPackaging, config::setPackaging);
            case JAVA_VERSION -> cycleSelectField(metadata.javaVersion(), direction, config::getJavaVersion, config::setJavaVersion);
            case APPLICATION_FORMAT -> cycleSelectField(appFormatField, direction, config::getApplicationFormat, config::setApplicationFormat);
            case DEPENDENCIES -> {
                if (direction > 0) dependencyPicker.moveDown();
                else dependencyPicker.moveUp();
            }
            default -> {}
        }
    }

    public void handleChar(char c) {
        switch (focusArea) {
            case GROUP -> config.setGroupId(config.getGroupId() + c);
            case ARTIFACT -> config.setArtifactId(config.getArtifactId() + c);
            case NAME -> config.setName(config.getName() + c);
            case DESCRIPTION -> config.setDescription(config.getDescription() + c);
            default -> {}
        }
    }

    public void handleBackspace() {
        switch (focusArea) {
            case GROUP -> { String v = config.getGroupId(); if (!v.isEmpty()) config.setGroupId(v.substring(0, v.length() - 1)); }
            case ARTIFACT -> { String v = config.getArtifactId(); if (!v.isEmpty()) config.setArtifactId(v.substring(0, v.length() - 1)); }
            case NAME -> { String v = config.getName(); if (!v.isEmpty()) config.setName(v.substring(0, v.length() - 1)); }
            case DESCRIPTION -> { String v = config.getDescription(); if (!v.isEmpty()) config.setDescription(v.substring(0, v.length() - 1)); }
            default -> {}
        }
    }

    public void toggleDependency() {
        if (focusArea == FocusArea.DEPENDENCIES) {
            dependencyPicker.toggleSelected();
        }
    }

    public void clearDependencies() {
        config.clearDependencies();
    }

    private void cycleSelectField(InitializrMetadata.SelectField field, int direction,
                                   java.util.function.Supplier<String> getter,
                                   java.util.function.Consumer<String> setter) {
        if (field == null || field.values().isEmpty()) return;
        var values = field.values();
        String current = getter.get();
        int idx = 0;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).id().equals(current)) { idx = i; break; }
        }
        idx = (idx + direction + values.size()) % values.size();
        setter.accept(values.get(idx).id());
    }

    public Element render() {
        return column(
                renderHeader(),
                renderConfigForm(),
                renderDependencyPanel(),
                renderActionBar(),
                renderFooter()
        ).id("main-screen");
    }

    private Element renderHeader() {
        return panel("",
                row(
                        text("  SPRING INITIALIZR").fg(SPRING_GREEN).bold(),
                        spacer(),
                        text("v" + appVersion() + "  ").fg(DIM_GRAY)
                )
        ).rounded().borderColor(SPRING_GREEN).length(3).id("header");
    }

    private Element renderConfigForm() {
        var elements = new ArrayList<Element>();

        // Project type (radio-style)
        elements.add(renderSelectRow("Project", metadata.type(), config.getProjectType(), focusArea == FocusArea.PROJECT_TYPE));
        // Language
        elements.add(renderSelectRow("Language", metadata.language(), config.getLanguage(), focusArea == FocusArea.LANGUAGE));
        // Boot version
        elements.add(renderSelectRow("Boot", metadata.bootVersion(), config.getBootVersion(), focusArea == FocusArea.BOOT_VERSION));

        elements.add(text(""));

        // Text inputs
        elements.add(renderTextRow("Group", config.getGroupId(), focusArea == FocusArea.GROUP));
        elements.add(renderTextRow("Artifact", config.getArtifactId(), focusArea == FocusArea.ARTIFACT));
        elements.add(renderTextRow("Name", config.getName(), focusArea == FocusArea.NAME));
        elements.add(renderTextRow("Description", config.getDescription(), focusArea == FocusArea.DESCRIPTION));
        elements.add(renderTextRow("Package", config.getPackageName(), false)); // auto-generated, not editable

        elements.add(text(""));

        // Packaging and Java
        elements.add(renderSelectRow("Packaging", metadata.packaging(), config.getPackaging(), focusArea == FocusArea.PACKAGING));
        elements.add(renderSelectRow("Java", metadata.javaVersion(), config.getJavaVersion(), focusArea == FocusArea.JAVA_VERSION));
        elements.add(renderSelectRow("Config", appFormatField, config.getApplicationFormat(), focusArea == FocusArea.APPLICATION_FORMAT));

        return panel("Configuration",
                column(elements.toArray(Element[]::new))
        ).rounded().borderColor(focusArea != FocusArea.DEPENDENCIES ? BRIGHT_GREEN : DIM_GRAY).id("config-form");
    }

    private Element renderSelectRow(String label, InitializrMetadata.SelectField field, String currentValue, boolean focused) {
        var parts = new ArrayList<Element>();
        String paddedLabel = String.format("  %-12s", label);
        parts.add(text(paddedLabel).fg(focused ? Color.WHITE : DIM_GRAY).bold());

        if (field != null) {
            for (var option : field.values()) {
                boolean selected = option.id().equals(currentValue);
                String marker = selected ? "\u25cf " : "\u25cb ";
                var optText = text(marker + displayName(option) + "  ");
                if (selected && focused) {
                    optText = optText.fg(SPRING_GREEN).bold();
                } else if (selected) {
                    optText = optText.fg(SPRING_GREEN);
                } else {
                    optText = optText.fg(DIM_GRAY);
                }
                parts.add(optText);
            }
        }

        if (focused) {
            parts.add(spacer());
            parts.add(text("\u25c0 \u25b6 ").fg(DIM_GRAY));
        }

        return row(parts.toArray(Element[]::new));
    }

    private Element renderTextRow(String label, String value, boolean focused) {
        String paddedLabel = String.format("  %-12s", label);
        String displayValue = focused ? "[ " + value + "_ ]" : "[ " + value + " ]";

        return row(
                text(paddedLabel).fg(focused ? Color.WHITE : DIM_GRAY).bold(),
                text(displayValue).fg(focused ? SPRING_GREEN : Color.WHITE)
        );
    }

    private Element renderDependencyPanel() {
        var elements = new ArrayList<Element>();

        // Search bar
        if (searchMode) {
            elements.add(
                    row(
                            text("  Search: ").fg(Color.WHITE).bold(),
                            text("[ " + searchBuffer + "_ ]").fg(SPRING_GREEN)
                    )
            );
        } else if (config.getSelectedCount() > 0) {
            elements.add(
                    text("  Press / to search, x to clear all").fg(DIM_GRAY).italic()
            );
        } else {
            elements.add(
                    text("  Press / to search dependencies").fg(DIM_GRAY).italic()
            );
        }
        elements.add(text(""));

        // Dependency list
        elements.add(dependencyPicker.render());

        return panel("Dependencies (" + config.getSelectedCount() + " selected)",
                column(elements.toArray(Element[]::new))
        ).rounded().borderColor(focusArea == FocusArea.DEPENDENCIES ? BRIGHT_GREEN : DIM_GRAY)
                .fill()
                .id("dep-picker");
    }

    private Element renderActionBar() {
        return row(
                text("  "),
                text("[ Generate g ]").fg(SPRING_GREEN).bold(),
                text("  "),
                text("[ Explore e ]").fg(Color.CYAN),
                text("  "),
                text("[ Quit q ]").fg(DIM_GRAY),
                spacer()
        ).length(1);
    }

    private Element renderFooter() {
        return row(
                text("  Tab").fg(Color.WHITE), text(":navigate  ").fg(DIM_GRAY),
                text("/").fg(Color.WHITE), text(":search  ").fg(DIM_GRAY),
                text("Space").fg(Color.WHITE), text(":toggle  ").fg(DIM_GRAY),
                text("\u2190\u2192").fg(Color.WHITE), text(":change  ").fg(DIM_GRAY),
                text("x").fg(Color.WHITE), text(":clear  ").fg(DIM_GRAY),
                text("?").fg(Color.WHITE), text(":help  ").fg(DIM_GRAY),
                text("q").fg(Color.WHITE), text(":quit").fg(DIM_GRAY),
                spacer()
        ).length(1);
    }

    private static String appVersion() {
        String v = MainScreen.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }

    private String displayName(InitializrMetadata.SelectOption option) {
        String name = option.name() != null ? option.name() : option.id();
        // Clean up legacy Spring version suffixes for display (match live site)
        return name.replace(".RELEASE", "").replace(".BUILD-SNAPSHOT", " (SNAPSHOT)");
    }
}
