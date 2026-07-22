package ru.kuznetsov.qagraph.change.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValidator;
import ru.kuznetsov.qagraph.change.base.BaseChangeVerifier;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.base.BaseVerifiedChange;
import ru.kuznetsov.qagraph.change.complete.*;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterializer;
import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.root.*;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.lang.reflect.Modifier;
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
                .declaredChangeSet());
        assertSame(first.baseEvidence(), first.baseResult()
                .baseEvidence());
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
        VerifiedChangeSet result = assertInstanceOf(
                VerifiedChangeSet.class, verifier.verify(completeValid()));
        assertTrue(result.hasWarnings());
        CompleteValidationDiagnostic warning = result.warnings().getFirst();
        assertEquals(warning.authoritativeIssue().code(), warning.code());
        assertEquals(warning.authoritativeIssue().severity(), warning.severity());
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
    void successTypesShouldExposeNoPublicConstructionPath() {
        assertTrue(List.of(VerifiedChangeSet.class,
                        IntrinsicallyValidChange.class,
                        IntrinsicChangeSetResult.class,
                        BaseVerifiedChange.class,
                        BaseChangeSetResult.class,
                        AggregateTransitionValid.class,
                        ProposedRootReconstructed.class,
                        CompleteProposedRootValid.class,
                        SchemaValidationEvidence.class,
                        SemanticValidationEvidence.class,
                        ProposedModelMaterialized.class).stream()
                .flatMap(type -> List.of(type.getDeclaredConstructors()).stream())
                .noneMatch(value -> Modifier.isPublic(value.getModifiers())));
        assertTrue(List.of(VerifiedChangeSet.class,
                        CompleteProposedRootValid.class,
                        SchemaValidationEvidence.class,
                        SemanticValidationEvidence.class,
                        ProposedModelMaterialized.class).stream()
                .flatMap(type -> List.of(type.getDeclaredMethods()).stream())
                .filter(value -> Modifier.isStatic(value.getModifiers()))
                .noneMatch(value -> Modifier.isPublic(value.getModifiers())
                        && FinalChangeSetVerificationResult.class
                        .isAssignableFrom(value.getReturnType())));
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
