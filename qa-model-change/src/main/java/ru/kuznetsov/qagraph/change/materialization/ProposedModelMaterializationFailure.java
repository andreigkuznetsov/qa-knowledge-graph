package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;

import java.util.List;
import java.util.Objects;

/**
 * All-or-nothing materialization failure with original Phase 3/4 outcomes.
 */
public record ProposedModelMaterializationFailure(
        BaseChangeSetResult sourceResult,
        MaterializationFailureKind failureKind,
        List<MaterializationDiagnostic> diagnostics
) implements ProposedModelMaterializationResult {

    public ProposedModelMaterializationFailure {
        Objects.requireNonNull(sourceResult, "sourceResult must not be null");
        Objects.requireNonNull(failureKind, "failureKind must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty()
                || diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must contain at least one non-null value"
            );
        }
        if (diagnostics.stream().anyMatch(value ->
                value.failureKind() != failureKind)) {
            throw new IllegalArgumentException(
                    "diagnostics must match the failure kind"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(MaterializationDiagnostic.ORDER)
                .toList();
    }
}
