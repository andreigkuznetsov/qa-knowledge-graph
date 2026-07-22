package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;

/**
 * Result of checking one intrinsic candidate against Base Model evidence.
 */
public sealed interface BaseVerificationResult
        permits BaseVerifiedChange, BaseVerificationFailure {

    IntrinsicallyValidChange candidate();
}
