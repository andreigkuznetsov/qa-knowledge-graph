package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;
import java.util.Objects;
import java.util.Optional;

/** One intrinsic candidate accepted by its owning Base verification run. */
public final class BaseVerifiedChange implements BaseVerificationResult {
    private final IntrinsicallyValidChange candidate;
    private final Optional<ArtifactState> matchedBaseState;
    BaseVerifiedChange(IntrinsicallyValidChange candidate, Optional<ArtifactState> state) {
        this.candidate = Objects.requireNonNull(candidate);
        this.matchedBaseState = Objects.requireNonNull(state);
        boolean added = candidate.declaration().kind() == ChangeKind.ADDED;
        if (added == state.isPresent()) throw new IllegalArgumentException("ADDED has no matched Base state; other kinds require one");
    }
    public IntrinsicallyValidChange candidate() { return candidate; }
    public Optional<ArtifactState> matchedBaseState() { return matchedBaseState; }
    @Override public boolean equals(Object o) { return o instanceof BaseVerifiedChange that && candidate.equals(that.candidate) && matchedBaseState.equals(that.matchedBaseState); }
    @Override public int hashCode() { return Objects.hash(candidate, matchedBaseState); }
}
