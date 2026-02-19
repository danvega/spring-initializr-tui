package dev.danvega.initializr.ui;

import dev.danvega.initializr.api.InitializrMetadata;
import dev.danvega.initializr.model.ProjectConfig;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Dependency search and selection component.
 * Displays dependencies in categorized groups with fuzzy search filtering.
 */
public class DependencyPicker {

    private static final Color SPRING_GREEN = Color.rgb(109, 179, 63);
    private static final Color DIM_GRAY = Color.DARK_GRAY;

    private final List<InitializrMetadata.DependencyCategory> categories;
    private final ProjectConfig config;
    private String searchQuery = "";
    private int cursorIndex = 0;
    private final List<FlatItem> flatItems = new ArrayList<>();

    public record FlatItem(String categoryName, InitializrMetadata.Dependency dependency, boolean isCategory) {}

    public DependencyPicker(List<InitializrMetadata.DependencyCategory> categories, ProjectConfig config) {
        this.categories = categories;
        this.config = config;
        rebuildFlatList();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase().trim();
        rebuildFlatList();
        cursorIndex = 0;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public boolean isAtTop() { return cursorIndex <= 0; }

    public void moveUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
            // Skip category headers
            while (cursorIndex > 0 && flatItems.get(cursorIndex).isCategory()) {
                cursorIndex--;
            }
        }
    }

    public void moveDown() {
        if (cursorIndex < flatItems.size() - 1) {
            cursorIndex++;
            // Skip category headers
            while (cursorIndex < flatItems.size() - 1 && flatItems.get(cursorIndex).isCategory()) {
                cursorIndex++;
            }
        }
    }

    public void toggleSelected() {
        if (cursorIndex >= 0 && cursorIndex < flatItems.size()) {
            var item = flatItems.get(cursorIndex);
            if (!item.isCategory() && item.dependency() != null) {
                config.toggleDependency(item.dependency().id());
            }
        }
    }

    private void rebuildFlatList() {
        flatItems.clear();
        for (var category : categories) {
            var matchingDeps = category.values().stream()
                    .filter(this::matchesSearch)
                    .toList();

            if (!matchingDeps.isEmpty()) {
                flatItems.add(new FlatItem(category.name(), null, true));
                for (var dep : matchingDeps) {
                    flatItems.add(new FlatItem(category.name(), dep, false));
                }
            }
        }
    }

    private boolean matchesSearch(InitializrMetadata.Dependency dep) {
        if (searchQuery.isEmpty()) return true;
        String name = dep.name() != null ? dep.name().toLowerCase() : "";
        String desc = dep.description() != null ? dep.description().toLowerCase() : "";
        String id = dep.id() != null ? dep.id().toLowerCase() : "";
        return name.contains(searchQuery) || desc.contains(searchQuery) || id.contains(searchQuery);
    }

    public Element render() {
        var elements = new ArrayList<Element>();

        // Selected summary
        var selected = config.getSelectedDependencies();
        if (!selected.isEmpty()) {
            elements.add(
                    text("  Selected: " + String.join(", ", selected) + "  (" + selected.size() + ")")
                            .fg(SPRING_GREEN).bold()
            );
        } else {
            elements.add(
                    text("  Search or browse to add dependencies").fg(DIM_GRAY).italic()
            );
        }

        // Dependency list
        int visibleStart = Math.max(0, cursorIndex - 10);
        int visibleEnd = Math.min(flatItems.size(), visibleStart + 20);

        for (int i = visibleStart; i < visibleEnd; i++) {
            var item = flatItems.get(i);
            if (item.isCategory()) {
                elements.add(
                        text("  > " + item.categoryName()).fg(Color.CYAN).bold()
                );
            } else {
                var dep = item.dependency();
                boolean isSelected = config.isDependencySelected(dep.id());
                boolean isCursor = i == cursorIndex;
                String checkmark = isSelected ? " \u2713 " : "   ";
                String prefix = isCursor ? " \u25b8" : "  ";
                String label = prefix + checkmark + dep.name();

                var line = text(label);
                if (isCursor) {
                    line = line.fg(Color.WHITE).bold();
                } else if (isSelected) {
                    line = line.fg(SPRING_GREEN);
                } else {
                    line = line.fg(Color.WHITE);
                }
                elements.add(line);
            }
        }

        if (flatItems.isEmpty()) {
            elements.add(text("  No dependencies match your search").fg(DIM_GRAY).italic());
        }

        return column(elements.toArray(Element[]::new));
    }
}
