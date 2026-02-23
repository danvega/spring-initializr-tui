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

    public static final Theme DRACULA = new Theme(
            Color.rgb(189, 147, 249),   // primary (purple)
            Color.rgb(212, 184, 255),   // primaryBright
            Color.rgb(138, 107, 191),   // primaryDim
            Color.rgb(139, 233, 253),   // secondary (cyan)
            Color.rgb(255, 184, 108),   // accent (orange)
            Color.rgb(248, 248, 242),   // text
            Color.rgb(98, 114, 164),    // textDim (comment)
            Color.rgb(80, 250, 123),    // success (green)
            Color.rgb(255, 85, 85),     // error (red)
            Color.rgb(98, 114, 164)     // syntaxComment (comment)
    );

    public static final Theme NORD = new Theme(
            Color.rgb(136, 192, 208),   // primary (frost blue, Nord8)
            Color.rgb(143, 188, 187),   // primaryBright (Nord7)
            Color.rgb(94, 129, 172),    // primaryDim (Nord10)
            Color.rgb(129, 161, 193),   // secondary (Nord9)
            Color.rgb(235, 203, 139),   // accent (yellow, Nord13)
            Color.rgb(236, 239, 244),   // text (snow, Nord6)
            Color.rgb(76, 86, 106),     // textDim (Nord3)
            Color.rgb(163, 190, 140),   // success (green, Nord14)
            Color.rgb(191, 97, 106),    // error (red, Nord11)
            Color.rgb(97, 110, 136)     // syntaxComment (Nord3 bright)
    );

    public static final Theme GRUVBOX_DARK = new Theme(
            Color.rgb(254, 128, 25),    // primary (orange)
            Color.rgb(250, 189, 47),    // primaryBright (yellow)
            Color.rgb(214, 93, 14),     // primaryDim (dark orange)
            Color.rgb(142, 192, 124),   // secondary (aqua)
            Color.rgb(184, 187, 38),    // accent (green)
            Color.rgb(235, 219, 178),   // text (fg)
            Color.rgb(102, 92, 84),     // textDim (bg3)
            Color.rgb(184, 187, 38),    // success (green)
            Color.rgb(251, 73, 52),     // error (red)
            Color.rgb(146, 131, 116)    // syntaxComment (gray)
    );

    public static final Theme TOKYO_NIGHT = new Theme(
            Color.rgb(122, 162, 247),   // primary (blue)
            Color.rgb(169, 184, 232),   // primaryBright
            Color.rgb(61, 89, 161),     // primaryDim (dark blue)
            Color.rgb(125, 207, 255),   // secondary (cyan)
            Color.rgb(224, 175, 104),   // accent (yellow)
            Color.rgb(192, 202, 245),   // text (fg)
            Color.rgb(86, 95, 137),     // textDim (comment)
            Color.rgb(158, 206, 106),   // success (green)
            Color.rgb(247, 118, 142),   // error (red)
            Color.rgb(86, 95, 137)      // syntaxComment (comment)
    );
}
