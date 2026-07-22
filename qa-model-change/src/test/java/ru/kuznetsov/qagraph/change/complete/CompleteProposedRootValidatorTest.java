package ru.kuznetsov.qagraph.change.complete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseEvidenceExtracted;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseEvidenceExtractor;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import ru.kuznetsov.qagraph.change.root.ProposedCanonicalRoot;
import ru.kuznetsov.qagraph.change.root.ProposedCanonicalRootReconstructor;
import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.SCHEMA_INVALID;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.SEMANTICALLY_INVALID;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.UNSUPPORTED_VERSION;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.VALIDATION_INFRASTRUCTURE_FAILURE;

class CompleteProposedRootValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CompleteProposedRootValidator validator =
            new CompleteProposedRootValidator();

    @Test
    void minimalCompleteRootShouldPassAndRetainExactPhaseSevenEvidence()
            throws Exception {
        ProposedRootReconstructed reconstructed = reconstructed(minimalRoot());

        CompleteProposedRootValid result = assertInstanceOf(
                CompleteProposedRootValid.class,
                validator.validate(reconstructed)
        );

        assertSame(reconstructed, result.reconstructedRoot());
        assertTrue(result.schemaEvidence().valid());
        assertTrue(result.semanticEvidence().valid());
        assertTrue(result.schemaEvidence().diagnostics().isEmpty());
    }

    @Test
    void schemaShouldValidateWholeRootAndSkipSemanticTraversal()
            throws Exception {
        List<ObjectNode> invalidRoots = List.of(
                remove(minimalRoot(), "project"),
                remove(minimalRoot(), "sources"),
                replace(minimalRoot(), "project", mapper.createArrayNode()),
                replace(minimalRoot(), "sources", mapper.createObjectNode()),
                addNode(minimalRoot(), """
                        {"id":"bad/id","type":"CHECK","name":"check",
                         "check":{"checkType":"SQL","assertion":"ok"}}
                        """),
                addNode(minimalRoot(), """
                        {"id":"N-1","type":"CHECK","name":"check",
                         "extra":true,
                         "check":{"checkType":"SQL","assertion":"ok"}}
                        """),
                addRelationship(minimalRoot(), """
                        {"id":"R-1","type":"RELATED_TO","to":"N-1"}
                        """),
                addRelationship(minimalRoot(), """
                        {"id":"R-1","type":"RELATED_TO","from":"N-1"}
                        """)
        );
        CountingBackend backend = new CountingBackend();
        CompleteProposedRootValidator controlled =
                new CompleteProposedRootValidator(backend);
        backend.schemaIssues = List.of(ValidationIssue.schemaError(
                "REQUIRED", "required property is missing", "/project"));

        CompleteProposedRootInvalid controlledResult = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                controlled.validate(reconstructed(invalidRoots.getFirst()))
        );
        assertEquals(SCHEMA_INVALID, controlledResult.classification());
        assertEquals(0, backend.semanticCalls);

        for (ObjectNode invalid : invalidRoots) {
            CompleteProposedRootInvalid result = assertInstanceOf(
                    CompleteProposedRootInvalid.class,
                    validator.validate(reconstructed(invalid))
            );
            assertEquals(SCHEMA_INVALID, result.classification());
            assertTrue(result.schemaEvidence().isPresent());
            assertTrue(result.semanticEvidence().isEmpty());
            assertTrue(result.diagnostics().stream().allMatch(value ->
                    value.origin()
                            == CompleteValidationDiagnosticOrigin.SCHEMA));
            assertTrue(result.diagnostics().stream().allMatch(value ->
                    !value.path().isBlank() && !value.code().isBlank()));
        }
    }

    @Test
    void authoritativeSemanticCodesShouldRemainUnchanged() throws Exception {
        ObjectNode unknown = addRelationship(minimalRoot(), """
                {"id":"R-1","from":"MISSING-FROM",
                 "type":"RELATED_TO","to":"MISSING-TO"}
                """);
        assertSemanticCodes(unknown, "UNKNOWN_FROM_NODE", "UNKNOWN_TO_NODE");

        ObjectNode incompatible = minimalRoot();
        addNode(incompatible, check("N-1"));
        addNode(incompatible, check("N-2"));
        addRelationship(incompatible, """
                {"id":"R-1","from":"N-1","type":"RELATED_TO",
                 "to":"N-2"}
                """);
        assertSemanticCodes(incompatible, "RELATIONSHIP_NOT_ALLOWED");

        ObjectNode self = minimalRoot();
        addNode(self, check("N-1"));
        addRelationship(self, """
                {"id":"R-1","from":"N-1","type":"DEPENDS_ON",
                 "to":"N-1"}
                """);
        assertSemanticCodes(self, "SELF_REFERENCE_NOT_ALLOWED");

        ObjectNode scenario = addNode(minimalRoot(), """
                {"id":"SC-1","type":"SCENARIO","name":"scenario",
                 "scenario":{"code":"SC-1","given":[],
                 "when":[{"id":"S-1","text":"when"}],
                 "then":[{"id":"S-2","text":"then"}]}}
                """);
        assertSemanticCodes(scenario, "SCENARIO_WITHOUT_TEST");
    }

    @Test
    void aggregateValidShapeCanStillFailCompleteSemanticRules()
            throws Exception {
        ObjectNode root = minimalRoot();
        addNode(root, check("N-1"));
        addNode(root, check("N-2"));
        addRelationship(root, """
                {"id":"R-1","from":"N-1","type":"RELATED_TO",
                 "to":"N-2"}
                """);

        CompleteProposedRootInvalid result = semanticInvalid(root);
        assertEquals(SEMANTICALLY_INVALID, result.classification());
        assertTrue(result.schemaEvidence().orElseThrow().valid());
        assertFalse(result.semanticEvidence().orElseThrow().valid());
    }

    @Test
    void unknownRetainedPropertyShouldSurvivePhaseSevenAndFailSchema()
            throws Exception {
        ObjectNode base = minimalRoot();
        base.putObject("unknownRootProperty").put("preserved", true);
        CanonicalBaseModelEvidence evidence = extracted(base);
        ProposedArtifactModel model = emptyModel(evidence);
        AggregateTransitionValid aggregate = aggregate(evidence, model);
        ProposedRootReconstructed reconstructed = assertInstanceOf(
                ProposedRootReconstructed.class,
                new ProposedCanonicalRootReconstructor().reconstruct(
                        evidence,
                        aggregate
                )
        );
        assertTrue(reconstructed.proposedRoot().snapshot()
                .has("unknownRootProperty"));

        CompleteProposedRootInvalid result = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                validator.validate(reconstructed)
        );
        assertEquals(SCHEMA_INVALID, result.classification());
        assertTrue(result.diagnostics().stream().anyMatch(value ->
                value.code().equals("ADDITIONALPROPERTIES")));
        assertTrue(reconstructed.proposedRoot().snapshot()
                .has("unknownRootProperty"));
    }

    @Test
    void diagnosticsShouldBeDeduplicatedSortedAndImmutable()
            throws Exception {
        CountingBackend backend = new CountingBackend();
        backend.semanticIssues = List.of(
                ValidationIssue.semanticWarning("Z", "last", "B", "/z"),
                ValidationIssue.semanticError("A", "first", "A", "/a"),
                ValidationIssue.semanticError("A", "first", "A", "/a")
        );
        CompleteProposedRootValidator controlled =
                new CompleteProposedRootValidator(backend);

        CompleteProposedRootInvalid first = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                controlled.validate(reconstructed(minimalRoot()))
        );
        CompleteProposedRootInvalid second = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                controlled.validate(reconstructed(minimalRoot()))
        );

        assertEquals(List.of("A", "Z"), first.diagnostics().stream()
                .map(CompleteValidationDiagnostic::code).toList());
        assertEquals(first.diagnostics(), second.diagnostics());
        assertThrows(UnsupportedOperationException.class,
                () -> first.diagnostics().add(first.diagnostics().getFirst()));
        assertThrows(UnsupportedOperationException.class,
                () -> first.semanticEvidence().orElseThrow()
                        .authoritativeIssues().clear());
    }

    @Test
    void warningOrderShouldBeDeterministicAndAuthoritativeFieldsBound()
            throws Exception {
        ValidationIssue z = ValidationIssue.semanticWarning(
                "Z_WARNING", "last", "Z", "/z");
        ValidationIssue a = ValidationIssue.semanticWarning(
                "A_WARNING", "first", "A", "/a");
        CountingBackend left = new CountingBackend();
        left.semanticIssues = List.of(z, a);
        CountingBackend right = new CountingBackend();
        right.semanticIssues = List.of(a, z);

        CompleteProposedRootValid first = assertInstanceOf(
                CompleteProposedRootValid.class,
                new CompleteProposedRootValidator(left)
                        .validate(reconstructed(minimalRoot())));
        CompleteProposedRootValid second = assertInstanceOf(
                CompleteProposedRootValid.class,
                new CompleteProposedRootValidator(right)
                        .validate(reconstructed(minimalRoot())));

        assertEquals(first.semanticEvidence().diagnostics(),
                second.semanticEvidence().diagnostics());
        assertEquals(List.of("A_WARNING", "Z_WARNING"),
                first.semanticEvidence().diagnostics().stream()
                        .map(CompleteValidationDiagnostic::code).toList());
        assertTrue(first.semanticEvidence().diagnostics().stream().allMatch(d ->
                d.severity() == d.authoritativeIssue().severity()
                        && d.code().equals(d.authoritativeIssue().code())
                        && d.message().equals(d.authoritativeIssue().message())
                        && d.objectId().equals(
                        d.authoritativeIssue().objectId())));
        assertThrows(UnsupportedOperationException.class,
                () -> first.semanticEvidence().diagnostics().clear());
        assertThrows(IllegalArgumentException.class,
                () -> new CompleteValidationDiagnostic(
                        CompleteValidationDiagnosticOrigin.SEMANTIC,
                        ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity.WARNING,
                        "CONFLICT", "/a", "first", "A", a));
    }

    @Test
    void infrastructureFailuresShouldRemainDistinctByStage()
            throws Exception {
        CountingBackend schemaFailure = new CountingBackend();
        schemaFailure.schemaException = new IllegalStateException("missing");
        CompleteProposedRootInvalid schemaResult = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                new CompleteProposedRootValidator(schemaFailure)
                        .validate(reconstructed(minimalRoot()))
        );
        assertEquals(VALIDATION_INFRASTRUCTURE_FAILURE,
                schemaResult.classification());
        assertEquals(CompleteValidationStage.SCHEMA, schemaResult.stage());
        assertTrue(schemaResult.diagnostics().isEmpty());

        CountingBackend semanticFailure = new CountingBackend();
        semanticFailure.semanticException = new IllegalStateException("broken");
        CompleteProposedRootInvalid semanticResult = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                new CompleteProposedRootValidator(semanticFailure)
                        .validate(reconstructed(minimalRoot()))
        );
        assertEquals(CompleteValidationStage.SEMANTIC,
                semanticResult.stage());
        assertTrue(semanticResult.schemaEvidence().orElseThrow().valid());
        assertTrue(semanticResult.semanticEvidence().isEmpty());
    }

    @Test
    void unsupportedRootVersionShouldRunNeitherAuthoritativeValidator()
            throws Exception {
        ObjectNode unsupported = minimalRoot();
        unsupported.put("schemaVersion", "9.9");
        CountingBackend backend = new CountingBackend();

        CompleteProposedRootInvalid result = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                new CompleteProposedRootValidator(backend)
                        .validate(reconstructed(unsupported))
        );

        assertEquals(UNSUPPORTED_VERSION, result.classification());
        assertEquals(CompleteValidationStage.VERSION, result.stage());
        assertEquals(0, backend.schemaCalls);
        assertEquals(0, backend.semanticCalls);
        assertEquals("9.9", result.reconstructedRoot().proposedRoot()
                .snapshot().get("schemaVersion").textValue());
    }

    @Test
    void validatorShouldNotMutateOrExposeRootJson() throws Exception {
        ProposedRootReconstructed reconstructed = reconstructed(minimalRoot());
        CompleteProposedRootValid result = assertInstanceOf(
                CompleteProposedRootValid.class,
                validator.validate(reconstructed)
        );
        ObjectNode exposed = result.reconstructedRoot().proposedRoot().snapshot();
        exposed.remove("project");
        assertTrue(result.reconstructedRoot().proposedRoot().snapshot()
                .has("project"));
    }

    private void assertSemanticCodes(ObjectNode root, String... expected)
            throws Exception {
        CompleteProposedRootValidationResult result = validator.validate(
                reconstructed(root)
        );
        List<CompleteValidationDiagnostic> diagnostics = switch (result) {
            case CompleteProposedRootValid success ->
                    success.semanticEvidence().diagnostics();
            case CompleteProposedRootInvalid failure ->
                    failure.semanticEvidence().orElseThrow().diagnostics();
        };
        List<String> codes = diagnostics.stream()
                .map(CompleteValidationDiagnostic::code).toList();
        assertTrue(diagnostics.stream().allMatch(value ->
                value.origin() == CompleteValidationDiagnosticOrigin.SEMANTIC));
        for (String code : expected) {
            assertTrue(codes.contains(code), () -> "Missing code " + code);
        }
    }

    private CompleteProposedRootInvalid semanticInvalid(ObjectNode root)
            throws Exception {
        CompleteProposedRootInvalid result = assertInstanceOf(
                CompleteProposedRootInvalid.class,
                validator.validate(reconstructed(root))
        );
        assertEquals(SEMANTICALLY_INVALID, result.classification());
        assertTrue(result.diagnostics().stream().allMatch(value ->
                value.origin() == CompleteValidationDiagnosticOrigin.SEMANTIC));
        return result;
    }

    private ProposedRootReconstructed reconstructed(ObjectNode root)
            throws Exception {
        CanonicalBaseModelEvidence evidence = extracted(minimalRoot());
        ProposedArtifactModel model = emptyModel(evidence);
        AggregateTransitionValid aggregate = aggregate(evidence, model);
        return ru.kuznetsov.qagraph.change.root.RootTestFixtures.reconstructed(
                new ProposedCanonicalRoot(root),
                evidence,
                aggregate
        );
    }

    private CanonicalBaseModelEvidence extracted(ObjectNode root) {
        return assertInstanceOf(
                CanonicalBaseEvidenceExtracted.class,
                new CanonicalBaseEvidenceExtractor().extract(root)
        ).evidence();
    }

    private ProposedArtifactModel emptyModel(
            CanonicalBaseModelEvidence evidence
    ) {
        return new ProposedArtifactModel(
                evidence.rootContext().schemaVersion(),
                List.of(),
                List.of()
        );
    }

    private AggregateTransitionValid aggregate(
            CanonicalBaseModelEvidence evidence,
            ProposedArtifactModel model
    ) {
        return ru.kuznetsov.qagraph.change.aggregate.AggregateTestFixtures.valid(
                ru.kuznetsov.qagraph.change.materialization.MaterializationTestFixtures.materialized(model, evidence));
    }

    private ObjectNode minimalRoot() throws Exception {
        return (ObjectNode) mapper.readTree("""
                {"schemaVersion":"0.1",
                 "project":{"id":"P-1","name":"Project"},
                 "sources":[],"nodes":[],"relationships":[]}
                """);
    }

    private String check(String id) {
        return "{\"id\":\"" + id + "\",\"type\":\"CHECK\","
                + "\"name\":\"check\",\"check\":{\"checkType\":"
                + "\"SQL\",\"assertion\":\"ok\"}}";
    }

    private ObjectNode addNode(ObjectNode root, String json)
            throws Exception {
        root.withArray("nodes").add(mapper.readTree(json));
        return root;
    }

    private ObjectNode addRelationship(ObjectNode root, String json)
            throws Exception {
        root.withArray("relationships").add(mapper.readTree(json));
        return root;
    }

    private ObjectNode remove(ObjectNode root, String field) {
        root.remove(field);
        return root;
    }

    private ObjectNode replace(
            ObjectNode root,
            String field,
            JsonNode replacement
    ) {
        root.set(field, replacement);
        return root;
    }

    private static final class CountingBackend
            implements CompleteValidationBackend {
        private List<ValidationIssue> schemaIssues = List.of();
        private List<ValidationIssue> semanticIssues = List.of();
        private RuntimeException schemaException;
        private RuntimeException semanticException;
        private int schemaCalls;
        private int semanticCalls;

        @Override
        public List<ValidationIssue> validateSchema(JsonNode root) {
            schemaCalls++;
            if (schemaException != null) {
                throw schemaException;
            }
            return new ArrayList<>(schemaIssues);
        }

        @Override
        public List<ValidationIssue> validateSemantic(JsonNode root) {
            semanticCalls++;
            if (semanticException != null) {
                throw semanticException;
            }
            return new ArrayList<>(semanticIssues);
        }
    }
}
