package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterializer;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.BASE_ROOT_EVIDENCE_MISMATCH;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.PROPOSED_MODEL_VERSION_MISMATCH;

class ProposedCanonicalRootReconstructorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CanonicalBaseEvidenceExtractor extractor =
            new CanonicalBaseEvidenceExtractor();
    private final ProposedCanonicalRootReconstructor reconstructor =
            new ProposedCanonicalRootReconstructor();

    @Test
    void materializerShouldCarryExactExtractedRootEvidence()
            throws Exception {
        CanonicalBaseModelEvidence evidence = evidence(root());
        BaseChangeSetResult verified = new BaseChangeSetResult(
                evidence.artifactIndex(),
                Optional.of(evidence),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        ProposedModelMaterialized materialized = assertInstanceOf(
                ProposedModelMaterialized.class,
                new ProposedModelMaterializer().materialize(
                        evidence.artifactIndex(),
                        verified
                )
        );

        assertTrue(materialized.baseEvidence().isPresent());
        assertTrue(materialized.baseEvidence().orElseThrow() == evidence);
    }

    @Test
    void shouldReconstructDeterministicallyWithReplacedArtifactArrays()
            throws Exception {
        CanonicalBaseModelEvidence evidence = evidence(root());
        ProposedArtifactModel proposed = new ProposedArtifactModel(
                evidence.rootContext().schemaVersion(),
                List.of(node("N-3"), node("N-1")),
                List.of(relationship("R-2", "N-1", "N-3"))
        );

        ProposedRootReconstructed result = reconstruct(evidence, proposed);
        ObjectNode snapshot = result.proposedRoot().snapshot();

        assertEquals(
                List.of("schemaVersion", "project", "sources", "a-extra",
                        "z-extra", "nodes", "relationships"),
                iterable(snapshot.fieldNames())
        );
        assertEquals(
                List.of("N-1", "N-3"),
                snapshot.withArray("nodes").valueStream()
                        .map(value -> value.get("id").textValue()).toList()
        );
        assertEquals("R-2", snapshot.withArray("relationships")
                .get(0).get("id").textValue());
        assertTrue(snapshot.get("a-extra").get("explicit").isNull());
        assertEquals("kept", snapshot.get("z-extra").textValue());
    }

    @Test
    void unchangedEligibleModelShouldReconstructWithoutInventingValues()
            throws Exception {
        CanonicalBaseModelEvidence evidence = evidence(root());
        ProposedArtifactModel proposed = new ProposedArtifactModel(
                evidence.rootContext().schemaVersion(),
                evidence.artifactIndex().artifacts().stream()
                        .filter(NodeArtifactState.class::isInstance)
                        .map(NodeArtifactState.class::cast).toList(),
                evidence.artifactIndex().artifacts().stream()
                        .filter(RelationshipArtifactState.class::isInstance)
                        .map(RelationshipArtifactState.class::cast).toList()
        );

        ObjectNode snapshot = reconstruct(evidence, proposed)
                .proposedRoot().snapshot();
        assertEquals(2, snapshot.withArray("nodes").size());
        assertEquals(1, snapshot.withArray("relationships").size());
        assertEquals(evidence.rootContext().retainedProperties().get("project"),
                snapshot.get("project"));
    }

    @Test
    void exactBoundEvidenceShouldBeRequiredEvenForEqualRoots()
            throws Exception {
        CanonicalBaseModelEvidence first = evidence(root());
        CanonicalBaseModelEvidence second = evidence(root());
        AggregateTransitionValid transition = transition(
                first,
                new ProposedArtifactModel(
                        first.rootContext().schemaVersion(),
                        List.of(node("N-1")),
                        List.of()
                )
        );

        ProposedRootReconstructionFailure failure = assertInstanceOf(
                ProposedRootReconstructionFailure.class,
                reconstructor.reconstruct(second, transition)
        );
        assertEquals(BASE_ROOT_EVIDENCE_MISMATCH,
                failure.diagnostics().getFirst().code());
    }

    @Test
    void absentBindingAndVersionMismatchShouldBeExplicit() throws Exception {
        CanonicalBaseModelEvidence evidence = evidence(root());
        ProposedArtifactModel model = new ProposedArtifactModel(
                evidence.rootContext().schemaVersion(),
                List.of(node("N-1")),
                List.of()
        );
        ProposedRootReconstructionFailure absent = assertInstanceOf(
                ProposedRootReconstructionFailure.class,
                reconstructor.reconstruct(evidence, new AggregateTransitionValid(
                        new ProposedModelMaterialized(model)))
        );
        assertEquals(BASE_ROOT_EVIDENCE_MISMATCH,
                absent.diagnostics().getFirst().code());

        CanonicalRootContext otherContext = new CanonicalRootContext(
                new ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion(
                        "9.9"),
                evidence.rootContext().retainedProperties()
        );
        CanonicalBaseModelEvidence incompatible = new CanonicalBaseModelEvidence(
                otherContext,
                new ru.kuznetsov.qagraph.change.base.BaseArtifactIndex(
                        otherContext.schemaVersion(), List.of())
        );
        ProposedRootReconstructionFailure mismatch = assertInstanceOf(
                ProposedRootReconstructionFailure.class,
                reconstructor.reconstruct(incompatible, transition(
                        incompatible,
                        model
                ))
        );
        assertEquals(
                ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.ROOT_VERSION_UNSUPPORTED,
                mismatch.diagnostics().getFirst().code()
        );
    }

    @Test
    void reconstructedRootAndFailureDiagnosticsShouldBeImmutable()
            throws Exception {
        CanonicalBaseModelEvidence evidence = evidence(root());
        ProposedRootReconstructed success = reconstruct(
                evidence,
                new ProposedArtifactModel(
                        evidence.rootContext().schemaVersion(),
                        List.of(node("N-1")),
                        List.of()
                )
        );
        ObjectNode exposed = success.proposedRoot().snapshot();
        exposed.withObject("/project").put("name", "mutated");
        assertEquals("base", success.proposedRoot().snapshot()
                .get("project").get("name").textValue());

        ProposedRootReconstructionFailure failure = assertInstanceOf(
                ProposedRootReconstructionFailure.class,
                reconstructor.reconstruct(evidence, new AggregateTransitionValid(
                        new ProposedModelMaterialized(success.aggregateTransition()
                                .materialization().proposedModel())))
        );
        assertThrows(UnsupportedOperationException.class,
                () -> failure.diagnostics().add(
                        failure.diagnostics().getFirst()));
    }

    private ProposedRootReconstructed reconstruct(
            CanonicalBaseModelEvidence evidence,
            ProposedArtifactModel model
    ) {
        return assertInstanceOf(
                ProposedRootReconstructed.class,
                reconstructor.reconstruct(evidence, transition(evidence, model))
        );
    }

    private AggregateTransitionValid transition(
            CanonicalBaseModelEvidence evidence,
            ProposedArtifactModel model
    ) {
        return new AggregateTransitionValid(new ProposedModelMaterialized(
                model,
                Optional.of(evidence)
        ));
    }

    private CanonicalBaseModelEvidence evidence(ObjectNode root) {
        return assertInstanceOf(CanonicalBaseEvidenceExtracted.class,
                extractor.extract(root)).evidence();
    }

    private ObjectNode root() throws Exception {
        return (ObjectNode) mapper.readTree("""
                {
                  "schemaVersion":"0.1",
                  "project":{"name":"base"},
                  "sources":[{"uri":"source"}],
                  "nodes":[
                    {"id":"N-2","type":"CHECK"},
                    {"id":"N-1","type":"CHECK"}
                  ],
                  "relationships":[
                    {"id":"R-1","type":"RELATED_TO",
                     "from":"N-1","to":"N-2"}
                  ],
                  "z-extra":"kept",
                  "a-extra":{"explicit":null}
                }
                """);
    }

    private NodeArtifactState node(String id) throws Exception {
        return new NodeArtifactState(
                ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion.V0_1,
                mapper.readTree("{\"id\":\"" + id
                        + "\",\"type\":\"CHECK\"}")
        );
    }

    private RelationshipArtifactState relationship(
            String id,
            String from,
            String to
    ) throws Exception {
        return new RelationshipArtifactState(
                ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion.V0_1,
                mapper.readTree("{\"id\":\"" + id
                        + "\",\"type\":\"RELATED_TO\",\"from\":\""
                        + from + "\",\"to\":\"" + to + "\"}")
        );
    }

    private List<String> iterable(java.util.Iterator<String> values) {
        List<String> result = new ArrayList<>();
        values.forEachRemaining(result::add);
        return result;
    }
}
