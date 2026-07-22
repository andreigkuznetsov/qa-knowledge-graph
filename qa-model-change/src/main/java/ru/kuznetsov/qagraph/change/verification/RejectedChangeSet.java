package ru.kuznetsov.qagraph.change.verification;

import ru.kuznetsov.qagraph.change.complete.CompleteProposedRootValidationResult;

import java.util.List;
import java.util.Objects;

public record RejectedChangeSet(
        CompleteProposedRootValidationResult completeValidation,
        FinalVerificationStage stage,
        List<FinalVerificationDiagnosticView> diagnostics
) implements FinalChangeSetVerificationResult {
    public RejectedChangeSet {
        Objects.requireNonNull(completeValidation, "completeValidation required");
        Objects.requireNonNull(stage, "stage required");
        Objects.requireNonNull(diagnostics, "diagnostics required");
        diagnostics = List.copyOf(diagnostics);
    }
}
