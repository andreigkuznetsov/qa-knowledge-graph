package ru.kuznetsov.qaip.explorer.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationOverviewHttpProductionQualificationTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    MockMvc mvc;

    @Test
    void qualifies_unified_overview_and_specialized_endpoint_parity_on_real_repository() throws Exception {
        String configured = System.getenv("ORDER_EVENTS_KAFKA_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "ORDER_EVENTS_KAFKA_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/main/java")),
                "Kafka qualification production sources are unavailable");

        String repositoryId = analyze(repository);
        JsonNode operations = getJson("/api/v1/repositories/{repositoryId}/operations", repositoryId);
        JsonNode postOperation = operation(operations, "POST", "/api/orders");
        JsonNode getOperation = operation(operations, "GET", "/");

        qualifyCreateOrder(repositoryId, postOperation);
        qualifySynchronousRoot(repositoryId, getOperation);
    }

    private void qualifyCreateOrder(String repositoryId, JsonNode operation) throws Exception {
        String operationId = operation.path("operationId").asText();
        JsonNode overview = overview(repositoryId, operationId);

        assertIdentity(overview, repositoryId, operation);
        assertThat(overview.at("/implementation/state").asText()).isEqualTo("INCOMPLETE");
        assertThat(overview.at("/eventPath/state").asText()).isEqualTo("AVAILABLE");
        assertThat(StreamSupport.stream(overview.at("/eventPath/path/steps").spliterator(), false)
                .map(step -> step.path("implementationRole").asText()))
                .containsExactly(
                        "REST_CONTROLLER", "MESSAGE_PRODUCER", "MESSAGE_DESTINATION",
                        "MESSAGE_CONSUMER", "APPLICATION_SERVICE", "REPOSITORY");
        assertThat(overview.at("/verification/state").asText()).isEqualTo("AVAILABLE");
        assertThat(overview.at("/verification/verification/verificationStatus").asText())
                .isEqualTo("VERIFIED");
        assertThat(overview.at("/verification/verification/testCount").asInt()).isEqualTo(2);
        assertThat(overview.at("/verification/verification/checkCount").asInt()).isEqualTo(16);
        assertThat(overview.at("/verification/verification/tests").size()).isEqualTo(2);

        JsonNode detailsError = getJsonExpecting(
                422, "/api/v1/repositories/{repositoryId}/operations/{operationId}",
                repositoryId, operationId);
        assertThat(detailsError.path("code").asText()).isEqualTo("INCOMPLETE_IMPLEMENTATION_PATH");
        assertThat(overview.at("/eventPath/path")).isEqualTo(getJson(
                "/api/v1/repositories/{repositoryId}/operations/{operationId}/event-path",
                repositoryId, operationId));
        assertThat(overview.at("/verification/verification")).isEqualTo(getJson(
                "/api/v1/repositories/{repositoryId}/operations/{operationId}/tests",
                repositoryId, operationId));
    }

    private void qualifySynchronousRoot(String repositoryId, JsonNode operation) throws Exception {
        String operationId = operation.path("operationId").asText();
        JsonNode overview = overview(repositoryId, operationId);

        assertIdentity(overview, repositoryId, operation);
        assertThat(overview.at("/implementation/state").asText()).isEqualTo("INCOMPLETE");
        assertThat(overview.at("/eventPath/state").asText()).isEqualTo("NOT_APPLICABLE");
        assertThat(overview.at("/verification/state").asText()).isEqualTo("AVAILABLE");
        assertThat(overview.at("/verification/verification/verificationStatus").asText())
                .isEqualTo("UNVERIFIED");
        assertThat(overview.at("/verification/verification/testCount").asInt()).isZero();
        assertThat(overview.at("/verification/verification/checkCount").asInt()).isZero();
        assertThat(overview.at("/verification/verification/tests").isEmpty()).isTrue();

        JsonNode detailsError = getJsonExpecting(
                422, "/api/v1/repositories/{repositoryId}/operations/{operationId}",
                repositoryId, operationId);
        assertThat(detailsError.path("code").asText()).isEqualTo("INCOMPLETE_IMPLEMENTATION_PATH");
        JsonNode eventPathError = getJsonExpecting(
                422, "/api/v1/repositories/{repositoryId}/operations/{operationId}/event-path",
                repositoryId, operationId);
        assertThat(eventPathError.path("code").asText()).isEqualTo("NOT_EVENT_DRIVEN");
        assertThat(overview.at("/verification/verification")).isEqualTo(getJson(
                "/api/v1/repositories/{repositoryId}/operations/{operationId}/tests",
                repositoryId, operationId));
    }

    private static void assertIdentity(JsonNode overview, String repositoryId, JsonNode operation) {
        assertThat(overview.at("/identity/repositoryId").asText()).isEqualTo(repositoryId);
        assertThat(overview.at("/identity/operationId").asText())
                .isEqualTo(operation.path("operationId").asText());
        assertThat(overview.at("/identity/method").asText()).isEqualTo(operation.path("method").asText());
        assertThat(overview.at("/identity/path").asText()).isEqualTo(operation.path("path").asText());
        assertThat(overview.at("/identity/displayName").asText())
                .isEqualTo(operation.path("displayName").asText());
    }

    private String analyze(Path repository) throws Exception {
        byte[] request = JSON.writeValueAsBytes(
                new AnalyzeRepositoryRequest(repository.toString(), "order-events-kafka-tests"));
        String response = mvc.perform(post("/api/v1/repositories/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).path("repositoryId").asText();
    }

    private JsonNode overview(String repositoryId, String operationId) throws Exception {
        return getJson(OperationOverviewApiContract.PATH, repositoryId, operationId);
    }

    private JsonNode getJson(String path, Object... variables) throws Exception {
        return getJsonExpecting(200, path, variables);
    }

    private JsonNode getJsonExpecting(int statusCode, String path, Object... variables) throws Exception {
        String response = mvc.perform(get(path, variables))
                .andExpect(status().is(statusCode))
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response);
    }

    private static JsonNode operation(JsonNode operations, String method, String path) {
        return StreamSupport.stream(operations.path("operations").spliterator(), false)
                .filter(operation -> method.equals(operation.path("method").asText()))
                .filter(operation -> path.equals(operation.path("path").asText()))
                .findFirst().orElseThrow();
    }
}
