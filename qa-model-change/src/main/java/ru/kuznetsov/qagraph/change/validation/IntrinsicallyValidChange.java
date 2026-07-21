package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChange;

import java.util.Objects;

/**
 * Declaration that passed Phase 3 checks; it is not a verified change.
 */
public record IntrinsicallyValidChange(
        int declarationIndex,
        DeclaredChange declaration
) implements IntrinsicChangeResult {

    public IntrinsicallyValidChange {
        if (declarationIndex < 0) {
            throw new IllegalArgumentException(
                    "declarationIndex must not be negative"
            );
        }
        Objects.requireNonNull(declaration, "declaration must not be null");
    }
}
