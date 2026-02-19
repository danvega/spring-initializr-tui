package dev.danvega.initializr.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigStoreTest {

    @TempDir
    Path tempDir;

    private ConfigStore store;

    @BeforeEach
    void setUp() {
        store = new ConfigStore(tempDir);
    }

    // --- addRecentDependencies ---

    @Test
    void addRecentDependencies_addsToFront() {
        var prefs = new ConfigStore.UserPreferences();
        store.addRecentDependencies(prefs, List.of("web", "jpa"));
        store.addRecentDependencies(prefs, List.of("security"));

        assertThat(prefs.recentDependencies()).hasSize(2);
        assertThat(prefs.recentDependencies().getFirst()).containsExactly("security");
    }

    @Test
    void addRecentDependencies_deduplicates() {
        var prefs = new ConfigStore.UserPreferences();
        store.addRecentDependencies(prefs, List.of("web", "jpa"));
        store.addRecentDependencies(prefs, List.of("security"));
        store.addRecentDependencies(prefs, List.of("web", "jpa"));

        assertThat(prefs.recentDependencies()).hasSize(2);
        assertThat(prefs.recentDependencies().getFirst()).containsExactly("web", "jpa");
    }

    @Test
    void addRecentDependencies_respectsMaxLimit() {
        var prefs = new ConfigStore.UserPreferences();
        for (int i = 0; i < 7; i++) {
            store.addRecentDependencies(prefs, List.of("dep-" + i));
        }

        assertThat(prefs.recentDependencies()).hasSize(ConfigStore.MAX_RECENT);
        assertThat(prefs.recentDependencies().getFirst()).containsExactly("dep-6");
    }

    // --- load / save round-trip ---

    @Test
    void loadSave_roundTrip() {
        var prefs = new ConfigStore.UserPreferences();
        prefs.setLastProjectType("maven-project");
        prefs.setLastLanguage("kotlin");
        prefs.setLastJavaVersion("21");
        prefs.setLastGroupId("org.acme");
        prefs.setLastPackaging("war");

        store.save(prefs);

        var loaded = store.load();
        assertThat(loaded.getLastProjectType()).isEqualTo("maven-project");
        assertThat(loaded.getLastLanguage()).isEqualTo("kotlin");
        assertThat(loaded.getLastJavaVersion()).isEqualTo("21");
        assertThat(loaded.getLastGroupId()).isEqualTo("org.acme");
        assertThat(loaded.getLastPackaging()).isEqualTo("war");
    }

    // --- load returns defaults when file doesn't exist ---

    @Test
    void load_returnsDefaultsWhenFileMissing() {
        var prefs = store.load();
        assertThat(prefs.getLastProjectType()).isEqualTo("gradle-project");
        assertThat(prefs.getLastLanguage()).isEqualTo("java");
        assertThat(prefs.getLastJavaVersion()).isEqualTo("25");
        assertThat(prefs.recentDependencies()).isEmpty();
    }

    // --- load returns defaults on corrupted JSON ---

    @Test
    void load_returnsDefaultsOnCorruptedJson() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("config.json"), "{ not valid json !!!");

        var prefs = store.load();
        assertThat(prefs.getLastProjectType()).isEqualTo("gradle-project");
        assertThat(prefs.recentDependencies()).isEmpty();
    }
}
