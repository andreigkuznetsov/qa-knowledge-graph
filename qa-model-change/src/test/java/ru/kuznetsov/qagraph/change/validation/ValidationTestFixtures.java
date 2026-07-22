package ru.kuznetsov.qagraph.change.validation;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import java.util.List;
public final class ValidationTestFixtures {
    private ValidationTestFixtures() { }
    public static IntrinsicallyValidChange candidate(int index, DeclaredChange declaration) { return new IntrinsicallyValidChange(index, declaration); }
    public static IntrinsicChangeSetResult result(List<IntrinsicallyValidChange> candidates) {
        return new IntrinsicChangeSetResult(new DeclaredChangeSet(candidates.stream().map(IntrinsicallyValidChange::declaration).toList()), candidates, List.of(), List.of());
    }
}
