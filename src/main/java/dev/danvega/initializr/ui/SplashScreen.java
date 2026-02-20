package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Splash screen showing the Spring banner and loading progress.
 */
public class SplashScreen {

    private static final Color BLUE = Color.rgb(137, 180, 250);
    private static final Color OVERLAY = Color.rgb(147, 153, 178);

    private static final String[] BANNER_LINES = {
        "  .   ____             _",
        " /\\\\ / ___| _ __  _ __(_)_ __   __ _",
        "( ( )\\___ \\| '_ \\| '__| | '_ \\ / _` |",
        " \\\\/  ___) | |_) | |  | | | | | (_| |",
        "  '  |____/| .__/|_|  |_|_| |_|\\__, |",
        " =========|_|=================|___/======"
    };

    private final double progress;
    private final String statusMessage;

    public SplashScreen(double progress, String statusMessage) {
        this.progress = progress;
        this.statusMessage = statusMessage;
    }

    private static String buildSubtitle() {
        String version = SplashScreen.class.getPackage().getImplementationVersion();
        if (version == null) version = "dev";
        String jdk = "JDK " + Runtime.version().feature();
        return " :: Initializr TUI ::          (v" + version + " | " + jdk + ")";
    }

    public Element render() {
        Element[] bannerElements = Arrays.stream(BANNER_LINES)
                .map(line -> text(line).fg(BLUE).bold().length(1))
                .toArray(Element[]::new);

        return column(
                spacer(),
                column(bannerElements),
                text(buildSubtitle()).fg(OVERLAY).length(1),
                spacer(),
                gauge(progress).fg(BLUE).label(statusMessage),
                spacer()
        ).id("splash");
    }
}
