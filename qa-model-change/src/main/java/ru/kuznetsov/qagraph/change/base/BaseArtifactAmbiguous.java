package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactState;

import java.util.List;
import java.util.Objects;

/**
 * Base lookup evidence for a duplicate logical target.
 */
public record BaseArtifactAmbiguous(List<ArtifactState> states)
        implements BaseArtifactLookup {

    public BaseArtifactAmbiguous {
        Objects.requireNonNull(states, "states must not be null");
        if (states.size() < 2 || states.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "ambiguous lookup requires at least two states"
            );
        }
        states = List.copyOf(states);
    }
}
