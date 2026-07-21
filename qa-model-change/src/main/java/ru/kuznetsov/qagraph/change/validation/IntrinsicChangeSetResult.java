package ru.kuznetsov.qagraph.change.validation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable Phase 3 outcomes for a declared change set.
 */
public record IntrinsicChangeSetResult(
        List<IntrinsicallyValidChange> validCandidates,
        List<IntrinsicallyInvalidChange> failedDeclarations,
        List<ChangeSetAmbiguity> ambiguities
) {

    public IntrinsicChangeSetResult {
        validCandidates = copy(validCandidates, "validCandidates").stream()
                .sorted(Comparator.comparingInt(
                        IntrinsicallyValidChange::declarationIndex))
                .toList();
        failedDeclarations = copy(
                failedDeclarations,
                "failedDeclarations"
        ).stream().sorted(Comparator
                .comparingInt(IntrinsicallyInvalidChange::declarationIndex)
                .thenComparingInt(value ->
                        value.classification().precedence()))
                .toList();
        ambiguities = copy(ambiguities, "ambiguities").stream()
                .sorted(Comparator.comparingInt(value ->
                        value.declarationIndices().getFirst()))
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
