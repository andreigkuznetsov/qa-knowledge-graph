package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import java.util.Objects;

/** One declaration accepted by the owning intrinsic validator run. */
public final class IntrinsicallyValidChange implements IntrinsicChangeResult {
    private final int declarationIndex;
    private final DeclaredChange declaration;
    IntrinsicallyValidChange(int index, DeclaredChange declaration) {
        if (index < 0) throw new IllegalArgumentException("declarationIndex must not be negative");
        this.declarationIndex = index;
        this.declaration = Objects.requireNonNull(declaration);
    }
    public int declarationIndex() { return declarationIndex; }
    public DeclaredChange declaration() { return declaration; }
    @Override public boolean equals(Object o) { return o instanceof IntrinsicallyValidChange that && declarationIndex == that.declarationIndex && declaration.equals(that.declaration); }
    @Override public int hashCode() { return Objects.hash(declarationIndex, declaration); }
}
