package dev.danvega.initializr.model;

import dev.danvega.initializr.api.InitializrMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectConfigTest {

    private ProjectConfig config;

    @BeforeEach
    void setUp() {
        config = new ProjectConfig();
    }

    // --- cleanBootVersion ---

    @Test
    void cleanBootVersion_nullReturnsNull() {
        assertThat(ProjectConfig.cleanBootVersion(null)).isNull();
    }

    @Test
    void cleanBootVersion_plainVersionUnchanged() {
        assertThat(ProjectConfig.cleanBootVersion("3.4.2")).isEqualTo("3.4.2");
    }

    @Test
    void cleanBootVersion_stripsReleaseSuffix() {
        assertThat(ProjectConfig.cleanBootVersion("2.7.18.RELEASE")).isEqualTo("2.7.18");
    }

    @Test
    void cleanBootVersion_convertsBuildSnapshot() {
        assertThat(ProjectConfig.cleanBootVersion("3.5.0.BUILD-SNAPSHOT"))
                .isEqualTo("3.5.0-SNAPSHOT");
    }

    // --- toggleDependency / isDependencySelected / getSelectedCount / getSelectedDependencies ---

    @Test
    void toggleDependency_addsDependency() {
        config.toggleDependency("web");
        assertThat(config.isDependencySelected("web")).isTrue();
        assertThat(config.getSelectedCount()).isEqualTo(1);
    }

    @Test
    void toggleDependency_removesDependency() {
        config.toggleDependency("web");
        config.toggleDependency("web");
        assertThat(config.isDependencySelected("web")).isFalse();
        assertThat(config.getSelectedCount()).isZero();
    }

    @Test
    void toggleDependency_reAddsDependency() {
        config.toggleDependency("web");
        config.toggleDependency("web");
        config.toggleDependency("web");
        assertThat(config.isDependencySelected("web")).isTrue();
    }

    @Test
    void getSelectedDependencies_returnsListCopy() {
        config.toggleDependency("web");
        config.toggleDependency("jpa");
        List<String> deps = config.getSelectedDependencies();
        assertThat(deps).containsExactly("web", "jpa");
        deps.clear(); // modifying the copy shouldn't affect the original
        assertThat(config.getSelectedCount()).isEqualTo(2);
    }

    // --- updatePackageName ---

    @Test
    void updatePackageName_combinesGroupAndArtifact() {
        config.setGroupId("org.example");
        config.setArtifactId("myapp");
        assertThat(config.getPackageName()).isEqualTo("org.example.myapp");
    }

    // --- setArtifactId side effects ---

    @Test
    void setArtifactId_alsoSetsNameAndUpdatesPackage() {
        config.setGroupId("com.test");
        config.setArtifactId("coolapp");
        assertThat(config.getName()).isEqualTo("coolapp");
        assertThat(config.getPackageName()).isEqualTo("com.test.coolapp");
    }

    // --- setGroupId side effects ---

    @Test
    void setGroupId_updatesPackageName() {
        config.setArtifactId("demo");
        config.setGroupId("io.acme");
        assertThat(config.getPackageName()).isEqualTo("io.acme.demo");
    }

    // --- applyDefaults ---

    @Test
    void applyDefaults_appliesMetadataValues() {
        var type = new InitializrMetadata.SelectField("single-select", "maven-project",
                List.of(new InitializrMetadata.SelectOption("gradle-project", "Gradle"),
                        new InitializrMetadata.SelectOption("maven-project", "Maven")));
        var language = new InitializrMetadata.SelectField("single-select", "kotlin",
                List.of(new InitializrMetadata.SelectOption("java", "Java"),
                        new InitializrMetadata.SelectOption("kotlin", "Kotlin")));
        var bootVersion = new InitializrMetadata.SelectField("single-select", "3.4.0",
                List.of(new InitializrMetadata.SelectOption("3.4.0", "3.4.0")));
        var packaging = new InitializrMetadata.SelectField("single-select", "war",
                List.of(new InitializrMetadata.SelectOption("jar", "Jar"),
                        new InitializrMetadata.SelectOption("war", "War")));
        var javaVersion = new InitializrMetadata.SelectField("single-select", "21",
                List.of(new InitializrMetadata.SelectOption("25", "25"),
                        new InitializrMetadata.SelectOption("21", "21")));
        var groupId = new InitializrMetadata.TextField("text", "org.acme");
        var artifactId = new InitializrMetadata.TextField("text", "myservice");
        var name = new InitializrMetadata.TextField("text", "myservice");
        var description = new InitializrMetadata.TextField("text", "My Service");
        var packageName = new InitializrMetadata.TextField("text", "org.acme.myservice");

        var metadata = new InitializrMetadata.Metadata(
                type, packaging, javaVersion, language, bootVersion,
                groupId, artifactId, null, name, description, packageName, null, null
        );

        config.applyDefaults(metadata);

        assertThat(config.getProjectType()).isEqualTo("maven-project");
        assertThat(config.getLanguage()).isEqualTo("kotlin");
        assertThat(config.getBootVersion()).isEqualTo("3.4.0");
        assertThat(config.getGroupId()).isEqualTo("org.acme");
        assertThat(config.getArtifactId()).isEqualTo("myservice");
        assertThat(config.getName()).isEqualTo("myservice");
        assertThat(config.getDescription()).isEqualTo("My Service");
        assertThat(config.getPackageName()).isEqualTo("org.acme.myservice");
        assertThat(config.getPackaging()).isEqualTo("war");
        assertThat(config.getJavaVersion()).isEqualTo("21");
    }
}
