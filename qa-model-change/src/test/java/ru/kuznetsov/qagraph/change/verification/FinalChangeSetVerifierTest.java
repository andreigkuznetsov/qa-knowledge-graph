package ru.kuznetsov.qagraph.change.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValidator;
import ru.kuznetsov.qagraph.change.base.BaseChangeVerifier;
import ru.kuznetsov.qagraph.change.complete.*;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterializer;
import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.root.*;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FinalChangeSetVerifierTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final FinalChangeSetVerifier verifier = new FinalChangeSetVerifier();

    @Test
    void completeValidChainShouldProduceImmutableIdempotentEvidence() throws Exception {
        CompleteProposedRootValid complete = completeValid();

        VerifiedChangeSet first = assertInstanceOf(
                VerifiedChangeSet.class, verifier.verify(complete));
        VerifiedChangeSet second = assertInstanceOf(
                VerifiedChangeSet.class, verifier.verify(complete));

        assertEquals(first, second);
        assertSame(first.declaredChangeSet(), first.intrinsicResult()
                .declaredChangeSet().orElseThrow());
        assertSame(first.baseEvidence(), first.baseResult()
                .baseEvidence().orElseThrow());
        assertEquals("0.1", first.schemaVersion().value());
        assertTrue(first.schemaEvidence().valid());
        assertTrue(first.semanticEvidence().valid());
        assertEquals(first.semanticEvidence().diagnostics(), first.warnings());
        assertTrue(first.warnings().stream().allMatch(value ->
                value.severity() == ValidationSeverity.WARNING));
        assertThrows(UnsupportedOperationException.class,
                () -> first.warnings().add(null));
        ObjectNode exposed = first.proposedRoot().snapshot();
        exposed.remove("project");
        assertTrue(first.proposedRoot().snapshot().has("project"));
    }

    @Test
    void warningsShouldRemainSuccessfulAndUnchanged() throws Exception {
        CompleteProposedRootValid clean = completeValid();
        ValidationIssue warning = ValidationIssue.semanticWarning(
                "WARN_CODE", "warning", "N-1", "/nodes/0");
        CompleteValidationDiagnostic diagnostic =
                new CompleteValidationDiagnostic(
                        CompleteValidationDiagnosticOrigin.SEMANTIC,
                        ValidationSeverity.WARNING,
                        warning.code(), warning.path(), warning.message(),
                        warning.objectId(), warning);
        CompleteProposedRootValid warned = new CompleteProposedRootValid(
                clean.reconstructedRoot(), clean.schemaEvidence(),
                new SemanticValidationEvidence(
                        List.of(warning), List.of(diagnostic)));

        VerifiedChangeSet result = assertInstanceOf(
                VerifiedChangeSet.class, verifier.verify(warned));
        assertTrue(result.hasWarnings());
        assertSame(diagnostic, result.warnings().getFirst());
        assertEquals("WARN_CODE", result.warnings().getFirst().code());
        assertEquals("/nodes/0", result.warnings().getFirst().path());
    }

    @Test
    void invalidCompleteEvidenceShouldPreserveActualStage() throws Exception {
        CompleteProposedRootValid clean = completeValid();
        CompleteProposedRootInvalid invalid = new CompleteProposedRootInvalid(
                clean.reconstructedRoot(),
                CompleteValidationClassification.UNSUPPORTED_VERSION,
                CompleteValidationStage.VERSION,
                Optional.empty(), Optional.empty(), List.of(), Optional.empty());

        RejectedChangeSet result = assertInstanceOf(
                RejectedChangeSet.class, verifier.verify(invalid));
        assertEquals(FinalVerificationStage.UNSUPPORTED_VERSION, result.stage());
        assertSame(invalid, result.completeValidation());
    }

    @Test
    void manuallyAssembledSuccessWithoutEarlierEvidenceShouldReject() throws Exception {
        CompleteProposedRootValid clean = completeValid();
        ProposedRootReconstructed root = clean.reconstructedRoot();
        AggregateTransitionValid forgedAggregate = new AggregateTransitionValid(
                new ProposedModelMaterialized(
                        root.aggregateTransition().materialization().proposedModel(),
                        Optional.of(root.baseEvidence())));
        CompleteProposedRootValid forged = new CompleteProposedRootValid(
                new ProposedRootReconstructed(
                        root.proposedRoot(), root.baseEvidence(), forgedAggregate),
                clean.schemaEvidence(), clean.semanticEvidence());

        RejectedChangeSet result = assertInstanceOf(
                RejectedChangeSet.class, verifier.verify(forged));
        assertEquals(FinalVerificationStage.FINAL_EVIDENCE_CONSISTENCY,
                result.stage());
        assertEquals("FINAL_VERIFICATION_EVIDENCE_INCONSISTENT",
                result.diagnostics().getFirst().code());
        assertEquals(result, verifier.verify(forged));
    }

    private CompleteProposedRootValid completeValid() throws Exception {
        ObjectNode baseRoot = (ObjectNode) mapper.readTree("""
                {"schemaVersion":"0.1",
                 "project":{"id":"P-1","name":"Project"},
                 "sources":[],"nodes":[],"relationships":[]}
                """);
        CanonicalBaseModelEvidence base = assertInstanceOf(
                CanonicalBaseEvidenceExtracted.class,
                new CanonicalBaseEvidenceExtractor().extract(baseRoot)
        ).evidence();
        ObjectNode nodeJson = (ObjectNode) mapper.readTree("""
                {"id":"N-1","type":"CHECK","name":"check",
                 "check":{"checkType":"SQL","assertion":"ok"}}
                """);
        NodeArtifactState node = new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, nodeJson);
        DeclaredChange declaration = new DeclaredChange(
                ArtifactCategory.NODE, node.identity(), ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1, Optional.empty(), Optional.of(node));
        DeclaredChangeSet set = new DeclaredChangeSet(List.of(declaration));
        var intrinsic = new IntrinsicChangeValidator().validate(set);
        var baseResult = new BaseChangeVerifier(base).verify(intrinsic);
        var materialized = assertInstanceOf(ProposedModelMaterialized.class,
                new ProposedModelMaterializer().materialize(
                        base.artifactIndex(), baseResult));
        var aggregate = assertInstanceOf(AggregateTransitionValid.class,
                new AggregateTransitionValidator().validate(materialized));
        var reconstructed = assertInstanceOf(ProposedRootReconstructed.class,
                new ProposedCanonicalRootReconstructor().reconstruct(
                        base, aggregate));
        return assertInstanceOf(CompleteProposedRootValid.class,
                new CompleteProposedRootValidator().validate(reconstructed));
    }
}
