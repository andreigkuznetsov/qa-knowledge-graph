package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChange;

import java.util.List;
import java.util.Objects;

/**
 * Declaration that failed with one primary classification.
 */
public record IntrinsicallyInvalidChange(
        int declarationIndex,
        DeclaredChange declaration,
        ChangeFailureClassification classification,
        List<ChangeDiagnostic> diagnostics
) implements IntrinsicChangeResult {

    public IntrinsicallyInvalidChange {
        if (declarationIndex < 0) {
            throw new IllegalArgumentException(
                    "declarationIndex must not be negative"
            );
        }
        Objects.requireNonNull(declaration, "declaration must not be null");
        Objects.requireNonNull(
                classification,
                "classification must not be null"
        );
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "diagnostics must not be empty"
            );
        }
        if (diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must not contain null members"
            );
        }
        if (diagnostics.stream().anyMatch(value ->
                value.classification() != classification
                        || value.declarationIndex() != declarationIndex)) {
            throw new IllegalArgumentException(
                    "diagnostics must match the result classification and index"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(ChangeDiagnostic.ORDER)
                .toList();
    }
}
