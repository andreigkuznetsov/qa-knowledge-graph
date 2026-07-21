package ru.kuznetsov.qagraph.change.aggregate;

import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;

import java.util.List;
import java.util.Objects;

/**
 * Materialized model with aggregate-transition diagnostics and no success.
 */
public record AggregateTransitionInvalid(
        ProposedModelMaterialized materialization,
        List<AggregateTransitionDiagnostic> diagnostics
) implements AggregateTransitionValidationResult {

    public AggregateTransitionInvalid {
        Objects.requireNonNull(
                materialization,
                "materialization must not be null"
        );
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty()
                || diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must contain at least one non-null value"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(AggregateTransitionDiagnostic.ORDER)
                .toList();
    }
}
