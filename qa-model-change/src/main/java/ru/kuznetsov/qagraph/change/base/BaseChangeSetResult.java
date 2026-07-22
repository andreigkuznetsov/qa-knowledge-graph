package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.validation.ChangeSetAmbiguity;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyInvalidChange;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured immutable Phase 3 and Base verification outcomes.
 */
public record BaseChangeSetResult(
        BaseArtifactIndex baseIndex,
        Optional<CanonicalBaseModelEvidence> baseEvidence,
        Optional<IntrinsicChangeSetResult> intrinsicResult,
        List<IntrinsicallyInvalidChange> intrinsicFailures,
        List<ChangeSetAmbiguity> ambiguities,
        List<BaseVerifiedChange> baseVerifiedCandidates,
        List<BaseVerificationFailure> baseFailures
) {

    public BaseChangeSetResult {
        Objects.requireNonNull(baseIndex, "baseIndex must not be null");
        Objects.requireNonNull(baseEvidence, "baseEvidence must not be null");
        Objects.requireNonNull(
                intrinsicResult,
                "intrinsicResult must not be null"
        );
        if (baseEvidence.isPresent()
                && baseEvidence.orElseThrow().artifactIndex() != baseIndex) {
            throw new IllegalArgumentException(
                    "baseEvidence must own the exact baseIndex"
            );
        }
        intrinsicFailures = copy(
                intrinsicFailures,
                "intrinsicFailures"
        ).stream().sorted(Comparator.comparingInt(
                IntrinsicallyInvalidChange::declarationIndex)).toList();
        ambiguities = copy(ambiguities, "ambiguities").stream()
                .sorted(Comparator.comparingInt(value ->
                        value.declarationIndices().getFirst()))
                .toList();
        baseVerifiedCandidates = copy(
                baseVerifiedCandidates,
                "baseVerifiedCandidates"
        ).stream().sorted(Comparator.comparingInt(value ->
                value.candidate().declarationIndex())).toList();
        baseFailures = copy(baseFailures, "baseFailures").stream()
                .sorted(Comparator
                        .comparingInt((BaseVerificationFailure value) ->
                                value.candidate().declarationIndex())
                        .thenComparingInt(value ->
                                value.classification().precedence()))
                .toList();
    }

    public BaseChangeSetResult(
            BaseArtifactIndex baseIndex,
            Optional<CanonicalBaseModelEvidence> baseEvidence,
            List<IntrinsicallyInvalidChange> intrinsicFailures,
            List<ChangeSetAmbiguity> ambiguities,
            List<BaseVerifiedChange> baseVerifiedCandidates,
            List<BaseVerificationFailure> baseFailures
    ) {
        this(
                baseIndex,
                baseEvidence,
                Optional.empty(),
                intrinsicFailures,
                ambiguities,
                baseVerifiedCandidates,
                baseFailures
        );
    }

    public BaseChangeSetResult(
            BaseArtifactIndex baseIndex,
            List<IntrinsicallyInvalidChange> intrinsicFailures,
            List<ChangeSetAmbiguity> ambiguities,
            List<BaseVerifiedChange> baseVerifiedCandidates,
            List<BaseVerificationFailure> baseFailures
    ) {
        this(
                baseIndex,
                Optional.empty(),
                Optional.empty(),
                intrinsicFailures,
                ambiguities,
                baseVerifiedCandidates,
                baseFailures
        );
    }

    private static <T> List<T> copy(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    name + " must not contain null members"
            );
        }
        return List.copyOf(values);
    }
}
