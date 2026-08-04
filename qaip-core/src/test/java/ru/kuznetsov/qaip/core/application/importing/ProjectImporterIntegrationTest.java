package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectImporterIntegrationTest {
    private final ProjectImporter importer = new DefaultProjectImporter(
            new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
            new DefaultProjectBinder(), new DefaultProjectApplicationValidator());

    @Test
    void real_pipeline_returns_final_proof_with_lossless_domain_content() throws IOException {
        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(representativeProjectWithNumericMetadata())));
        ApplicationValidProjectDocument proof = assertInstanceOf(
                ApplicationValidProjectDocument.class, success.document());
        var project = proof.project();
        assertTrue(success.warnings().isEmpty());
        assertEquals("P-1", project.metadata().id());
        assertEquals("local-1", project.subject().localArtifactId());
        assertEquals(2, project.nodes().size());
        assertEquals(1, project.relationships().size());
        assertEquals("jira", project.evidenceManifest().sourceId());
        assertEquals(2, project.declaredChanges().size());
        assertEquals(new BigInteger("9223372036854775808"),
                project.metadata().attributes().get("largeInteger"));
        assertEquals(0, new BigDecimal("1234567890.123456789012345678900").compareTo(
                (BigDecimal) project.metadata().attributes().get("preciseDecimal")));
    }

    @Test
    void real_pipeline_reports_malformed_json_at_parsing_stage() {
        ProjectImportFailure failure = assertInstanceOf(ProjectImportFailure.class,
                importer.importProject(new RawProjectJson("{ invalid")));
        assertEquals(ProjectImportStage.PARSING, failure.failedStage());
        assertEquals("MALFORMED_JSON", failure.findings().getFirst().code());
    }

    @Test
    void real_pipeline_accepts_canonical_direct_implementation_chain() throws IOException {
        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(resource(
                        "/canonical/valid-implementation-chain-project.json"))));

        var project = success.document().project();
        assertTrue(success.warnings().isEmpty());
        assertEquals(4, project.nodes().size());
        assertEquals(3, project.relationships().size());
        assertEquals(List.of("IMPLEMENTED_BY", "USES", "USES"),
                project.relationships().stream().map(relationship -> relationship.type()).toList());
    }

    private static String representativeProjectWithNumericMetadata() throws IOException {
        try (var stream = ProjectImporterIntegrationTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return json.replace("\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}",
                    "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{"
                            + "\"largeInteger\":9223372036854775808,"
                            + "\"preciseDecimal\":1234567890.123456789012345678900}}" );
        }
    }

    private static String resource(String path) throws IOException {
        try (var stream = ProjectImporterIntegrationTest.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Canonical project fixture is missing: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
