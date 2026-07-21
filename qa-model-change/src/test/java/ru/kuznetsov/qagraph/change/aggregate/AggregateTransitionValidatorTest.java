package ru.kuznetsov.qagraph.change.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.PROPOSED_MODEL_VERSION_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_ENDPOINT_INVALID;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_ENDPOINT_MISSING;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_NODE_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_ENDPOINT_INVALID;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_ENDPOINT_MISSING;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_NODE_NOT_FOUND;

class AggregateTransitionValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AggregateTransitionValidator validator =
            new AggregateTransitionValidator();

    @Test
    void nodeOnlyModelShouldSucceedWithoutFinalVerificationClaim()
            throws Exception {
        ProposedModelMaterialized materialization = materialized(
                CanonicalQaModelVersion.V0_1,
                List.of(node("N-1")),
                List.of()
        );

        AggregateTransitionValid result = assertInstanceOf(
                AggregateTransitionValid.class,
                validator.validate(materialization)
        );

        assertEquals(materialization, result.materialization());
    }

    @Test
    void relationshipsWithExistingExactEndpointsShouldSucceed()
            throws Exception {
        assertValid(model(
                List.of(node("N-1"), node("N-2")),
                List.of(
                        relationship("R-2", "N-2", "N-1"),
                        relationship("R-1", "N-1", "N-2")
                )
        ));
    }

    @Test
    void nodeAddedWithReferencingRelationshipInFinalModelShouldSucceed()
            throws Exception {
        assertValid(model(
                List.of(node("NEW"), node("EXISTING")),
                List.of(relationship("R-1", "EXISTING", "NEW"))
        ));
    }

    @Test
    void nodeAndRelationshipSameIdentityShouldRemainCategoryDistinct()
            throws Exception {
        assertValid(model(
                List.of(node("SAME"), node("OTHER")),
                List.of(relationship("SAME", "SAME", "OTHER"))
        ));

        assertInvalid(model(
                        List.of(node("OTHER")),
                        List.of(relationship("MISSING", "MISSING", "OTHER"))),
                RELATIONSHIP_SOURCE_NODE_NOT_FOUND);
    }

    @Test
    void selfRelationshipIsPreservedForValidationCoreOwnership()
            throws Exception {
        assertValid(model(
                List.of(node("N-1")),
                List.of(relationship("R-1", "N-1", "N-1"))
        ));
    }

    @Test
    void missingEndpointPropertiesShouldProduceIndependentDiagnostics()
            throws Exception {
        AggregateTransitionInvalid result = assertInvalid(
                model(List.of(), List.of(rawRelationship("""
                        {"id":"R-1","type":"RELATED_TO"}
                        """))),
                RELATIONSHIP_SOURCE_ENDPOINT_MISSING,
                RELATIONSHIP_TARGET_ENDPOINT_MISSING
        );

        assertEquals(List.of(
                AggregateTransitionFailureKind.STRUCTURALLY_INVALID,
                AggregateTransitionFailureKind.STRUCTURALLY_INVALID
        ), result.diagnostics().stream()
                .map(AggregateTransitionDiagnostic::failureKind)
                .toList());
    }

    @Test
    void missingEndpointShouldNotProduceNodeNotFoundForThatEndpoint()
            throws Exception {
        assertInvalid(model(
                        List.of(),
                        List.of(rawRelationship("""
                                {"id":"R-1","type":"RELATED_TO",
                                 "to":"MISSING"}
                                """))),
                RELATIONSHIP_SOURCE_ENDPOINT_MISSING,
                RELATIONSHIP_TARGET_NODE_NOT_FOUND);
    }

    @Test
    void nullAndNonTextualEndpointsShouldBeInvalid() throws Exception {
        assertInvalid(model(
                        List.of(),
                        List.of(rawRelationship("""
                                {"id":"R-1","type":"RELATED_TO",
                                 "from":null,"to":42}
                                """))),
                RELATIONSHIP_SOURCE_ENDPOINT_INVALID,
                RELATIONSHIP_TARGET_ENDPOINT_INVALID);
    }

    @Test
    void blankAndInvalidCharacterEndpointsShouldBeInvalid()
            throws Exception {
        assertInvalid(model(
                        List.of(),
                        List.of(rawRelationship("""
                                {"id":"R-1","type":"RELATED_TO",
                                 "from":" ","to":"bad/id"}
                                """))),
                RELATIONSHIP_SOURCE_ENDPOINT_INVALID,
                RELATIONSHIP_TARGET_ENDPOINT_INVALID);
    }

    @Test
    void invalidEndpointShouldSuppressLookupOnlyForThatEndpoint()
            throws Exception {
        AggregateTransitionInvalid result = assertInvalid(model(
                        List.of(),
                        List.of(rawRelationship("""
                                {"id":"R-1","type":"RELATED_TO",
                                 "from":"bad/id","to":"MISSING"}
                                """))),
                RELATIONSHIP_SOURCE_ENDPOINT_INVALID,
                RELATIONSHIP_TARGET_NODE_NOT_FOUND);

        assertEquals(List.of("bad/id", "MISSING"),
                result.diagnostics().stream()
                        .map(value -> value.endpointValue().orElseThrow())
                        .toList());
    }

    @Test
    void bothDanglingEndpointsShouldProduceTwoDiagnostics()
            throws Exception {
        assertInvalid(model(
                        List.of(),
                        List.of(relationship("R-1", "MISSING-A", "MISSING-B"))),
                RELATIONSHIP_SOURCE_NODE_NOT_FOUND,
                RELATIONSHIP_TARGET_NODE_NOT_FOUND);
    }

    @Test
    void removedNodeWithPreservedRelationshipShouldFail()
            throws Exception {
        assertInvalid(model(
                        List.of(node("PRESENT")),
                        List.of(relationship("R-1", "REMOVED", "PRESENT"))),
                RELATIONSHIP_SOURCE_NODE_NOT_FOUND);
    }

    @Test
    void addedOrModifiedRelationshipToAbsentNodeShouldFail()
            throws Exception {
        assertInvalid(model(
                        List.of(node("PRESENT")),
                        List.of(relationship("ADDED", "PRESENT", "ABSENT"))),
                RELATIONSHIP_TARGET_NODE_NOT_FOUND);
        assertInvalid(model(
                        List.of(node("PRESENT")),
                        List.of(relationship("MODIFIED", "ABSENT", "PRESENT"))),
                RELATIONSHIP_SOURCE_NODE_NOT_FOUND);
    }

    @Test
    void removingNodeAndAllReferencingRelationshipsShouldSucceed()
            throws Exception {
        assertValid(model(List.of(node("PRESERVED")), List.of()));
    }

    @Test
    void endpointLookupShouldBeCaseSensitiveAndNeverTrim()
            throws Exception {
        assertInvalid(model(
                        List.of(node("Exact")),
                        List.of(relationship("R-1", "exact", "Exact"))),
                RELATIONSHIP_SOURCE_NODE_NOT_FOUND);
        assertInvalid(model(
                        List.of(node("Exact")),
                        List.of(rawRelationship("""
                                {"id":"R-2","type":"RELATED_TO",
                                 "from":" Exact ","to":"Exact"}
                                """))),
                RELATIONSHIP_SOURCE_ENDPOINT_INVALID);
    }

    @Test
    void unsupportedVersionShouldStopEndpointTraversal() throws Exception {
        CanonicalQaModelVersion unsupported =
                new CanonicalQaModelVersion("9.9");
        ProposedModelMaterialized input = materialized(
                unsupported,
                List.of(),
                List.of(rawRelationship(unsupported, """
                        {"id":"R-1","type":"RELATED_TO"}
                        """))
        );

        assertInvalid(input, PROPOSED_MODEL_VERSION_UNSUPPORTED);
    }

    @Test
    void diagnosticsShouldBeDeterministicImmutableAndInputOrderIndependent()
            throws Exception {
        RelationshipArtifactState second = relationship(
                "R-2", "MISSING-2A", "MISSING-2B"
        );
        RelationshipArtifactState first = relationship(
                "R-1", "MISSING-1A", "MISSING-1B"
        );
        ProposedModelMaterialized forward = model(
                List.of(), List.of(second, first));
        ProposedModelMaterialized reverse = model(
                List.of(), List.of(first, second));

        AggregateTransitionInvalid firstResult = assertInstanceOf(
                AggregateTransitionInvalid.class,
                validator.validate(forward)
        );
        AggregateTransitionInvalid repeated = assertInstanceOf(
                AggregateTransitionInvalid.class,
                validator.validate(forward)
        );
        AggregateTransitionInvalid reversed = assertInstanceOf(
                AggregateTransitionInvalid.class,
                validator.validate(reverse)
        );

        assertEquals(firstResult, repeated);
        assertEquals(firstResult.diagnostics(), reversed.diagnostics());
        assertEquals(List.of("R-1", "R-1", "R-2", "R-2"),
                firstResult.diagnostics().stream()
                        .map(value -> value.relationshipIdentity()
                                .orElseThrow().value())
                        .toList());
        assertThrows(UnsupportedOperationException.class,
                () -> firstResult.diagnostics().clear());
    }

    private void assertValid(ProposedModelMaterialized materialization) {
        assertInstanceOf(
                AggregateTransitionValid.class,
                validator.validate(materialization)
        );
    }

    private AggregateTransitionInvalid assertInvalid(
            ProposedModelMaterialized materialization,
            AggregateTransitionDiagnosticCode... codes
    ) {
        AggregateTransitionInvalid result = assertInstanceOf(
                AggregateTransitionInvalid.class,
                validator.validate(materialization)
        );
        assertEquals(List.of(codes), result.diagnostics().stream()
                .map(AggregateTransitionDiagnostic::code)
                .toList());
        return result;
    }

    private ProposedModelMaterialized model(
            List<NodeArtifactState> nodes,
            List<RelationshipArtifactState> relationships
    ) {
        return materialized(
                CanonicalQaModelVersion.V0_1,
                nodes,
                relationships
        );
    }

    private ProposedModelMaterialized materialized(
            CanonicalQaModelVersion version,
            List<NodeArtifactState> nodes,
            List<RelationshipArtifactState> relationships
    ) {
        return new ProposedModelMaterialized(new ProposedArtifactModel(
                version,
                nodes,
                relationships
        ));
    }

    private NodeArtifactState node(String id) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"CHECK","name":"Node"}
                        """.formatted(id))
        );
    }

    private RelationshipArtifactState relationship(
            String id,
            String from,
            String to
    ) throws Exception {
        return rawRelationship("""
                {"id":"%s","type":"RELATED_TO",
                 "from":"%s","to":"%s"}
                """.formatted(id, from, to));
    }

    private RelationshipArtifactState rawRelationship(String json)
            throws Exception {
        return rawRelationship(CanonicalQaModelVersion.V0_1, json);
    }

    private RelationshipArtifactState rawRelationship(
            CanonicalQaModelVersion version,
            String json
    ) throws Exception {
        return new RelationshipArtifactState(version, mapper.readTree(json));
    }
}
