package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Full-screen help overlay showing keyboard shortcuts for all screens.
 */
public class HelpScreen {

    public Element render() {
        var t = ThemeManager.current();

        return column(
                panel("Help",
                        column(
                                text("  Main Screen").fg(t.primary()).bold(),
                                text(""),
                                shortcutRow("Tab / Shift+Tab", "Navigate between fields"),
                                shortcutRow("\u2190 \u2192", "Cycle option values"),
                                shortcutRow("h l", "Cycle option values (vim)"),
                                shortcutRow("Ctrl+p / Ctrl+n", "Cycle options"),
                                shortcutRow("\u2191 \u2193", "Move between fields / dependencies"),
                                shortcutRow("k j", "Move between fields / dependencies (vim)"),
                                shortcutRow("/", "Search dependencies"),
                                shortcutRow("Space / Enter", "Toggle selected dependency"),
                                shortcutRow("c", "Cycle category filter"),
                                shortcutRow("x", "Clear all dependencies"),
                                shortcutRow("e", "Explore generated project"),
                                shortcutRow("g", "Generate and download project"),
                                shortcutRow("?", "Show this help screen"),
                                shortcutRow("q / Ctrl+C", "Quit"),
                                text(""),
                                text("  Explore Screen").fg(t.primary()).bold(),
                                text(""),
                                shortcutRow("\u2190 \u2192", "Switch between files"),
                                shortcutRow("h l", "Switch between files (vim)"),
                                shortcutRow("\u2191 \u2193", "Scroll file content"),
                                shortcutRow("k j", "Scroll file content (vim)"),
                                shortcutRow("PgUp / PgDn", "Scroll by page"),
                                shortcutRow("Ctrl+u / Ctrl+d", "Scroll by page (vim)"),
                                shortcutRow("Enter", "Generate project"),
                                shortcutRow("Esc", "Return to main screen"),
                                text(""),
                                text("  Generate Screen").fg(t.primary()).bold(),
                                text(""),
                                shortcutRow("\u2191 \u2193", "Select IDE"),
                                shortcutRow("k j", "Select IDE (vim)"),
                                shortcutRow("Enter", "Launch selected IDE"),
                                shortcutRow("g", "Back to main screen"),
                                shortcutRow("r", "Retry on error"),
                                text(""),
                                text(""),
                                row(
                                        text("  Press ").fg(t.textDim()),
                                        text("Esc").fg(t.text()).bold(),
                                        text(" or ").fg(t.textDim()),
                                        text("?").fg(t.text()).bold(),
                                        text(" to close").fg(t.textDim())
                                )
                        )
                ).rounded().borderColor(t.primary())
        );
    }

    private Element shortcutRow(String key, String description) {
        var t = ThemeManager.current();
        return row(
                text(String.format("    %-18s", key)).fg(t.text()).bold(),
                text(description).fg(t.textDim())
        );
    }
}
