package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Full-screen help overlay showing keyboard shortcuts for all screens.
 */
public class HelpScreen {

    private static final Color BLUE = Color.rgb(137, 180, 250);
    private static final Color OVERLAY = Color.rgb(147, 153, 178);

    public Element render() {
        return column(
                panel("Help",
                        column(
                                text("  Main Screen").fg(BLUE).bold(),
                                text(""),
                                shortcutRow("Tab / Shift+Tab", "Navigate between fields"),
                                shortcutRow("\u2190 \u2192", "Cycle option values"),
                                shortcutRow("\u2191 \u2193", "Move between fields / dependencies"),
                                shortcutRow("/", "Search dependencies"),
                                shortcutRow("Space / Enter", "Toggle selected dependency"),
                                shortcutRow("c", "Cycle category filter"),
                                shortcutRow("x", "Clear all dependencies"),
                                shortcutRow("e", "Explore generated project"),
                                shortcutRow("g", "Generate and download project"),
                                shortcutRow("?", "Show this help screen"),
                                shortcutRow("q / Ctrl+C", "Quit"),
                                text(""),
                                text("  Explore Screen").fg(BLUE).bold(),
                                text(""),
                                shortcutRow("\u2190 \u2192", "Switch between files"),
                                shortcutRow("\u2191 \u2193", "Scroll file content"),
                                shortcutRow("PgUp / PgDn", "Scroll by page"),
                                shortcutRow("Enter", "Generate project"),
                                shortcutRow("Esc", "Return to main screen"),
                                text(""),
                                text("  Generate Screen").fg(BLUE).bold(),
                                text(""),
                                shortcutRow("\u2191 \u2193", "Select IDE"),
                                shortcutRow("Enter", "Launch selected IDE"),
                                shortcutRow("g", "Back to main screen"),
                                shortcutRow("r", "Retry on error"),
                                text(""),
                                text(""),
                                row(
                                        text("  Press ").fg(OVERLAY),
                                        text("Esc").fg(Color.WHITE).bold(),
                                        text(" or ").fg(OVERLAY),
                                        text("?").fg(Color.WHITE).bold(),
                                        text(" to close").fg(OVERLAY)
                                )
                        )
                ).rounded().borderColor(BLUE)
        );
    }

    private Element shortcutRow(String key, String description) {
        return row(
                text(String.format("    %-18s", key)).fg(Color.WHITE).bold(),
                text(description).fg(OVERLAY)
        );
    }
}
