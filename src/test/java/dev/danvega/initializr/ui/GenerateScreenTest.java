package dev.danvega.initializr.ui;

import dev.danvega.initializr.util.IdeLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateScreenTest {

    private GenerateScreen screen;

    @BeforeEach
    void setUp() {
        screen = new GenerateScreen();
    }

    // --- state transitions ---

    @Test
    void initialState_isGenerating() {
        assertThat(screen.getState()).isEqualTo(GenerateScreen.State.GENERATING);
    }

    @Test
    void setSuccess_transitionsToSuccess() {
        var ides = List.of(
                new IdeLauncher.DetectedIde("IntelliJ IDEA", "idea", Path.of("/usr/local/bin/idea")),
                new IdeLauncher.DetectedIde("VS Code", "code", Path.of("/usr/local/bin/code"))
        );
        screen.setSuccess(Path.of("/tmp/myproject"), ides);

        assertThat(screen.getState()).isEqualTo(GenerateScreen.State.SUCCESS);
        assertThat(screen.getProjectDir()).isEqualTo(Path.of("/tmp/myproject"));
    }

    @Test
    void setError_transitionsToError() {
        screen.setError("Connection failed");
        assertThat(screen.getState()).isEqualTo(GenerateScreen.State.ERROR);
    }

    // --- IDE navigation ---

    @Test
    void moveIdeUp_respectsLowerBound() {
        var ides = List.of(
                new IdeLauncher.DetectedIde("IntelliJ", "idea", null),
                new IdeLauncher.DetectedIde("VS Code", "code", null)
        );
        screen.setSuccess(Path.of("/tmp"), ides);

        screen.moveIdeUp(); // already at 0, should stay
        assertThat(screen.getSelectedIde().name()).isEqualTo("IntelliJ");
    }

    @Test
    void moveIdeDown_respectsUpperBound() {
        var ides = List.of(
                new IdeLauncher.DetectedIde("IntelliJ", "idea", null),
                new IdeLauncher.DetectedIde("VS Code", "code", null)
        );
        screen.setSuccess(Path.of("/tmp"), ides);

        screen.moveIdeDown();
        screen.moveIdeDown(); // past end, should stay at last
        assertThat(screen.getSelectedIde().name()).isEqualTo("VS Code");
    }

    @Test
    void getSelectedIde_returnsCorrectIde() {
        var ides = List.of(
                new IdeLauncher.DetectedIde("IntelliJ", "idea", null),
                new IdeLauncher.DetectedIde("VS Code", "code", null),
                new IdeLauncher.DetectedIde("Cursor", "cursor", null)
        );
        screen.setSuccess(Path.of("/tmp"), ides);

        screen.moveIdeDown();
        assertThat(screen.getSelectedIde().name()).isEqualTo("VS Code");

        screen.moveIdeDown();
        assertThat(screen.getSelectedIde().name()).isEqualTo("Cursor");
    }

    @Test
    void getSelectedIde_returnsNullWhenNoIdes() {
        screen.setSuccess(Path.of("/tmp"), List.of());
        assertThat(screen.getSelectedIde()).isNull();
    }
}
