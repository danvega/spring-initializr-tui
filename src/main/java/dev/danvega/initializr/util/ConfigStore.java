package dev.danvega.initializr.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists user preferences and recently used dependency combinations
 * to ~/.spring-initializr-tui/config.json.
 */
public class ConfigStore {

    private static final Path DEFAULT_CONFIG_DIR = Path.of(System.getProperty("user.home"), ".spring-initializr-tui");
    static final int MAX_RECENT = 5;

    private final Path configDir;
    private final Path configFile;
    private final JsonMapper jsonMapper;

    public ConfigStore() {
        this(DEFAULT_CONFIG_DIR);
    }

    public ConfigStore(Path configDir) {
        this.configDir = configDir;
        this.configFile = configDir.resolve("config.json");
        this.jsonMapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public UserPreferences load() {
        if (!Files.exists(configFile)) {
            return new UserPreferences();
        }
        try {
            return jsonMapper.readValue(configFile, UserPreferences.class);
        } catch (Exception e) {
            return new UserPreferences();
        }
    }

    public void save(UserPreferences prefs) {
        try {
            Files.createDirectories(configDir);
            jsonMapper.writeValue(configFile, prefs);
        } catch (Exception e) {
            // Silently fail — preferences are not critical
        }
    }

    public void addRecentDependencies(UserPreferences prefs, List<String> deps) {
        if (deps.isEmpty()) return;
        prefs.recentDependencies().removeIf(r -> r.equals(deps));
        prefs.recentDependencies().addFirst(new ArrayList<>(deps));
        while (prefs.recentDependencies().size() > MAX_RECENT) {
            prefs.recentDependencies().removeLast();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserPreferences {
        private String lastProjectType = "gradle-project";
        private String lastLanguage = "java";
        private String lastJavaVersion = "25";
        private String lastGroupId = "com.example";
        private String lastPackaging = "jar";
        private String lastApplicationFormat = "properties";
        private List<List<String>> recentDependencies = new ArrayList<>();

        public String getLastProjectType() { return lastProjectType; }
        public void setLastProjectType(String v) { this.lastProjectType = v; }

        public String getLastLanguage() { return lastLanguage; }
        public void setLastLanguage(String v) { this.lastLanguage = v; }

        public String getLastJavaVersion() { return lastJavaVersion; }
        public void setLastJavaVersion(String v) { this.lastJavaVersion = v; }

        public String getLastGroupId() { return lastGroupId; }
        public void setLastGroupId(String v) { this.lastGroupId = v; }

        public String getLastPackaging() { return lastPackaging; }
        public void setLastPackaging(String v) { this.lastPackaging = v; }

        public String getLastApplicationFormat() { return lastApplicationFormat; }
        public void setLastApplicationFormat(String v) { this.lastApplicationFormat = v; }

        public List<List<String>> recentDependencies() { return recentDependencies; }
        public void setRecentDependencies(List<List<String>> v) { this.recentDependencies = v; }
    }
}
