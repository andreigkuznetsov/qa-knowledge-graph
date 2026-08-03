package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryAnalysisContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void accepts_valid_request_without_inspecting_the_filesystem() {
        Path missingRelativeRoot = Path.of("does-not-need-to-exist");

        var request = new RepositoryAnalysisRequest(missingRelativeRoot, "Orders");

        assertEquals(missingRelativeRoot, request.repositoryRoot());
        assertEquals("Orders", request.projectName());
    }

    @Test
    void rejects_null_repository_root() {
        assertThrows(NullPointerException.class, () -> new RepositoryAnalysisRequest(null, "Orders"));
    }

    @Test
    void rejects_blank_project_name() {
        assertThrows(IllegalArgumentException.class,
                () -> new RepositoryAnalysisRequest(Path.of("repository"), " \t"));
    }

    @Test
    void accepts_complete_result_and_defensively_copies_canonical_json() {
        var source = JSON.createObjectNode().put("projectContractVersion", "qaip-project-v1");

        var result = new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.COMPLETE, "PROJECT-ORDERS", source, 3, List.of(), Optional.empty());
        source.put("mutated", true);
        var returned = result.canonicalProjectJson();
        ((ObjectNode) returned).put("consumerMutation", true);

        assertEquals("qaip-project-v1", result.canonicalProjectJson().get("projectContractVersion").textValue());
        assertEquals(false, result.canonicalProjectJson().has("mutated"));
        assertEquals(false, result.canonicalProjectJson().has("consumerMutation"));
        assertNotSame(returned, result.canonicalProjectJson());
    }

    @Test
    void accepts_partial_result() {
        var result = new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.PARTIAL,
                "PROJECT-ORDERS",
                JSON.createObjectNode(),
                2,
                List.of("One unsupported path was omitted"),
                Optional.empty());

        assertEquals(RepositoryAnalysisStatus.PARTIAL, result.status());
        assertEquals(2, result.discoveredOperationCount());
        assertEquals(List.of("One unsupported path was omitted"), result.warnings());
    }

    @Test
    void accepts_failed_result() {
        var result = new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.FAILED,
                null,
                null,
                2,
                List.of(),
                Optional.of("Repository cannot be analyzed"));

        assertEquals(RepositoryAnalysisStatus.FAILED, result.status());
        assertEquals(2, result.discoveredOperationCount());
        assertEquals(Optional.of("Repository cannot be analyzed"), result.failureMessage());
    }

    @Test
    void rejects_invalid_state_combinations() {
        var canonical = JSON.createObjectNode();

        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.COMPLETE, "PROJECT", canonical, 1,
                List.of("warning"), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.PARTIAL, "PROJECT", canonical, 1,
                List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.FAILED, "PROJECT", canonical, 1,
                List.of(), Optional.of("failure")));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.FAILED, null, null, 0,
                List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.COMPLETE, "PROJECT", canonical, -1,
                List.of(), Optional.empty()));
    }

    @Test
    void warnings_are_immutable_and_detached_from_the_input_list() {
        var warnings = new ArrayList<>(List.of("Unsupported construct omitted"));
        var result = new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.PARTIAL,
                "PROJECT",
                JSON.createObjectNode(),
                1,
                warnings,
                Optional.empty());

        warnings.add("later mutation");

        assertEquals(List.of("Unsupported construct omitted"), result.warnings());
        assertThrows(UnsupportedOperationException.class, () -> result.warnings().add("consumer mutation"));
    }
}
