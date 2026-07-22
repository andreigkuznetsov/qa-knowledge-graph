package ru.kuznetsov.qagraph.change.root;

import java.util.List;
import java.util.Objects;

public record ProposedRootReconstructionFailure(
        RootReconstructionFailureKind failureKind,
        List<RootReconstructionDiagnostic> diagnostics
) implements ProposedRootReconstructionResult {
    public ProposedRootReconstructionFailure {
        Objects.requireNonNull(failureKind, "failureKind must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty() || diagnostics.stream().anyMatch(
                Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must contain non-null members"
            );
        }
        if (diagnostics.stream().anyMatch(value ->
                value.failureKind() != failureKind)) {
            throw new IllegalArgumentException(
                    "diagnostics must match failureKind"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(RootReconstructionDiagnostic.ORDER)
                .toList();
    }
}
