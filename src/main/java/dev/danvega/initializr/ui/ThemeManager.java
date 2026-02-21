package dev.danvega.initializr.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global theme registry and accessor. Defaults to the Spring theme.
 */
public final class ThemeManager {

    private static final Map<String, Theme> THEMES = new LinkedHashMap<>();
    private static Theme current = Theme.SPRING;

    static {
        THEMES.put("spring", Theme.SPRING);
        THEMES.put("catppuccin-mocha", Theme.CATPPUCCIN_MOCHA);
    }

    private ThemeManager() {}

    public static Theme current() {
        return current;
    }

    public static void setTheme(String name) {
        current = THEMES.getOrDefault(name, Theme.SPRING);
    }

    public static List<String> availableThemes() {
        return List.copyOf(THEMES.keySet());
    }
}
