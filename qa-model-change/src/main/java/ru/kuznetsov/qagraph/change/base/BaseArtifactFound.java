package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactState;

import java.util.Objects;

/**
 * Base lookup evidence containing one unambiguous immutable artifact state.
 */
public record BaseArtifactFound(ArtifactState state)
        implements BaseArtifactLookup {

    public BaseArtifactFound {
        Objects.requireNonNull(state, "state must not be null");
    }
}
