package dev.danvega.initializr.api;

import dev.danvega.initializr.model.ProjectConfig;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

/**
 * HTTP client for the Spring Initializr API at start.spring.io.
 */
public class InitializrClient {

    private static final String BASE_URL = "https://start.spring.io";
    private static final String USER_AGENT = loadUserAgent();
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;

    private static String loadUserAgent() {
        try (var in = InitializrClient.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                var props = new java.util.Properties();
                props.load(in);
                var version = props.getProperty("app.version");
                if (version != null && !version.isBlank()) {
                    return "SpringInitializrTUI/" + version;
                }
            }
        } catch (IOException e) {
            // fall through to default
        }
        return "SpringInitializrTUI";
    }

    public InitializrClient() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    /**
     * Fetch metadata (boot versions, dependencies, languages, etc.) from the API.
     */
    public InitializrMetadata.Metadata fetchMetadata() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch metadata: HTTP " + response.statusCode());
        }
        return jsonMapper.readValue(response.body(), InitializrMetadata.Metadata.class);
    }

    /**
     * Preview the build file (pom.xml or build.gradle) without downloading the full project.
     */
    public String previewBuildFile(ProjectConfig config) throws IOException, InterruptedException {
        return previewBuildFile(config, config.getProjectType());
    }

    /**
     * Preview a specific build file format regardless of the project's configured type.
     */
    public String previewBuildFile(ProjectConfig config, String projectType) throws IOException, InterruptedException {
        String endpoint = switch (projectType) {
            case "maven-project" -> "/pom.xml";
            case "gradle-project" -> "/build.gradle";
            case "gradle-project-kotlin" -> "/build.gradle.kts";
            default -> "/pom.xml";
        };
        return fetchBuildFile(endpoint, config);
    }

    private String fetchBuildFile(String endpoint, ProjectConfig config) throws IOException, InterruptedException {
        var uri = URI.create(BASE_URL + endpoint + "?" + buildQueryString(config));
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to preview build file: HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Fetch the project ZIP into memory as a byte array.
     */
    public byte[] fetchProjectZip(ProjectConfig config) throws IOException, InterruptedException {
        var uri = URI.create(BASE_URL + "/starter.zip?" + buildQueryString(config));
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch project ZIP: HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Generate and download the project ZIP file.
     * Returns the path to the downloaded ZIP.
     */
    public Path generateProject(ProjectConfig config, Path outputDir) throws IOException, InterruptedException {
        var uri = URI.create(BASE_URL + "/starter.zip?" + buildQueryString(config));
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        var zipPath = outputDir.resolve(config.getArtifactId() + ".zip");
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(zipPath));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(zipPath);
            throw new IOException("Failed to generate project: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String buildQueryString(ProjectConfig config) {
        var params = new StringJoiner("&");
        params.add("type=" + encode(config.getProjectType()));
        params.add("language=" + encode(config.getLanguage()));
        params.add("bootVersion=" + encode(ProjectConfig.cleanBootVersion(config.getBootVersion())));
        params.add("groupId=" + encode(config.getGroupId()));
        params.add("artifactId=" + encode(config.getArtifactId()));
        params.add("name=" + encode(config.getName()));
        params.add("description=" + encode(config.getDescription()));
        params.add("packageName=" + encode(config.getPackageName()));
        params.add("packaging=" + encode(config.getPackaging()));
        params.add("javaVersion=" + encode(config.getJavaVersion()));
        params.add("applicationFormat=" + encode(config.getApplicationFormat()));

        if (!config.getSelectedDependencies().isEmpty()) {
            params.add("dependencies=" + encode(String.join(",", config.getSelectedDependencies())));
        }
        return params.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
