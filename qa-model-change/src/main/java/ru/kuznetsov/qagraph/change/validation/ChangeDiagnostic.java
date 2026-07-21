package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.Comparator;
import java.util.Objects;

/**
 * Stable, immutable diagnostic for an untrusted change declaration.
 */
public record ChangeDiagnostic(
        ChangeDiagnosticCode code,
        ChangeFailureClassification classification,
        int declarationIndex,
        ArtifactCategory category,
        CanonicalIdentity identity,
        String path,
        String message
) {

    public static final Comparator<ChangeDiagnostic> ORDER = Comparator
            .comparingInt((ChangeDiagnostic value) ->
                    value.code().priority())
            .thenComparing(ChangeDiagnostic::path)
            .thenComparing(value -> value.code().name());

    public ChangeDiagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(
                classification,
                "classification must not be null"
        );
        if (declarationIndex < 0) {
            throw new IllegalArgumentException(
                    "declarationIndex must not be negative"
            );
        }
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
