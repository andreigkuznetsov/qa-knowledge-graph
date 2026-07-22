package ru.kuznetsov.qagraph.change.base;

/**
 * Exact logical-key lookup outcome from an immutable Base artifact index.
 */
public sealed interface BaseArtifactLookup
        permits BaseArtifactMissing, BaseArtifactFound,
        BaseArtifactAmbiguous {
}
