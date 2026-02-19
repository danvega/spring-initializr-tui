package dev.danvega.initializr.model;

import dev.danvega.initializr.api.InitializrMetadata;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable state holding the current project configuration.
 */
public class ProjectConfig {

    private String projectType = "gradle-project";
    private String language = "java";
    private String bootVersion = "";
    private String groupId = "com.example";
    private String artifactId = "demo";
    private String name = "demo";
    private String description = "Demo project for Spring Boot";
    private String packageName = "com.example.demo";
    private String packaging = "jar";
    private String javaVersion = "25";
    private String applicationFormat = "properties";

    private final Set<String> selectedDependencies = new LinkedHashSet<>();

    /**
     * Initialize defaults from API metadata.
     */
    public void applyDefaults(InitializrMetadata.Metadata metadata) {
        if (metadata.type() != null) this.projectType = metadata.type().defaultOrFirst();
        if (metadata.language() != null) this.language = metadata.language().defaultOrFirst();
        if (metadata.bootVersion() != null) this.bootVersion = metadata.bootVersion().defaultOrFirst();
        if (metadata.groupId() != null) this.groupId = metadata.groupId().defaultOrEmpty();
        if (metadata.artifactId() != null) this.artifactId = metadata.artifactId().defaultOrEmpty();
        if (metadata.name() != null) this.name = metadata.name().defaultOrEmpty();
        if (metadata.description() != null) this.description = metadata.description().defaultOrEmpty();
        if (metadata.packageName() != null) this.packageName = metadata.packageName().defaultOrEmpty();
        if (metadata.packaging() != null) this.packaging = metadata.packaging().defaultOrFirst();
        if (metadata.javaVersion() != null) this.javaVersion = metadata.javaVersion().defaultOrFirst();
        if (metadata.applicationFormat() != null) this.applicationFormat = metadata.applicationFormat().defaultOrFirst();
    }

    /**
     * Strip legacy ".RELEASE" / ".BUILD-SNAPSHOT" suffixes from boot version IDs.
     * Maven Central uses plain versions (e.g., "4.0.2" not "4.0.2.RELEASE").
     */
    public static String cleanBootVersion(String version) {
        if (version == null) return version;
        return version
                .replace(".RELEASE", "")
                .replace(".BUILD-SNAPSHOT", "-SNAPSHOT");
    }

    public void toggleDependency(String depId) {
        if (!selectedDependencies.remove(depId)) {
            selectedDependencies.add(depId);
        }
    }

    public boolean isDependencySelected(String depId) {
        return selectedDependencies.contains(depId);
    }

    public List<String> getSelectedDependencies() {
        return new ArrayList<>(selectedDependencies);
    }

    public int getSelectedCount() {
        return selectedDependencies.size();
    }

    public void updatePackageName() {
        this.packageName = groupId + "." + artifactId;
    }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getBootVersion() { return bootVersion; }
    public void setBootVersion(String bootVersion) { this.bootVersion = bootVersion; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
        updatePackageName();
    }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        this.name = artifactId;
        updatePackageName();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    public String getApplicationFormat() { return applicationFormat; }
    public void setApplicationFormat(String applicationFormat) { this.applicationFormat = applicationFormat; }
}
