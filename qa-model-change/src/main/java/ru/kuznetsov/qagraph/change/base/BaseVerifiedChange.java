package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;

import java.util.Objects;
import java.util.Optional;

/**
 * Candidate whose declaration truthfully describes current Base evidence.
 * This is not a final VerifiedChange.
 */
public record BaseVerifiedChange(
        IntrinsicallyValidChange candidate,
        Optional<ArtifactState> matchedBaseState
) implements BaseVerificationResult {

    public BaseVerifiedChange {
        Objects.requireNonNull(candidate, "candidate must not be null");
        Objects.requireNonNull(
                matchedBaseState,
                "matchedBaseState must not be null"
        );
        boolean added = candidate.declaration().kind() == ChangeKind.ADDED;
        if (added == matchedBaseState.isPresent()) {
            throw new IllegalArgumentException(
                    "ADDED requires absence evidence; other kinds require a match"
            );
        }
    }
}
