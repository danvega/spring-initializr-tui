package dev.danvega.initializr.ui;

import dev.danvega.initializr.util.ConfigStore;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.widgets.input.TextInputState;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Theme picker with a searchable theme list and a live preview of the
 * currently applied palette.
 */
public class ThemeScreen {

    private final ConfigStore configStore = new ConfigStore();
    private final TextInputState nameState = new TextInputState();

    private String message = "";
    private boolean applied;

    public Element render() {
        var themes = ThemeManager.fuzzySearch(nameState.text());
        var t = ThemeManager.current();

        return column(
                renderHeader(t),
                row(
                        renderBrowser(themes, t).percent(42),
                        renderPreview(t).percent(58)
                ).fill(),
                renderFooter(t)
        ).id("theme-screen");
    }

    private Element renderHeader(Theme t) {
        return panel("",
                row(
                        column(
                                text("Theme Switcher").fg(t.primary()).bold()
                        ),
                        spacer(),
                        text("  " + displayName(ThemeManager.updatedTheme()) + "  ")
                                .fg(t.secondary()).bold()
                )
        ).rounded().borderColor(t.primary()).length(4);
    }

    private Panel renderBrowser(List<String> themes, Theme t) {
        var rows = new ArrayList<Element>();
        String topMatch = themes.isEmpty() ? null : themes.get(0);
        String activeTheme = ThemeManager.updatedTheme();

        if (themes.isEmpty()) {
            rows.add(text(""));
            rows.add(text("  No themes found").fg(t.error()).bold());
            rows.add(text("  Try a shorter or different search").fg(t.textDim()));
        } else {
            for (String theme : themes) {
                boolean active = theme.equals(activeTheme);
                boolean highlighted = theme.equals(topMatch) && !nameState.text().isBlank();

                var marker = text(active ? "  \u2713  " : highlighted ? "  \u25b8  " : "     ")
                        .fg(active ? t.success() : highlighted ? t.accent() : t.textDim());

                var name = text(displayName(theme))
                        .fg(active ? t.primary() : highlighted ? t.accent() : t.text());
                if (active || highlighted) {
                    name = name.bold();
                }

                rows.add(row(marker, name, spacer()));
            }
        }

        String status = !message.isBlank()
                ? message
                : nameState.text().isBlank()
                        ? "Type to filter \u2022 Enter applies"
                        : themes.size() + (themes.size() == 1 ? " matching theme" : " matching themes");

        Color statusColor = !message.isBlank()
                ? (applied ? t.success() : t.error())
                : t.textDim();

        return panel("Browse",
                column(
                        textInput(nameState)
                                .title(" Search ")
                                .placeholder("Type a theme name...")
                                .rounded()
                                .borderColor(t.primaryDim())
                                .focusedBorderColor(t.primaryBright())
                                .onSubmit(this::applyTheme),
                        text(""),
                        text("  " + themesLabel(themes.size())).fg(t.textDim()),
                        text(""),
                        column(rows.toArray(Element[]::new)),
                        spacer(),
                        text(""),
                        text("  " + status).fg(statusColor)
                )
        ).rounded().borderColor(t.primaryBright()).id("theme-browser");
    }

    private Panel renderPreview(Theme t) {
        return panel("Color Palette",
                column(
                        row(
                                column(
                                        text("  " + displayName(ThemeManager.updatedTheme())).fg(t.primary()).bold(),
                                        text("  A taste of your terminal").fg(t.textDim())
                                ),
                                spacer(),
                                text("  ACTIVE  ").fg(t.success()).bold()
                        ),
                        text(""),
                        row(
                                text("  "),
                                swatch(t.primary()),
                                text(" "),
                                swatch(t.primaryBright()),
                                text(" "),
                                swatch(t.secondary()),
                                text(" "),
                                swatch(t.accent()),
                                text(" "),
                                swatch(t.success()),
                                text(" "),
                                swatch(t.error())
                        ),
                        text(""),
                        text("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500")
                                .fg(t.primaryDim()),
                        text(""),
                        row(
                                text("  "),
                                text("[ Enter ]").fg(t.primary()).bold(),
                                text("  Apply theme").fg(t.textDim())
                        )
                )
        ).rounded().borderColor(t.secondary()).id("theme-preview");
    }

    private Element renderFooter(Theme t) {
        return panel("",
                row(
                        text("  Type to filter").fg(t.textDim()),
                        text("  \u2022  ").fg(t.primaryDim()),
                        text("Enter").fg(t.text()).bold(),
                        text(" apply").fg(t.textDim()),
                        text("  \u2022  ").fg(t.primaryDim()),
                        text("Esc").fg(t.text()).bold(),
                        text(" back").fg(t.textDim()),
                        spacer(),
                        text("Themes: " + ThemeManager.availableThemes().size() + "  ").fg(t.textDim())
                )
        ).rounded().borderColor(t.primaryDim()).length(3);
    }

    private Element previewField(String label, String value, Theme t) {
        return row(
                text(String.format("  %-14s", label)).fg(t.textDim()),
                text(value).fg(t.text()).bold()
        );
    }

    private Element swatch(Color color) {
        return text("\u2588\u2588\u2588").fg(color);
    }

    private String themesLabel(int count) {
        return count + (count == 1 ? " theme" : " themes");
    }

    private static String displayName(String theme) {
        if (theme == null || theme.isBlank()) {
            return "Spring";
        }

        var words = theme.split("-");
        var label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return label.toString();
    }

    private void applyTheme() {
        String query = nameState.text().trim();
        if (query.isBlank()) {
            message = "Type a theme name to get started";
            applied = false;
            return;
        }

        var matches = ThemeManager.fuzzySearch(query);
        String candidate = matches.isEmpty() ? query : matches.get(0);
        String result = ThemeManager.selectTheme(candidate);

        if (result == null) {
            message = "No theme matches \"" + query + "\"";
            applied = false;
            return;
        }

        configStore.saveTheme(result);
        message = "Applied " + displayName(result) + " theme";
        applied = true;
        nameState.clear();
    }

    public void reset() {
        nameState.clear();
        message = "";
        applied = false;
    }
}
