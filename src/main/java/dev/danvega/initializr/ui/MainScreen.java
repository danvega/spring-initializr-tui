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

    public enum FocusArea { PROJECT_TYPE, LANGUAGE, BOOT_VERSION, GROUP, ARTIFACT, NAME, DESCRIPTION, PACKAGING, JAVA_VERSION, APPLICATION_FORMAT, DEPENDENCIES }

    private final InitializrMetadata.Metadata metadata;
    private final ProjectConfig config;
    private final DependencyPicker dependencyPicker;
    private final InitializrMetadata.SelectField appFormatField;
    private FocusArea focusArea = FocusArea.PROJECT_TYPE;
    private boolean searchMode = false;
    private StringBuilder searchBuffer = new StringBuilder();

    public MainScreen(InitializrMetadata.Metadata metadata, ProjectConfig config,
                      List<List<String>> recentDependencies) {
        this.metadata = metadata;
        this.config = config;
        this.dependencyPicker = new DependencyPicker(
                metadata.dependencies() != null ? metadata.dependencies().values() : List.of(),
                config,
                recentDependencies
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

    public void cycleCategory() {
        dependencyPicker.cycleCategory();
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
        var t = ThemeManager.current();
        return panel("",
                row(
                        text("  SPRING INITIALIZR").fg(t.primary()).bold(),
                        spacer(),
                        text("v" + appVersion() + "  ").fg(t.textDim())
                )
        ).rounded().borderColor(t.primary()).length(3).id("header");
    }

    private Element renderConfigForm() {
        var t = ThemeManager.current();
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
        ).rounded().borderColor(focusArea != FocusArea.DEPENDENCIES ? t.primaryBright() : t.textDim()).id("config-form");
    }

    private Element renderSelectRow(String label, InitializrMetadata.SelectField field, String currentValue, boolean focused) {
        var t = ThemeManager.current();
        var parts = new ArrayList<Element>();
        String paddedLabel = String.format("  %-12s", label);
        parts.add(text(paddedLabel).fg(focused ? t.text() : t.textDim()).bold());

        if (field != null) {
            for (var option : field.values()) {
                boolean selected = option.id().equals(currentValue);
                String marker = selected ? "\u25cf " : "\u25cb ";
                var optText = text(marker + displayName(option) + "  ");
                if (selected && focused) {
                    optText = optText.fg(t.primary()).bold();
                } else if (selected) {
                    optText = optText.fg(t.primary());
                } else {
                    optText = optText.fg(t.textDim());
                }
                parts.add(optText);
            }
        }

        if (focused) {
            parts.add(spacer());
            parts.add(text("\u25c0 \u25b6 ").fg(t.textDim()));
        }

        return row(parts.toArray(Element[]::new));
    }

    private Element renderTextRow(String label, String value, boolean focused) {
        var t = ThemeManager.current();
        String paddedLabel = String.format("  %-12s", label);
        String displayValue = focused ? "[ " + value + "_ ]" : "[ " + value + " ]";

        return row(
                text(paddedLabel).fg(focused ? t.text() : t.textDim()).bold(),
                text(displayValue).fg(focused ? t.primary() : t.text())
        );
    }

    private Element renderDependencyPanel() {
        var t = ThemeManager.current();
        var elements = new ArrayList<Element>();

        // Search bar if (searchMode)
        if (searchMode) {
            Element searchBar;
            if (searchBuffer.isEmpty()) {
                searchBar = row (
                    text("  Search: ").fg(t.text()).bold(),
                    text("[ ").fg(t.primary()),
                    text("Esc").fg(t.text()).bold(),
                    text(":exit").fg(t.textDim()),
                    text(" ]").fg(t.primary())
                );
            } else {
                searchBar = row(
                    text("  Search: ").fg(t.text()).bold(),
                    text("[ " + searchBuffer + "_ ]").fg(t.primary())
                );
            }
            elements.add(searchBar);
        } else if (config.getSelectedCount() > 0) {
            elements.add(
                    text("  Press / to search, x to clear all").fg(t.textDim()).italic()
            );
        } else {
            elements.add(
                    text("  Press / to search dependencies").fg(t.textDim()).italic()
            );
        }
        elements.add(text(""));

        // Dependency list
        elements.add(dependencyPicker.render());

        String depTitle = "Dependencies (" + config.getSelectedCount() + " selected)";
        if (dependencyPicker.hasCategoryFilter()) {
            depTitle += " \u2014 " + dependencyPicker.getActiveCategoryName();
        }
        return panel(depTitle,
                column(elements.toArray(Element[]::new))
        ).rounded().borderColor(focusArea == FocusArea.DEPENDENCIES ? t.primaryBright() : t.textDim())
                .fill()
                .id("dep-picker");
    }

    private Element renderActionBar() {
        var t = ThemeManager.current();
        return row(
                text("  "),
                text("[ Generate g ]").fg(t.primary()).bold(),
                text("  "),
                text("[ Explore e ]").fg(t.secondary()),
                text("  "),
                text("[ Quit q ]").fg(t.textDim()),
                spacer()
        ).length(1);
    }

    private Element renderFooter() {
        var t = ThemeManager.current();
        return row(
                text("  Tab").fg(t.text()), text(":navigate  ").fg(t.textDim()),
                text("/").fg(t.text()), text(":search  ").fg(t.textDim()),
                text("Space").fg(t.text()), text(":toggle  ").fg(t.textDim()),
                text("\u2190\u2192").fg(t.text()), text(":change  ").fg(t.textDim()),
                text("c").fg(t.text()), text(":filter  ").fg(t.textDim()),
                text("x").fg(t.text()), text(":clear  ").fg(t.textDim()),
                text("?").fg(t.text()), text(":help  ").fg(t.textDim()),
                text("q").fg(t.text()), text(":quit").fg(t.textDim()),
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
