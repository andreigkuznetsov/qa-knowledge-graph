package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.List;
import java.util.Objects;

/**
 * One ambiguous target declared more than once in a change set.
 */
public record ChangeSetAmbiguity(
        ArtifactCategory category,
        CanonicalIdentity identity,
        List<Integer> declarationIndices,
        ChangeDiagnostic diagnostic
) {

    public ChangeSetAmbiguity {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(
                declarationIndices,
                "declarationIndices must not be null"
        );
        if (declarationIndices.size() < 2
                || declarationIndices.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "ambiguity requires at least two declaration indices"
            );
        }
        declarationIndices = List.copyOf(declarationIndices);
        Objects.requireNonNull(diagnostic, "diagnostic must not be null");
        if (diagnostic.classification()
                != ChangeFailureClassification.AMBIGUOUS) {
            throw new IllegalArgumentException(
                    "ambiguity diagnostic must be AMBIGUOUS"
            );
        }
    }
}
