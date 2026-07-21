package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable deterministic Proposed Model materialization diagnostic.
 */
public record MaterializationDiagnostic(
        MaterializationDiagnosticCode code,
        MaterializationFailureKind failureKind,
        int declarationIndex,
        Optional<ArtifactCategory> category,
        Optional<CanonicalIdentity> identity,
        String path,
        String message
) {

    public static final Comparator<MaterializationDiagnostic> ORDER =
            Comparator
                    .comparingInt((MaterializationDiagnostic value) ->
                            value.failureKind().priority())
                    .thenComparingInt(value -> value.code().priority())
                    .thenComparing(MaterializationDiagnostic::path)
                    .thenComparing(value -> value.code().name());

    public MaterializationDiagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(failureKind, "failureKind must not be null");
        if (declarationIndex < -1) {
            throw new IllegalArgumentException(
                    "declarationIndex must be -1 or non-negative"
            );
        }
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        if (category.isPresent() != identity.isPresent()) {
            throw new IllegalArgumentException(
                    "category and identity must be present together"
            );
        }
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (path.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException(
                    "path and message must not be blank"
            );
        }
    }
}
