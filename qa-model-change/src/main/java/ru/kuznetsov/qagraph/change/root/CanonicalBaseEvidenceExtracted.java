package ru.kuznetsov.qagraph.change.root;

import java.util.Objects;

/**
 * Successfully extracted immutable Base root evidence.
 */
public record CanonicalBaseEvidenceExtracted(
        CanonicalBaseModelEvidence evidence
) implements CanonicalBaseEvidenceExtractionResult {

    public CanonicalBaseEvidenceExtracted {
        Objects.requireNonNull(evidence, "evidence must not be null");
    }
}
