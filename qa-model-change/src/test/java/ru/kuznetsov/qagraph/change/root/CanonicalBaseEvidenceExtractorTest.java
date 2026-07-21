package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_COLLECTION_INVALID;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_COLLECTION_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_EXTRACTION_FAILED;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_SCHEMA_VERSION_INVALID;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_SCHEMA_VERSION_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_VERSION_UNSUPPORTED;

class CanonicalBaseEvidenceExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CanonicalBaseEvidenceExtractor extractor =
            new CanonicalBaseEvidenceExtractor();

    @Test
    void shouldExtractCompleteBoundEvidenceAndPreserveRetainedValues()
            throws Exception {
        ObjectNode source = root();

        CanonicalBaseModelEvidence evidence = assertInstanceOf(
                CanonicalBaseEvidenceExtracted.class,
                extractor.extract(source)
        ).evidence();

        assertEquals("0.1", evidence.rootContext().schemaVersion().value());
        assertEquals(3, evidence.artifactIndex().artifacts().size());
        ObjectNode retained = evidence.rootContext().retainedProperties();
        assertTrue(retained.has("project"));
        assertTrue(retained.has("sources"));
        assertTrue(retained.has("extension"));
        assertTrue(retained.get("extension").get("explicit").isNull());
        assertFalse(retained.has("schemaVersion"));
        assertFalse(retained.has("nodes"));
        assertFalse(retained.has("relationships"));

        source.withObject("/project").put("name", "mutated");
        retained.withObject("/project").put("name", "also-mutated");
        assertEquals(
                "base",
                evidence.rootContext().retainedProperties()
                        .get("project").get("name").textValue()
        );
    }

    @Test
    void shouldReturnExplicitVersionFailures() throws Exception {
        assertFailure(remove(root(), "schemaVersion"),
                ROOT_SCHEMA_VERSION_MISSING);
        assertFailure(replace(root(), "schemaVersion", 42),
                ROOT_SCHEMA_VERSION_INVALID);
        assertFailure(replace(root(), "schemaVersion", "9.9"),
                ROOT_VERSION_UNSUPPORTED);
    }

    @Test
    void shouldRejectMissingContextAndArtifactCollections() throws Exception {
        assertFailure(remove(root(), "project"),
                ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING);
        assertFailure(remove(root(), "sources"),
                ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING);
        assertFailure(remove(root(), "nodes"),
                ROOT_ARTIFACT_COLLECTION_MISSING);
        assertFailure(replace(root(), "relationships", "not-an-array"),
                ROOT_ARTIFACT_COLLECTION_INVALID);
    }

    @Test
    void malformedArtifactShouldFailWithoutPartialEvidence() throws Exception {
        ObjectNode malformedNode = root();
        malformedNode.withArray("nodes").addObject().put("name", "no-id");
        assertFailure(malformedNode, ROOT_ARTIFACT_EXTRACTION_FAILED);

        ObjectNode malformedRelationship = root();
        malformedRelationship.withArray("relationships")
                .addObject().put("from", "N-1").put("to", "N-2");
        assertFailure(malformedRelationship, ROOT_ARTIFACT_EXTRACTION_FAILED);
    }

    @Test
    void extractionFailuresShouldBeImmutableAndRejectInvalidConstruction()
            throws Exception {
        CanonicalBaseEvidenceExtractionFailure failure = assertInstanceOf(
                CanonicalBaseEvidenceExtractionFailure.class,
                extractor.extract(remove(root(), "nodes"))
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> failure.diagnostics().add(
                        failure.diagnostics().getFirst())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CanonicalBaseEvidenceExtractionFailure(List.of())
        );
    }

    private ObjectNode root() throws Exception {
        return (ObjectNode) mapper.readTree("""
                {
                  "schemaVersion":"0.1",
                  "project":{"name":"base"},
                  "sources":[{"uri":"a","optional":null}],
                  "nodes":[
                    {"id":"N-2","type":"CHECK"},
                    {"id":"N-1","type":"CHECK"}
                  ],
                  "relationships":[
                    {"id":"R-1","type":"RELATED_TO",
                     "from":"N-1","to":"N-2"}
                  ],
                  "extension":{"explicit":null}
                }
                """);
    }

    private ObjectNode remove(ObjectNode root, String property) {
        root.remove(property);
        return root;
    }

    private ObjectNode replace(ObjectNode root, String property, String value) {
        root.put(property, value);
        return root;
    }

    private ObjectNode replace(ObjectNode root, String property, int value) {
        root.put(property, value);
        return root;
    }

    private void assertFailure(
            ObjectNode root,
            BaseEvidenceExtractionDiagnosticCode code
    ) {
        CanonicalBaseEvidenceExtractionFailure result = assertInstanceOf(
                CanonicalBaseEvidenceExtractionFailure.class,
                extractor.extract(root)
        );
        assertEquals(code, result.diagnostics().getFirst().code());
    }
}
