package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Full-screen help overlay showing keyboard shortcuts for all screens.
 */
public class HelpScreen {

    private static final Color SPRING_GREEN = Color.rgb(109, 179, 63);
    private static final Color DIM_GRAY = Color.DARK_GRAY;

    public Element render() {
        return column(
                panel("Help",
                        column(
                                text("  Main Screen").fg(SPRING_GREEN).bold(),
                                text(""),
                                shortcutRow("Tab / Shift+Tab", "Navigate between fields"),
                                shortcutRow("\u2190 \u2192", "Cycle option values"),
                                shortcutRow("\u2191 \u2193", "Move between fields / dependencies"),
                                shortcutRow("/", "Search dependencies"),
                                shortcutRow("Space / Enter", "Toggle selected dependency"),
                                shortcutRow("e", "Explore generated project"),
                                shortcutRow("g", "Generate and download project"),
                                shortcutRow("q / Ctrl+C", "Quit"),
                                text(""),
                                text("  Explore Screen").fg(SPRING_GREEN).bold(),
                                text(""),
                                shortcutRow("\u2190 \u2192", "Switch between files"),
                                shortcutRow("\u2191 \u2193", "Scroll file content"),
                                shortcutRow("PgUp / PgDn", "Scroll by page"),
                                shortcutRow("Enter", "Generate project"),
                                shortcutRow("Esc", "Return to main screen"),
                                text(""),
                                text("  Generate Screen").fg(SPRING_GREEN).bold(),
                                text(""),
                                shortcutRow("\u2191 \u2193", "Select IDE"),
                                shortcutRow("Enter", "Launch selected IDE"),
                                shortcutRow("g", "Back to main screen"),
                                shortcutRow("r", "Retry on error"),
                                text(""),
                                text(""),
                                row(
                                        text("  Press ").fg(DIM_GRAY),
                                        text("Esc").fg(Color.WHITE).bold(),
                                        text(" or ").fg(DIM_GRAY),
                                        text("?").fg(Color.WHITE).bold(),
                                        text(" to close").fg(DIM_GRAY)
                                )
                        )
                ).rounded().borderColor(SPRING_GREEN)
        );
    }

    private Element shortcutRow(String key, String description) {
        return row(
                text(String.format("    %-18s", key)).fg(Color.WHITE).bold(),
                text(description).fg(DIM_GRAY)
        );
    }
}
