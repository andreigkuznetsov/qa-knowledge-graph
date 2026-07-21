package ru.kuznetsov.qagraph.change.root;

/**
 * Explicit all-or-nothing Base root extraction result.
 */
public sealed interface CanonicalBaseEvidenceExtractionResult
        permits CanonicalBaseEvidenceExtracted,
        CanonicalBaseEvidenceExtractionFailure {
}
