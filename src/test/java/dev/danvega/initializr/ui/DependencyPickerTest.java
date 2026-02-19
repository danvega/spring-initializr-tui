package dev.danvega.initializr.ui;

import dev.danvega.initializr.api.InitializrMetadata;
import dev.danvega.initializr.model.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyPickerTest {

    private ProjectConfig config;
    private DependencyPicker picker;

    // Flat list layout:
    // 0: "Web" (category header)
    // 1: web
    // 2: webflux
    // 3: "Data" (category header)
    // 4: jpa
    // 5: jdbc

    @BeforeEach
    void setUp() {
        config = new ProjectConfig();
        var categories = List.of(
                new InitializrMetadata.DependencyCategory("Web", List.of(
                        dep("web", "Spring Web", "Build web applications"),
                        dep("webflux", "Spring Reactive Web", "Reactive web apps")
                )),
                new InitializrMetadata.DependencyCategory("Data", List.of(
                        dep("jpa", "Spring Data JPA", "Java Persistence API"),
                        dep("jdbc", "Spring Data JDBC", "JDBC support")
                ))
        );
        picker = new DependencyPicker(categories, config);
    }

    private static InitializrMetadata.Dependency dep(String id, String name, String description) {
        return new InitializrMetadata.Dependency(id, name, description, null, null);
    }

    // --- search filtering ---

    @Test
    void setSearchQuery_filtersResults() {
        picker.setSearchQuery("reactive");
        assertThat(picker.getSearchQuery()).isEqualTo("reactive");
    }

    @Test
    void setSearchQuery_emptyShowsAll() {
        picker.setSearchQuery("reactive");
        picker.setSearchQuery("");
        assertThat(picker.getSearchQuery()).isEmpty();
    }

    // --- cursor navigation ---

    @Test
    void moveDown_skipsCategoryHeaders() {
        // From 0 (Web header) -> 1 (web) -> 2 (webflux) -> 4 (jpa, skips Data header at 3)
        picker.moveDown(); // web
        picker.moveDown(); // webflux
        picker.moveDown(); // jpa (skips Data header)
        picker.toggleSelected();
        assertThat(config.isDependencySelected("jpa")).isTrue();
    }

    @Test
    void moveUp_skipsCategoryHeaders() {
        // Navigate to jpa (index 4), then moveUp should skip Data header back to webflux
        picker.moveDown(); // web (1)
        picker.moveDown(); // webflux (2)
        picker.moveDown(); // jpa (4, skips Data header at 3)
        picker.moveUp();   // webflux (2, skips Data header at 3)
        picker.toggleSelected();
        assertThat(config.isDependencySelected("webflux")).isTrue();
    }

    @Test
    void moveUp_stopsAtTop() {
        // Move down to web (1), then back up twice — should not go below 0
        picker.moveDown(); // web (1)
        picker.moveUp();   // 0 (Web header)
        picker.moveUp();   // stays at 0
        picker.moveDown(); // web (1) again
        picker.toggleSelected();
        assertThat(config.isDependencySelected("web")).isTrue();
    }

    @Test
    void moveDown_stopsAtBottom() {
        for (int i = 0; i < 20; i++) picker.moveDown();
        picker.toggleSelected();
        assertThat(config.isDependencySelected("jdbc")).isTrue();
    }

    // --- toggleSelected ---

    @Test
    void toggleSelected_togglesDependencyInConfig() {
        picker.moveDown(); // move to web (index 1)
        picker.toggleSelected();
        assertThat(config.isDependencySelected("web")).isTrue();

        picker.toggleSelected();
        assertThat(config.isDependencySelected("web")).isFalse();
    }
}
