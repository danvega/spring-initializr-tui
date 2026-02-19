package dev.danvega.initializr.ui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Splash screen showing the Spring banner and loading progress.
 */
public class SplashScreen {

    private static final Color SPRING_GREEN = Color.rgb(109, 179, 63);
    private static final Color DIM_GREEN = Color.rgb(80, 130, 50);

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
                .map(line -> text(line).fg(SPRING_GREEN).bold().length(1))
                .toArray(Element[]::new);

        return column(
                spacer(),
                column(bannerElements),
                text(buildSubtitle()).fg(DIM_GREEN).length(1),
                spacer(),
                gauge(progress).fg(SPRING_GREEN).label(statusMessage),
                spacer()
        ).id("splash");
    }
}
