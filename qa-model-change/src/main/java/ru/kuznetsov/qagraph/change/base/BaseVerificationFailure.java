package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.validation.ChangeDiagnostic;
import ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;

import java.util.List;
import java.util.Objects;

/**
 * Unsupported or Base-unverifiable intrinsic candidate.
 */
public record BaseVerificationFailure(
        IntrinsicallyValidChange candidate,
        ChangeFailureClassification classification,
        List<ChangeDiagnostic> diagnostics
) implements BaseVerificationResult {

    public BaseVerificationFailure {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(
                classification,
                "classification must not be null"
        );
        if (classification != ChangeFailureClassification.UNSUPPORTED
                && classification
                != ChangeFailureClassification.UNVERIFIABLE) {
            throw new IllegalArgumentException(
                    "Base failure must be UNSUPPORTED or UNVERIFIABLE"
            );
        }
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty()
                || diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must contain at least one non-null value"
            );
        }
        int index = candidate.declarationIndex();
        if (diagnostics.stream().anyMatch(value ->
                value.classification() != classification
                        || value.declarationIndex() != index)) {
            throw new IllegalArgumentException(
                    "diagnostics must match failure classification and index"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(ChangeDiagnostic.ORDER)
                .toList();
    }
}
