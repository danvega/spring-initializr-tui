package dev.danvega.initializr.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Global theme registry and accessor. Defaults to the Spring theme.
 */
public final class ThemeManager {
    private static String selectedTheme = "spring";
    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    private static Theme current = Theme.SPRING;

    static {
	THEMES.put("spring", Theme.SPRING);
        THEMES.put("catppuccin-mocha", Theme.CATPPUCCIN_MOCHA);
        THEMES.put("dracula", Theme.DRACULA);
        THEMES.put("nord", Theme.NORD);
        THEMES.put("gruvbox-dark", Theme.GRUVBOX_DARK);
        THEMES.put("tokyo-night", Theme.TOKYO_NIGHT);
        THEMES.put("synthwave-84", Theme.SYNTHWAVE_84);
	THEMES.put("gruber-darker",Theme.GRUBER_DARKER_THEME);
    }

    public static Theme current() {
        return current;
    }

    public static void setTheme(String name) {
        selectedTheme = THEMES.containsKey(name) ? name : "spring";
        current = THEMES.get(selectedTheme);
    }


    public static List<String> availableThemes() {
        return List.copyOf(THEMES.keySet());
    }


public static String selectTheme(String query) {
    if (query == null || query.isBlank()) {
        return null;
    }

    String key = query.trim().toLowerCase();
    String match = null;

    if (THEMES.containsKey(key)) {
        match = key;
    } else {
        var matches = THEMES.keySet().stream()
                .filter(name -> name.contains(key))
                .toList();

        if (matches.size() == 1) {
            match = matches.get(0);
        }
    }

    if (match == null) {
        return null;
    }

    selectedTheme = match;
    current = THEMES.get(match);

    return match;
}

public static String updatedTheme() {
    return selectedTheme;
}

public static List<String> fuzzySearch(String query) {

    if (query == null || query.isBlank()) {
        return new ArrayList<>(THEMES.keySet());
    }

    return THEMES.keySet().stream()
        .map(name -> Map.entry(name, fuzzyScore(name, query)))
        .filter(entry -> entry.getValue() >= 0)
        .sorted((a, b) ->
            Integer.compare(b.getValue(), a.getValue())
        )
        .map(Map.Entry::getKey)
        .toList();
}

private static int fuzzyScore(String candidate, String query) {

    String name = candidate.toLowerCase();
    String search = query.toLowerCase();

    int score = 0;
    int searchIndex = 0;
    int previousMatch = -2;

    for (int i = 0; i < name.length() && searchIndex < search.length(); i++) {

        if (name.charAt(i) != search.charAt(searchIndex)) {
            continue;
        }

        score += 10;

        // Consecutive character bonus
        if (i == previousMatch + 1) {
            score += 15;
        }

        // Beginning of a word bonus
        if (i == 0 || name.charAt(i - 1) == '-') {
            score += 10;
        }

        // Beginning of the candidate bonus
        if (i == 0) {
            score += 10;
        }

        previousMatch = i;
        searchIndex++;
    }

    // Every query character must match
    if (searchIndex != search.length()) {
        return -1;
    }

    // Prefer shorter gaps between matched characters
    score -= previousMatch - searchIndex + 1;

    // Exact match bonus
    if (name.equals(search)) {
        score += 100;
    }

    return score;
}
}
