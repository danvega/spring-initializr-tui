package dev.danvega.initializr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Records modeling the Spring Initializr API metadata response.
 */
public final class InitializrMetadata {

    private InitializrMetadata() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("type") SelectField type,
            @JsonProperty("packaging") SelectField packaging,
            @JsonProperty("javaVersion") SelectField javaVersion,
            @JsonProperty("language") SelectField language,
            @JsonProperty("bootVersion") SelectField bootVersion,
            @JsonProperty("groupId") TextField groupId,
            @JsonProperty("artifactId") TextField artifactId,
            @JsonProperty("version") TextField version,
            @JsonProperty("name") TextField name,
            @JsonProperty("description") TextField description,
            @JsonProperty("packageName") TextField packageName,
            @JsonProperty("applicationFormat") SelectField applicationFormat,
            @JsonProperty("dependencies") DependencyGroup dependencies
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SelectField(
            @JsonProperty("type") String type,
            @JsonProperty("default") String defaultValue,
            @JsonProperty("values") List<SelectOption> values
    ) {
        public String defaultOrFirst() {
            if (defaultValue != null) return defaultValue;
            return values.isEmpty() ? "" : values.getFirst().id();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SelectOption(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextField(
            @JsonProperty("type") String type,
            @JsonProperty("default") String defaultValue
    ) {
        public String defaultOrEmpty() {
            return defaultValue != null ? defaultValue : "";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DependencyGroup(
            @JsonProperty("type") String type,
            @JsonProperty("values") List<DependencyCategory> values
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DependencyCategory(
            @JsonProperty("name") String name,
            @JsonProperty("values") List<Dependency> values
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dependency(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("versionRange") String versionRange,
            @JsonProperty("_links") Map<String, Object> links
    ) {}
}
