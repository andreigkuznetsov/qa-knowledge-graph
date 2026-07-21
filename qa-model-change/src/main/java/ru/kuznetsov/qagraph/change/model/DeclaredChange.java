package ru.kuznetsov.qagraph.change.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Untrusted declaration of one proposed atomic change.
 */
public record DeclaredChange(
        ArtifactCategory category,
        CanonicalIdentity identity,
        ChangeKind kind,
        CanonicalQaModelVersion schemaVersion,
        Optional<ArtifactState> beforeState,
        Optional<ArtifactState> afterState
) {

    public DeclaredChange {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
        Objects.requireNonNull(beforeState, "beforeState must not be null");
        Objects.requireNonNull(afterState, "afterState must not be null");
    }
}
