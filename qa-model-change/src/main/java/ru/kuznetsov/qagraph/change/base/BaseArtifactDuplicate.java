package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.Comparator;
import java.util.Objects;

/**
 * Deterministic duplicate-target evidence from a Base artifact index.
 */
public record BaseArtifactDuplicate(
        ArtifactCategory category,
        CanonicalIdentity identity,
        int occurrenceCount
) {

    public static final Comparator<BaseArtifactDuplicate> ORDER = Comparator
            .comparingInt((BaseArtifactDuplicate value) ->
                    categoryRank(value.category()))
            .thenComparing(value -> value.identity().value());

    public BaseArtifactDuplicate {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        if (occurrenceCount < 2) {
            throw new IllegalArgumentException(
                    "occurrenceCount must be at least two"
            );
        }
    }

    private static int categoryRank(ArtifactCategory category) {
        return switch (category) {
            case NODE -> 0;
            case RELATIONSHIP -> 1;
        };
    }
}
