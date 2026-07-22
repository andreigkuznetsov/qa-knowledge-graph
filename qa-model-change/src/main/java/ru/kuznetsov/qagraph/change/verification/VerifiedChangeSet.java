package ru.kuznetsov.qagraph.change.verification;

import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.complete.CompleteProposedRootValid;
import ru.kuznetsov.qagraph.change.complete.CompleteValidationDiagnostic;
import ru.kuznetsov.qagraph.change.complete.SchemaValidationEvidence;
import ru.kuznetsov.qagraph.change.complete.SemanticValidationEvidence;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import ru.kuznetsov.qagraph.change.root.ProposedCanonicalRoot;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.List;
import java.util.Objects;

public record VerifiedChangeSet(
        CompleteProposedRootValid completeValidation,
        DeclaredChangeSet declaredChangeSet,
        IntrinsicChangeSetResult intrinsicResult,
        BaseChangeSetResult baseResult
) implements FinalChangeSetVerificationResult {
    public VerifiedChangeSet {
        Objects.requireNonNull(completeValidation, "completeValidation required");
        Objects.requireNonNull(declaredChangeSet, "declaredChangeSet required");
        Objects.requireNonNull(intrinsicResult, "intrinsicResult required");
        Objects.requireNonNull(baseResult, "baseResult required");
    }

    public CanonicalBaseModelEvidence baseEvidence() {
        return completeValidation.reconstructedRoot().baseEvidence();
    }

    public ProposedArtifactModel proposedModel() {
        return completeValidation.reconstructedRoot().aggregateTransition()
                .materialization().proposedModel();
    }

    public ProposedCanonicalRoot proposedRoot() {
        return completeValidation.reconstructedRoot().proposedRoot();
    }

    public SchemaValidationEvidence schemaEvidence() {
        return completeValidation.schemaEvidence();
    }

    public SemanticValidationEvidence semanticEvidence() {
        return completeValidation.semanticEvidence();
    }

    public List<CompleteValidationDiagnostic> warnings() {
        return semanticEvidence().diagnostics().stream()
                .filter(value -> value.severity() == ValidationSeverity.WARNING)
                .toList();
    }

    public boolean hasWarnings() {
        return !warnings().isEmpty();
    }

    public CanonicalQaModelVersion schemaVersion() {
        return proposedModel().schemaVersion();
    }
}
