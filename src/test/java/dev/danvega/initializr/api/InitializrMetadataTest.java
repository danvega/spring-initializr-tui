package dev.danvega.initializr.api;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InitializrMetadataTest {

    // --- SelectField.defaultOrFirst ---

    @Test
    void selectField_defaultOrFirst_returnsDefaultWhenPresent() {
        var field = new InitializrMetadata.SelectField("single-select", "maven-project",
                List.of(new InitializrMetadata.SelectOption("gradle-project", "Gradle"),
                        new InitializrMetadata.SelectOption("maven-project", "Maven")));

        assertThat(field.defaultOrFirst()).isEqualTo("maven-project");
    }

    @Test
    void selectField_defaultOrFirst_fallsBackToFirstWhenNoDefault() {
        var field = new InitializrMetadata.SelectField("single-select", null,
                List.of(new InitializrMetadata.SelectOption("java", "Java"),
                        new InitializrMetadata.SelectOption("kotlin", "Kotlin")));

        assertThat(field.defaultOrFirst()).isEqualTo("java");
    }

    @Test
    void selectField_defaultOrFirst_returnsEmptyWhenNoDefaultAndEmptyValues() {
        var field = new InitializrMetadata.SelectField("single-select", null, List.of());

        assertThat(field.defaultOrFirst()).isEmpty();
    }

    // --- TextField.defaultOrEmpty ---

    @Test
    void textField_defaultOrEmpty_returnsDefaultWhenPresent() {
        var field = new InitializrMetadata.TextField("text", "com.example");
        assertThat(field.defaultOrEmpty()).isEqualTo("com.example");
    }

    @Test
    void textField_defaultOrEmpty_returnsEmptyWhenNull() {
        var field = new InitializrMetadata.TextField("text", null);
        assertThat(field.defaultOrEmpty()).isEmpty();
    }

    // --- JSON round-trip ---

    @Test
    void metadata_jsonRoundTrip() throws Exception {
        var mapper = JsonMapper.builder().build();

        String json = """
                {
                    "type": {
                        "type": "single-select",
                        "default": "maven-project",
                        "values": [
                            {"id": "maven-project", "name": "Maven"},
                            {"id": "gradle-project", "name": "Gradle"}
                        ]
                    },
                    "language": {
                        "type": "single-select",
                        "default": "java",
                        "values": [
                            {"id": "java", "name": "Java"}
                        ]
                    },
                    "groupId": {
                        "type": "text",
                        "default": "com.example"
                    },
                    "artifactId": {
                        "type": "text",
                        "default": "demo"
                    }
                }
                """;

        var metadata = mapper.readValue(json, InitializrMetadata.Metadata.class);

        assertThat(metadata.type()).isNotNull();
        assertThat(metadata.type().defaultOrFirst()).isEqualTo("maven-project");
        assertThat(metadata.type().values()).hasSize(2);
        assertThat(metadata.language().defaultOrFirst()).isEqualTo("java");
        assertThat(metadata.groupId().defaultOrEmpty()).isEqualTo("com.example");
        assertThat(metadata.artifactId().defaultOrEmpty()).isEqualTo("demo");
    }
}
