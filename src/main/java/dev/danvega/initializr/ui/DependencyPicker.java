package dev.danvega.initializr.ui;

import dev.danvega.initializr.api.InitializrMetadata;
import dev.danvega.initializr.model.ProjectConfig;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.*;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Dependency search and selection component.
 * Displays dependencies in categorized groups with fuzzy search filtering.
 */
public class DependencyPicker {

    private final List<InitializrMetadata.DependencyCategory> categories;
    private final ProjectConfig config;
    private final Map<String, InitializrMetadata.Dependency> depLookup;
    private final List<List<String>> recentDependencies;
    private String searchQuery = "";
    private int cursorIndex = 0;
    private final List<FlatItem> flatItems = new ArrayList<>();

    // Category filter state
    private int activeCategoryIndex = -1; // -1 = show all

    public record FlatItem(String categoryName, InitializrMetadata.Dependency dependency, boolean isCategory,
                           int[] matchPositions) {
        public FlatItem(String categoryName, InitializrMetadata.Dependency dependency, boolean isCategory) {
            this(categoryName, dependency, isCategory, null);
        }
    }

    public DependencyPicker(List<InitializrMetadata.DependencyCategory> categories, ProjectConfig config,
                            List<List<String>> recentDependencies) {
        this.categories = categories;
        this.config = config;
        this.recentDependencies = recentDependencies != null ? recentDependencies : List.of();

        // Build lookup map for resolving dep IDs
        this.depLookup = new HashMap<>();
        for (var category : categories) {
            for (var dep : category.values()) {
                depLookup.put(dep.id(), dep);
            }
        }

        rebuildFlatList();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase().trim();
        if (!searchQuery.isEmpty()) {
            activeCategoryIndex = -1; // clear category filter when searching
        }
        rebuildFlatList();
        cursorIndex = 0;
        // Skip category header if first item is one
        if (!flatItems.isEmpty() && flatItems.getFirst().isCategory()) {
            cursorIndex = flatItems.size() > 1 ? 1 : 0;
        }
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public boolean isAtTop() { return cursorIndex <= 0; }

    public void moveUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
            while (cursorIndex > 0 && flatItems.get(cursorIndex).isCategory()) {
                cursorIndex--;
            }
        }
    }

    public void moveDown() {
        if (cursorIndex < flatItems.size() - 1) {
            cursorIndex++;
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

    public void cycleCategory() {
        if (categories.isEmpty()) return;
        activeCategoryIndex++;
        if (activeCategoryIndex >= categories.size()) {
            activeCategoryIndex = -1; // back to "All"
        }
        rebuildFlatList();
        cursorIndex = 0;
        if (!flatItems.isEmpty() && flatItems.getFirst().isCategory()) {
            cursorIndex = flatItems.size() > 1 ? 1 : 0;
        }
    }

    public String getActiveCategoryName() {
        if (activeCategoryIndex < 0 || activeCategoryIndex >= categories.size()) {
            return "All";
        }
        return categories.get(activeCategoryIndex).name();
    }

    public boolean hasCategoryFilter() {
        return activeCategoryIndex >= 0;
    }

    private void rebuildFlatList() {
        flatItems.clear();

        if (!searchQuery.isEmpty()) {
            rebuildWithFuzzySearch();
            return;
        }

        // When not searching, prepend recent deps if available and no category filter
        if (activeCategoryIndex < 0 && !recentDependencies.isEmpty()) {
            var recentDeps = resolveRecentDeps();
            if (!recentDeps.isEmpty()) {
                flatItems.add(new FlatItem("\u2605 Recently Used", null, true));
                for (var dep : recentDeps) {
                    flatItems.add(new FlatItem("\u2605 Recently Used", dep, false));
                }
            }
        }

        // Normal category listing (with optional filter)
        for (int i = 0; i < categories.size(); i++) {
            if (activeCategoryIndex >= 0 && i != activeCategoryIndex) continue;
            var category = categories.get(i);
            if (!category.values().isEmpty()) {
                flatItems.add(new FlatItem(category.name(), null, true));
                for (var dep : category.values()) {
                    flatItems.add(new FlatItem(category.name(), dep, false));
                }
            }
        }
    }

    private List<InitializrMetadata.Dependency> resolveRecentDeps() {
        var seen = new LinkedHashSet<String>();
        var result = new ArrayList<InitializrMetadata.Dependency>();
        for (var depSet : recentDependencies) {
            for (var id : depSet) {
                if (seen.add(id) && depLookup.containsKey(id)) {
                    result.add(depLookup.get(id));
                    if (result.size() >= 10) return result;
                }
            }
        }
        return result;
    }

    private void rebuildWithFuzzySearch() {
        record ScoredDep(InitializrMetadata.Dependency dep, String categoryName, int score, int[] matchPositions) {}

        var scored = new ArrayList<ScoredDep>();

        for (var category : categories) {
            if (activeCategoryIndex >= 0 && categories.indexOf(category) != activeCategoryIndex) continue;
            for (var dep : category.values()) {
                String name = dep.name() != null ? dep.name() : "";
                String id = dep.id() != null ? dep.id() : "";
                String desc = dep.description() != null ? dep.description() : "";

                var nameResult = fuzzyScore(searchQuery, name);
                var idResult = fuzzyScore(searchQuery, id);
                var descResult = fuzzyScore(searchQuery, desc);

                int bestScore = Math.max(nameResult.score(), Math.max(idResult.score(), descResult.score()));
                if (bestScore > 0) {
                    // Use name match positions for highlighting, falling back to others
                    int[] positions = nameResult.score() > 0 ? nameResult.positions() : null;
                    scored.add(new ScoredDep(dep, category.name(), bestScore, positions));
                }
            }
        }

        // Sort by score descending; equal scores keep original order (stable sort)
        scored.sort(Comparator.comparingInt(ScoredDep::score).reversed());

        String lastCategory = null;
        for (var item : scored) {
            if (!item.categoryName().equals(lastCategory)) {
                flatItems.add(new FlatItem(item.categoryName(), null, true));
                lastCategory = item.categoryName();
            }
            flatItems.add(new FlatItem(item.categoryName(), item.dep(), false, item.matchPositions()));
        }
    }

    record FuzzyResult(int score, int[] positions) {}

    private FuzzyResult fuzzyScore(String query, String target) {
        if (query.isEmpty() || target.isEmpty()) return new FuzzyResult(0, new int[0]);

        String lowerTarget = target.toLowerCase();

        // Exact substring match gets highest score
        int substringIdx = lowerTarget.indexOf(query);
        if (substringIdx >= 0) {
            int[] positions = new int[query.length()];
            for (int i = 0; i < query.length(); i++) positions[i] = substringIdx + i;
            return new FuzzyResult(1000 + (100 - substringIdx), positions); // bonus for earlier match
        }

        // Fuzzy: all chars must appear in order
        int[] positions = new int[query.length()];
        int targetIdx = 0;
        int score = 0;
        int prevMatchIdx = -2;

        for (int qi = 0; qi < query.length(); qi++) {
            char qc = query.charAt(qi);
            boolean found = false;
            while (targetIdx < lowerTarget.length()) {
                if (lowerTarget.charAt(targetIdx) == qc) {
                    positions[qi] = targetIdx;

                    // Consecutive match bonus
                    if (targetIdx == prevMatchIdx + 1) {
                        score += 15;
                    }

                    // Word boundary bonus (start of string, after space/hyphen/dot)
                    if (targetIdx == 0 || !Character.isLetterOrDigit(lowerTarget.charAt(targetIdx - 1))) {
                        score += 20;
                    }

                    // Camel case boundary bonus
                    if (targetIdx > 0 && Character.isUpperCase(target.charAt(targetIdx))
                            && Character.isLowerCase(target.charAt(targetIdx - 1))) {
                        score += 15;
                    }

                    score += 10; // base score per matched char
                    prevMatchIdx = targetIdx;
                    targetIdx++;
                    found = true;
                    break;
                }
                targetIdx++;
            }
            if (!found) return new FuzzyResult(0, new int[0]); // not all chars matched
        }

        // Density bonus: fewer gaps = better
        int span = positions[positions.length - 1] - positions[0] + 1;
        if (span > 0) {
            score += (int) (50.0 * query.length() / span);
        }

        return new FuzzyResult(score, positions);
    }

    public Element render() {
        var t = ThemeManager.current();
        var elements = new ArrayList<Element>();

        // Selected summary
        var selected = config.getSelectedDependencies();
        if (!selected.isEmpty()) {
            elements.add(
                    text("  Selected: " + String.join(", ", selected) + "  (" + selected.size() + ")")
                            .fg(t.primary()).bold()
            );
        } else {
            elements.add(
                    text("  Search or browse to add dependencies").fg(t.textDim()).italic()
            );
        }

        // Dependency list
        int visibleStart = Math.max(0, cursorIndex - 10);
        int visibleEnd = Math.min(flatItems.size(), visibleStart + 20);

        boolean inSearchMode = !searchQuery.isEmpty();

        for (int i = visibleStart; i < visibleEnd; i++) {
            var item = flatItems.get(i);
            if (item.isCategory()) {
                boolean isRecent = item.categoryName().startsWith("\u2605");
                elements.add(
                        text("  > " + item.categoryName())
                                .fg(isRecent ? t.accent() : t.secondary()).bold()
                );
            } else {
                var dep = item.dependency();
                boolean isSelected = config.isDependencySelected(dep.id());
                boolean isCursor = i == cursorIndex;
                String checkmark = isSelected ? " \u2713 " : "   ";
                String prefix = isCursor ? " \u25b8" : "  ";
                String depName = dep.name();

                if (inSearchMode && item.matchPositions() != null && item.matchPositions().length > 0) {
                    // Render with highlighted match positions
                    elements.add(renderHighlightedDep(prefix, checkmark, depName, item.matchPositions(), isCursor, isSelected));
                } else {
                    String label = prefix + checkmark + depName;
                    var line = text(label);
                    if (isCursor) {
                        line = line.fg(t.text()).bold();
                    } else if (isSelected) {
                        line = line.fg(t.primary());
                    } else {
                        line = line.fg(t.text());
                    }
                    elements.add(line);
                }
            }
        }

        if (flatItems.isEmpty()) {
            elements.add(text("  No dependencies match your search").fg(t.textDim()).italic());
        }

        return column(elements.toArray(Element[]::new));
    }

    private Element renderHighlightedDep(String prefix, String checkmark, String name,
                                          int[] matchPositions, boolean isCursor, boolean isSelected) {
        var t = ThemeManager.current();
        // Build the name with highlighted chars using row of text segments
        var parts = new ArrayList<Element>();
        String beforeName = prefix + checkmark;
        Color baseColor = isCursor ? t.text() : (isSelected ? t.primary() : t.text());

        var prefixEl = text(beforeName).fg(baseColor);
        if (isCursor) prefixEl = prefixEl.bold();
        parts.add(prefixEl);

        var matchSet = new HashSet<Integer>();
        for (int pos : matchPositions) matchSet.add(pos);

        // Group consecutive chars with same highlight state
        var sb = new StringBuilder();
        boolean currentHighlight = false;

        for (int i = 0; i < name.length(); i++) {
            boolean isMatch = matchSet.contains(i);
            if (i == 0) {
                currentHighlight = isMatch;
                sb.append(name.charAt(i));
            } else if (isMatch == currentHighlight) {
                sb.append(name.charAt(i));
            } else {
                // Flush segment
                var segment = text(sb.toString());
                if (currentHighlight) {
                    segment = segment.fg(t.primary()).bold();
                } else {
                    segment = segment.fg(baseColor);
                    if (isCursor) segment = segment.bold();
                }
                parts.add(segment);
                sb.setLength(0);
                sb.append(name.charAt(i));
                currentHighlight = isMatch;
            }
        }
        // Flush last segment
        if (!sb.isEmpty()) {
            var segment = text(sb.toString());
            if (currentHighlight) {
                segment = segment.fg(t.primary()).bold();
            } else {
                segment = segment.fg(baseColor);
                if (isCursor) segment = segment.bold();
            }
            parts.add(segment);
        }

        return row(parts.toArray(Element[]::new));
    }
}
