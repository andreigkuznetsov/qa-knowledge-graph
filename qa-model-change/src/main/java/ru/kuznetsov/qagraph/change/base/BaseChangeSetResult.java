package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.validation.ChangeSetAmbiguity;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyInvalidChange;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Structured immutable Phase 3 and Base verification outcomes.
 */
public record BaseChangeSetResult(
        List<IntrinsicallyInvalidChange> intrinsicFailures,
        List<ChangeSetAmbiguity> ambiguities,
        List<BaseVerifiedChange> baseVerifiedCandidates,
        List<BaseVerificationFailure> baseFailures
) {

    public BaseChangeSetResult {
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
