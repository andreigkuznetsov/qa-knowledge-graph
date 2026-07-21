package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.model.DeclaredChange;

/**
 * Result of validating one declaration without a Base Model.
 */
public sealed interface IntrinsicChangeResult
        permits IntrinsicallyValidChange, IntrinsicallyInvalidChange {

    int declarationIndex();

    DeclaredChange declaration();
}
