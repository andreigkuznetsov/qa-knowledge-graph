package ru.kuznetsov.qagraph.change.verification;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.complete.CompleteProposedRootInvalid;
import ru.kuznetsov.qagraph.change.complete.CompleteProposedRootValid;
import ru.kuznetsov.qagraph.change.complete.CompleteProposedRootValidationResult;
import ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification;
import ru.kuznetsov.qagraph.change.complete.CompleteValidationDiagnostic;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Stateless Phase 9 boundary. It never re-runs validation. */
public final class FinalChangeSetVerifier {
    private static final String INCONSISTENT =
            "FINAL_VERIFICATION_EVIDENCE_INCONSISTENT";

    public FinalChangeSetVerificationResult verify(
            CompleteProposedRootValidationResult result
    ) {
        Objects.requireNonNull(result, "result must not be null");
        if (result instanceof CompleteProposedRootInvalid invalid) {
            return rejectInvalid(invalid);
        }
        CompleteProposedRootValid valid = (CompleteProposedRootValid) result;
        ProposedRootReconstructed root = valid.reconstructedRoot();
        ProposedModelMaterialized materialization = root.aggregateTransition()
                .materialization();
        BaseChangeSetResult base = materialization.sourceResult();
        Optional<IntrinsicChangeSetResult> intrinsicOptional = base.intrinsicResult();
        if (intrinsicOptional.isEmpty()) {
            return inconsistent(result, "Intrinsic source evidence is missing");
        }
        IntrinsicChangeSetResult intrinsic = intrinsicOptional.orElseThrow();
        Optional<DeclaredChangeSet> declaredOptional = intrinsic.declaredChangeSet();
        if (declaredOptional.isEmpty()) {
            return inconsistent(result, "Declared Change Set evidence is missing");
        }
        if (!consistent(valid, declaredOptional.orElseThrow(), intrinsic, base)) {
            return inconsistent(result, "Retained success evidence is incoherent");
        }
        return new VerifiedChangeSet(
                valid,
                declaredOptional.orElseThrow(),
                intrinsic,
                base
        );
    }

    private boolean consistent(
            CompleteProposedRootValid valid,
            DeclaredChangeSet declared,
            IntrinsicChangeSetResult intrinsic,
            BaseChangeSetResult base
    ) {
        ProposedRootReconstructed root = valid.reconstructedRoot();
        ProposedModelMaterialized materialized = root.aggregateTransition()
                .materialization();
        ProposedArtifactModel model = materialized.proposedModel();
        boolean declarationsBound = intrinsic.validCandidates().size()
                == declared.changes().size()
                && intrinsic.validCandidates().stream().allMatch(candidate ->
                candidate.declarationIndex() < declared.changes().size()
                        && candidate.declaration() == declared.changes().get(
                        candidate.declarationIndex()))
                && base.baseVerifiedCandidates().size()
                == intrinsic.validCandidates().size();
        boolean evidenceBound = base.baseEvidence().filter(value ->
                value == root.baseEvidence()).isPresent()
                && base.baseIndex() == root.baseEvidence().artifactIndex()
                && materialized.baseEvidence() == root.baseEvidence();
        JsonNode snapshot = root.proposedRoot().snapshot();
        boolean rootMatches = snapshot.path("nodes").equals(
                artifacts(model.nodes()))
                && snapshot.path("relationships").equals(
                artifacts(model.relationships()));
        String version = model.schemaVersion().value();
        boolean versions = "0.1".equals(version)
                && version.equals(root.baseEvidence().rootContext()
                .schemaVersion().value())
                && version.equals(root.baseEvidence().artifactIndex()
                .schemaVersion().value())
                && snapshot.path("schemaVersion").asText().equals(version)
                && declared.changes().stream().allMatch(value ->
                value.schemaVersion().value().equals(version));
        boolean validation = valid.schemaEvidence().valid()
                && valid.schemaEvidence().authoritativeIssues().isEmpty()
                && valid.semanticEvidence().valid()
                && valid.semanticEvidence().authoritativeIssues().stream()
                .noneMatch(value -> value.severity() == ValidationSeverity.ERROR)
                && valid.semanticEvidence().diagnostics().stream()
                .noneMatch(value -> value.severity() == ValidationSeverity.ERROR);
        return declarationsBound && evidenceBound && rootMatches
                && versions && validation;
    }

    private JsonNode artifacts(List<? extends ru.kuznetsov.qagraph.change.model.ArtifactState> values) {
        var array = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        values.forEach(value -> array.add(value.snapshot()));
        return array;
    }

    private RejectedChangeSet rejectInvalid(CompleteProposedRootInvalid invalid) {
        FinalVerificationStage stage = switch (invalid.classification()) {
            case SCHEMA_INVALID -> FinalVerificationStage.SCHEMA_VALIDATION;
            case SEMANTICALLY_INVALID -> FinalVerificationStage.SEMANTIC_VALIDATION;
            case VALIDATION_INFRASTRUCTURE_FAILURE ->
                    FinalVerificationStage.VALIDATION_INFRASTRUCTURE;
            case UNSUPPORTED_VERSION -> FinalVerificationStage.UNSUPPORTED_VERSION;
        };
        List<FinalVerificationDiagnosticView> views = invalid.diagnostics()
                .stream().map(value -> view(stage, value)).toList();
        return new RejectedChangeSet(invalid, stage, views);
    }

    private FinalVerificationDiagnosticView view(
            FinalVerificationStage stage,
            CompleteValidationDiagnostic diagnostic
    ) {
        return new FinalVerificationDiagnosticView(
                stage, diagnostic.code(), diagnostic.path(),
                diagnostic.message(), Optional.of(diagnostic));
    }

    private RejectedChangeSet inconsistent(
            CompleteProposedRootValidationResult result,
            String message
    ) {
        return stage(result, FinalVerificationStage.FINAL_EVIDENCE_CONSISTENCY,
                message);
    }

    private RejectedChangeSet stage(
            CompleteProposedRootValidationResult result,
            FinalVerificationStage stage,
            String message
    ) {
        return new RejectedChangeSet(result, stage, List.of(
                new FinalVerificationDiagnosticView(
                        stage, INCONSISTENT, "$", message, Optional.empty())
        ));
    }
}
