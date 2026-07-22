package ru.kuznetsov.qagraph.change.verification;

import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.complete.*;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import ru.kuznetsov.qagraph.change.root.ProposedCanonicalRoot;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.List;
import java.util.Objects;

/** Final verified evidence. Construction is owned by {@link FinalChangeSetVerifier}. */
public final class VerifiedChangeSet implements FinalChangeSetVerificationResult {
    private final CompleteProposedRootValid completeValidation;
    private final DeclaredChangeSet declaredChangeSet;
    private final IntrinsicChangeSetResult intrinsicResult;
    private final BaseChangeSetResult baseResult;

    VerifiedChangeSet(CompleteProposedRootValid completeValidation,
                      DeclaredChangeSet declaredChangeSet,
                      IntrinsicChangeSetResult intrinsicResult,
                      BaseChangeSetResult baseResult) {
        this.completeValidation = Objects.requireNonNull(completeValidation);
        this.declaredChangeSet = Objects.requireNonNull(declaredChangeSet);
        this.intrinsicResult = Objects.requireNonNull(intrinsicResult);
        this.baseResult = Objects.requireNonNull(baseResult);
    }

    public CompleteProposedRootValid completeValidation() { return completeValidation; }
    public DeclaredChangeSet declaredChangeSet() { return declaredChangeSet; }
    public IntrinsicChangeSetResult intrinsicResult() { return intrinsicResult; }
    public BaseChangeSetResult baseResult() { return baseResult; }
    public CanonicalBaseModelEvidence baseEvidence() { return completeValidation.reconstructedRoot().baseEvidence(); }
    public ProposedArtifactModel proposedModel() { return completeValidation.reconstructedRoot().aggregateTransition().materialization().proposedModel(); }
    public ProposedCanonicalRoot proposedRoot() { return completeValidation.reconstructedRoot().proposedRoot(); }
    public SchemaValidationEvidence schemaEvidence() { return completeValidation.schemaEvidence(); }
    public SemanticValidationEvidence semanticEvidence() { return completeValidation.semanticEvidence(); }
    public List<CompleteValidationDiagnostic> warnings() { return semanticEvidence().diagnostics().stream().filter(value -> value.severity() == ValidationSeverity.WARNING).toList(); }
    public boolean hasWarnings() { return !warnings().isEmpty(); }
    public CanonicalQaModelVersion schemaVersion() { return proposedModel().schemaVersion(); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VerifiedChangeSet that)) return false;
        return completeValidation.equals(that.completeValidation)
                && declaredChangeSet.equals(that.declaredChangeSet)
                && intrinsicResult.equals(that.intrinsicResult)
                && baseResult.equals(that.baseResult);
    }
    @Override public int hashCode() { return Objects.hash(completeValidation, declaredChangeSet, intrinsicResult, baseResult); }
}
