package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;

/**
 * Semantic color theme for the TUI. Each role maps to a specific UI purpose.
 */
public record Theme(
        Color primary,         // Borders, headings, keywords, progress
        Color primaryBright,   // Focused form borders
        Color primaryDim,      // Subtitles
        Color secondary,       // Category headers, annotations, XML attrs
        Color accent,          // Recent items, string literals
        Color text,            // Base text
        Color textDim,         // Hints, secondary text
        Color success,         // Success states
        Color error,           // Error states
        Color syntaxComment    // Code comments
) {

    public static final Theme SPRING = new Theme(
            Color.rgb(109, 179, 63),    // primary
            Color.rgb(143, 213, 96),    // primaryBright
            Color.rgb(80, 130, 50),     // primaryDim
            Color.CYAN,                 // secondary
            Color.rgb(255, 200, 60),    // accent
            Color.WHITE,                // text
            Color.DARK_GRAY,            // textDim
            Color.rgb(40, 167, 69),     // success
            Color.RED,                  // error
            Color.rgb(100, 100, 100)    // syntaxComment
    );

    public static final Theme CATPPUCCIN_MOCHA = new Theme(
            Color.rgb(137, 180, 250),   // primary (blue)
            Color.rgb(180, 210, 255),   // primaryBright
            Color.rgb(88, 120, 170),    // primaryDim
            Color.rgb(148, 226, 213),   // secondary (teal)
            Color.rgb(249, 226, 175),   // accent (yellow)
            Color.rgb(205, 214, 244),   // text
            Color.rgb(127, 132, 156),   // textDim (overlay0)
            Color.rgb(166, 227, 161),   // success (green)
            Color.rgb(243, 139, 168),   // error (red)
            Color.rgb(108, 112, 134)    // syntaxComment (overlay0)
    );
}
