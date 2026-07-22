package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable Phase 3 outcomes for a declared change set.
 */
public record IntrinsicChangeSetResult(
        Optional<DeclaredChangeSet> declaredChangeSet,
        List<IntrinsicallyValidChange> validCandidates,
        List<IntrinsicallyInvalidChange> failedDeclarations,
        List<ChangeSetAmbiguity> ambiguities
) {

    public IntrinsicChangeSetResult {
        Objects.requireNonNull(
                declaredChangeSet,
                "declaredChangeSet must not be null"
        );
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

    public IntrinsicChangeSetResult(
            List<IntrinsicallyValidChange> validCandidates,
            List<IntrinsicallyInvalidChange> failedDeclarations,
            List<ChangeSetAmbiguity> ambiguities
    ) {
        this(Optional.empty(), validCandidates, failedDeclarations, ambiguities);
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
