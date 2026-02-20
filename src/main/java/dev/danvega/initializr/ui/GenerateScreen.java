package dev.danvega.initializr.ui;

import dev.danvega.initializr.util.IdeLauncher;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Post-generation screen showing project info, directory tree, and IDE selection.
 */
public class GenerateScreen {

    private static final Color BLUE = Color.rgb(137, 180, 250);
    private static final Color GREEN = Color.rgb(166, 227, 161);
    private static final Color RED = Color.rgb(243, 139, 168);
    private static final Color YELLOW = Color.rgb(249, 226, 175);

    public enum State { GENERATING, SUCCESS, ERROR }

    private State state = State.GENERATING;
    private double progress = 0.0;
    private String statusMessage = "Generating project...";
    private Path projectDir;
    private List<IdeLauncher.DetectedIde> detectedIdes = List.of();
    private int selectedIdeIndex = 0;
    private String errorMessage;
    private String postGenerateCommand = "";

    public void setGenerating(double progress, String message) {
        this.state = State.GENERATING;
        this.progress = progress;
        this.statusMessage = message;
    }

    public void setSuccess(Path projectDir, List<IdeLauncher.DetectedIde> ides) {
        this.state = State.SUCCESS;
        this.projectDir = projectDir;
        this.detectedIdes = ides;
        this.progress = 1.0;
    }

    public void setError(String message) {
        this.state = State.ERROR;
        this.errorMessage = message;
    }

    public State getState() { return state; }

    public void moveIdeUp() {
        if (selectedIdeIndex > 0) selectedIdeIndex--;
    }

    public void moveIdeDown() {
        if (selectedIdeIndex < detectedIdes.size() - 1) selectedIdeIndex++;
    }

    public IdeLauncher.DetectedIde getSelectedIde() {
        if (detectedIdes.isEmpty()) return null;
        return detectedIdes.get(selectedIdeIndex);
    }

    public Path getProjectDir() { return projectDir; }

    public void setPostGenerateCommand(String command) {
        this.postGenerateCommand = command != null ? command : "";
    }

    public Element render() {
        return switch (state) {
            case GENERATING -> renderGenerating();
            case SUCCESS -> renderSuccess();
            case ERROR -> renderError();
        };
    }

    private Element renderGenerating() {
        return panel("Generating Project",
                column(
                        spacer(),
                        text("  " + statusMessage).fg(Color.WHITE),
                        gauge(progress).fg(BLUE),
                        spacer()
                )
        ).rounded().borderColor(BLUE).id("generate-panel");
    }

    private Element renderSuccess() {
        var elements = new ArrayList<Element>();

        elements.add(text("  \u2713 Project Generated!").fg(GREEN).bold());
        elements.add(text(""));
        elements.add(text("  Extracted to: " + projectDir).fg(Color.WHITE));
        elements.add(text(""));

        // IDE selection
        if (!detectedIdes.isEmpty()) {
            elements.add(text("  Open in IDE:").fg(Color.WHITE).bold());
            for (int i = 0; i < detectedIdes.size(); i++) {
                var ide = detectedIdes.get(i);
                String prefix = i == selectedIdeIndex ? "    \u25b8 " : "      ";
                var line = text(prefix + ide.name());
                if (i == selectedIdeIndex) {
                    line = line.fg(BLUE).bold();
                } else {
                    line = line.fg(Color.WHITE);
                }
                elements.add(line);
            }
        } else {
            elements.add(text("  No IDEs detected. Open the project manually:").fg(Color.YELLOW));
            elements.add(text("  " + projectDir).fg(Color.WHITE));
        }

        elements.add(text(""));
        String openLabel = postGenerateCommand.isBlank()
                ? "  [Enter] Open  "
                : "  [Enter] Open + run " + postGenerateCommand + "  ";
        elements.add(
                row(
                        text(openLabel).fg(BLUE),
                        text("  [g] Generate Another  ").fg(Color.WHITE),
                        text("  [q] Quit  ").fg(Color.DARK_GRAY)
                )
        );

        return panel("\u2713 Project Generated!",
                column(elements.toArray(Element[]::new))
        ).rounded().borderColor(GREEN).id("success-panel");
    }

    private Element renderError() {
        return panel("Error",
                column(
                        text("  " + errorMessage).fg(Color.RED),
                        text(""),
                        text("  Press [r] to retry or [q] to quit").fg(Color.DARK_GRAY)
                )
        ).rounded().borderColor(Color.RED).id("error-panel");
    }
}
