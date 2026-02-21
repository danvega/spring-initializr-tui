package dev.danvega.initializr.ui;

import dev.tamboui.toolkit.element.Element;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Splash screen showing the Spring banner and loading progress.
 */
public class SplashScreen {

    private static final String[] BANNER_LINES = {
        "  .   ____             _",
        " /\\ / ___| _ __  _ __(_)_ __   __ _",
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
        var t = ThemeManager.current();

        Element[] bannerElements = Arrays.stream(BANNER_LINES)
                .map(line -> text(line).fg(t.primary()).bold().length(1))
                .toArray(Element[]::new);

        return column(
                spacer(),
                column(bannerElements),
                text(buildSubtitle()).fg(t.primaryDim()).length(1),
                spacer(),
                gauge(progress).fg(t.primary()).label(statusMessage),
                spacer()
        ).id("splash");
    }
}
