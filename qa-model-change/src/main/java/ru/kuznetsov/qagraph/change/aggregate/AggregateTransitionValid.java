package ru.kuznetsov.qagraph.change.aggregate;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import java.util.Objects;
/** Aggregate success created only by {@link AggregateTransitionValidator}. */
public final class AggregateTransitionValid implements AggregateTransitionValidationResult {
    private final ProposedModelMaterialized materialization;
    AggregateTransitionValid(ProposedModelMaterialized value) { materialization = Objects.requireNonNull(value); }
    public ProposedModelMaterialized materialization() { return materialization; }
    @Override public boolean equals(Object o) { return o instanceof AggregateTransitionValid that && materialization.equals(that.materialization); }
    @Override public int hashCode() { return materialization.hashCode(); }
}
