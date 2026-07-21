package ru.kuznetsov.qagraph.change.root;

import java.util.Comparator;
import java.util.Objects;

/**
 * Immutable deterministic Base root extraction diagnostic.
 */
public record BaseEvidenceExtractionDiagnostic(
        BaseEvidenceExtractionDiagnosticCode code,
        String path,
        String message
) {

    public static final Comparator<BaseEvidenceExtractionDiagnostic> ORDER =
            Comparator
                    .comparingInt((BaseEvidenceExtractionDiagnostic value) ->
                            value.code().priority())
                    .thenComparing(BaseEvidenceExtractionDiagnostic::path)
                    .thenComparing(value -> value.code().name());

    public BaseEvidenceExtractionDiagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (path.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException(
                    "path and message must not be blank"
            );
        }
    }
}
