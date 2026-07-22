package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import ru.kuznetsov.qagraph.change.validation.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Verifier-owned immutable Phase 3/Base Change Set outcome. */
public final class BaseChangeSetResult {
    private final BaseArtifactIndex baseIndex;
    private final CanonicalBaseModelEvidence baseEvidence;
    private final IntrinsicChangeSetResult intrinsicResult;
    private final List<IntrinsicallyInvalidChange> intrinsicFailures;
    private final List<ChangeSetAmbiguity> ambiguities;
    private final List<BaseVerifiedChange> baseVerifiedCandidates;
    private final List<BaseVerificationFailure> baseFailures;

    BaseChangeSetResult(BaseArtifactIndex index, CanonicalBaseModelEvidence evidence,
                        IntrinsicChangeSetResult intrinsic,
                        List<IntrinsicallyInvalidChange> intrinsicFailures,
                        List<ChangeSetAmbiguity> ambiguities,
                        List<BaseVerifiedChange> verified,
                        List<BaseVerificationFailure> failures) {
        baseIndex = Objects.requireNonNull(index);
        baseEvidence = Objects.requireNonNull(evidence);
        intrinsicResult = Objects.requireNonNull(intrinsic);
        if (evidence.artifactIndex() != index) throw new IllegalArgumentException("baseEvidence must own exact baseIndex");
        this.intrinsicFailures = copy(intrinsicFailures).stream().sorted(Comparator.comparingInt(IntrinsicallyInvalidChange::declarationIndex)).toList();
        this.ambiguities = copy(ambiguities).stream().sorted(Comparator.comparingInt(value -> value.declarationIndices().getFirst())).toList();
        baseVerifiedCandidates = copy(verified).stream().sorted(Comparator.comparingInt(value -> value.candidate().declarationIndex())).toList();
        baseFailures = copy(failures).stream().sorted(Comparator.comparingInt((BaseVerificationFailure value) -> value.candidate().declarationIndex()).thenComparingInt(value -> value.classification().precedence())).toList();
    }
    public BaseArtifactIndex baseIndex() { return baseIndex; }
    public CanonicalBaseModelEvidence baseEvidence() { return baseEvidence; }
    public IntrinsicChangeSetResult intrinsicResult() { return intrinsicResult; }
    public List<IntrinsicallyInvalidChange> intrinsicFailures() { return intrinsicFailures; }
    public List<ChangeSetAmbiguity> ambiguities() { return ambiguities; }
    public List<BaseVerifiedChange> baseVerifiedCandidates() { return baseVerifiedCandidates; }
    public List<BaseVerificationFailure> baseFailures() { return baseFailures; }
    private static <T> List<T> copy(List<T> values) { Objects.requireNonNull(values); if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("list contains null"); return List.copyOf(values); }
    @Override public boolean equals(Object o) { return o instanceof BaseChangeSetResult that && baseIndex == that.baseIndex && baseEvidence == that.baseEvidence && intrinsicResult.equals(that.intrinsicResult) && intrinsicFailures.equals(that.intrinsicFailures) && ambiguities.equals(that.ambiguities) && baseVerifiedCandidates.equals(that.baseVerifiedCandidates) && baseFailures.equals(that.baseFailures); }
    @Override public int hashCode() { return Objects.hash(System.identityHashCode(baseIndex), System.identityHashCode(baseEvidence), intrinsicResult, intrinsicFailures, ambiguities, baseVerifiedCandidates, baseFailures); }
}
