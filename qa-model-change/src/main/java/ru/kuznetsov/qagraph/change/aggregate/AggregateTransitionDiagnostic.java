package ru.kuznetsov.qagraph.change.aggregate;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable deterministic aggregate-transition diagnostic.
 */
public record AggregateTransitionDiagnostic(
        AggregateTransitionDiagnosticCode code,
        AggregateTransitionFailureKind failureKind,
        Optional<ArtifactCategory> category,
        Optional<CanonicalIdentity> relationshipIdentity,
        RelationshipEndpointRole endpointRole,
        Optional<String> endpointValue,
        String path,
        String message
) {

    public static final Comparator<AggregateTransitionDiagnostic> ORDER =
            Comparator
                    .comparingInt((AggregateTransitionDiagnostic value) ->
                            value.failureKind().precedence())
                    .thenComparing(value -> value.relationshipIdentity()
                            .map(CanonicalIdentity::value).orElse(""))
                    .thenComparingInt(value -> value.endpointRole().rank())
                    .thenComparingInt(value -> value.code().priority())
                    .thenComparing(AggregateTransitionDiagnostic::path);

    public AggregateTransitionDiagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(failureKind, "failureKind must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(
                relationshipIdentity,
                "relationshipIdentity must not be null"
        );
        if (category.isPresent() != relationshipIdentity.isPresent()) {
            throw new IllegalArgumentException(
                    "category and relationship identity must appear together"
            );
        }
        Objects.requireNonNull(endpointRole, "endpointRole must not be null");
        Objects.requireNonNull(
                endpointValue,
                "endpointValue must not be null"
        );
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (path.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException(
                    "path and message must not be blank"
            );
        }
    }
}
