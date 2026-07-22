package ru.kuznetsov.qagraph.change.aggregate;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
public final class AggregateTestFixtures {
    private AggregateTestFixtures() { }
    public static AggregateTransitionValid valid(ProposedModelMaterialized value) { return new AggregateTransitionValid(value); }
}
