package ru.kuznetsov.qagraph.change.root;

import java.util.List;
import java.util.Objects;

/**
 * Failed Base root extraction with no partial evidence.
 */
public record CanonicalBaseEvidenceExtractionFailure(
        List<BaseEvidenceExtractionDiagnostic> diagnostics
) implements CanonicalBaseEvidenceExtractionResult {

    public CanonicalBaseEvidenceExtractionFailure {
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (diagnostics.isEmpty()
                || diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "diagnostics must contain at least one non-null value"
            );
        }
        diagnostics = diagnostics.stream()
                .sorted(BaseEvidenceExtractionDiagnostic.ORDER)
                .toList();
    }
}
