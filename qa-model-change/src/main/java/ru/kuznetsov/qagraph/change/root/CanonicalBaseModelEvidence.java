package ru.kuznetsov.qagraph.change.root;

import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;

import java.util.Objects;

/**
 * Bound immutable Base root context and artifact index from one extraction.
 */
public final class CanonicalBaseModelEvidence {

    private final CanonicalRootContext rootContext;
    private final BaseArtifactIndex artifactIndex;

    CanonicalBaseModelEvidence(
            CanonicalRootContext rootContext,
            BaseArtifactIndex artifactIndex
    ) {
        this.rootContext = Objects.requireNonNull(
                rootContext,
                "rootContext must not be null"
        );
        this.artifactIndex = Objects.requireNonNull(
                artifactIndex,
                "artifactIndex must not be null"
        );
        if (!rootContext.schemaVersion().equals(
                artifactIndex.schemaVersion())) {
            throw new IllegalArgumentException(
                    "root context and artifact index versions must match"
            );
        }
    }

    public CanonicalRootContext rootContext() {
        return rootContext;
    }

    public BaseArtifactIndex artifactIndex() {
        return artifactIndex;
    }
}
