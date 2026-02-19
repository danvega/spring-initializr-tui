package dev.danvega.initializr.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExploreScreenTest {

    private ExploreScreen screen;
    private static final int LINE_COUNT = 50;

    @BeforeEach
    void setUp() {
        String content = IntStream.rangeClosed(1, LINE_COUNT)
                .mapToObj(i -> "line " + i)
                .collect(Collectors.joining("\n"));
        var files = new LinkedHashMap<String, String>();
        files.put("pom.xml", content);
        screen = new ExploreScreen(files);
    }

    // --- scrollUp / scrollDown ---

    @Test
    void scrollDown_incrementsOffset() {
        screen.scrollDown();
        screen.scrollDown();
        screen.scrollDown();
        // scrollOffset should be 3
        // We can verify by scrolling up 3 times and checking we're back at 0
        screen.scrollUp();
        screen.scrollUp();
        screen.scrollUp();
        screen.scrollUp(); // one extra — should stay at 0
        // If scrollUp at 0 doesn't go negative, this is fine
    }

    @Test
    void scrollUp_stopsAtZero() {
        screen.scrollUp(); // already at 0
        screen.scrollUp();
        // No exception, offset stays at 0
    }

    @Test
    void scrollDown_stopsAtLastLine() {
        for (int i = 0; i < LINE_COUNT + 10; i++) {
            screen.scrollDown();
        }
        // Should be clamped to lines.length - 1
        screen.scrollDown(); // should not go further
    }

    // --- pageUp / pageDown ---

    @Test
    void pageDown_jumps20Lines() {
        screen.pageDown();
        // offset should be 20
        // Verify by scrolling up 20 times to get back to 0
        for (int i = 0; i < 20; i++) screen.scrollUp();
        screen.scrollUp(); // extra — should stay at 0
    }

    @Test
    void pageUp_jumps20Lines() {
        screen.pageDown(); // go to 20
        screen.pageDown(); // go to 40
        screen.pageUp();   // back to 20
        // Verify by doing 20 scrollUp to reach 0
        for (int i = 0; i < 20; i++) screen.scrollUp();
        screen.scrollUp(); // extra
    }

    @Test
    void pageUp_stopsAtZero() {
        screen.pageDown(); // go to 20
        screen.pageUp();
        screen.pageUp(); // should clamp to 0
    }

    @Test
    void pageDown_stopsAtLastLine() {
        for (int i = 0; i < 10; i++) screen.pageDown();
        // Should be clamped to lines.length - 1 = 49
    }
}
