package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Validator-owned immutable outcome for one exact Declared Change Set. */
public final class IntrinsicChangeSetResult {
    private final DeclaredChangeSet declaredChangeSet;
    private final List<IntrinsicallyValidChange> validCandidates;
    private final List<IntrinsicallyInvalidChange> failedDeclarations;
    private final List<ChangeSetAmbiguity> ambiguities;

    IntrinsicChangeSetResult(DeclaredChangeSet source,
                             List<IntrinsicallyValidChange> valid,
                             List<IntrinsicallyInvalidChange> failed,
                             List<ChangeSetAmbiguity> ambiguities) {
        declaredChangeSet = Objects.requireNonNull(source);
        validCandidates = copy(valid, "validCandidates").stream().sorted(Comparator.comparingInt(IntrinsicallyValidChange::declarationIndex)).toList();
        failedDeclarations = copy(failed, "failedDeclarations").stream().sorted(Comparator.comparingInt(IntrinsicallyInvalidChange::declarationIndex).thenComparingInt(value -> value.classification().precedence())).toList();
        this.ambiguities = copy(ambiguities, "ambiguities").stream().sorted(Comparator.comparingInt(value -> value.declarationIndices().getFirst())).toList();
    }
    public DeclaredChangeSet declaredChangeSet() { return declaredChangeSet; }
    public List<IntrinsicallyValidChange> validCandidates() { return validCandidates; }
    public List<IntrinsicallyInvalidChange> failedDeclarations() { return failedDeclarations; }
    public List<ChangeSetAmbiguity> ambiguities() { return ambiguities; }
    private static <T> List<T> copy(List<T> values, String name) { Objects.requireNonNull(values, name); if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(name + " contains null"); return List.copyOf(values); }
    @Override public boolean equals(Object o) { return o instanceof IntrinsicChangeSetResult that && declaredChangeSet.equals(that.declaredChangeSet) && validCandidates.equals(that.validCandidates) && failedDeclarations.equals(that.failedDeclarations) && ambiguities.equals(that.ambiguities); }
    @Override public int hashCode() { return Objects.hash(declaredChangeSet, validCandidates, failedDeclarations, ambiguities); }
}
