package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;

import java.util.Objects;

public record CompleteProposedRootValid(
        ProposedRootReconstructed reconstructedRoot,
        SchemaValidationEvidence schemaEvidence,
        SemanticValidationEvidence semanticEvidence
) implements CompleteProposedRootValidationResult {
    public CompleteProposedRootValid {
        Objects.requireNonNull(reconstructedRoot, "reconstructedRoot is required");
        Objects.requireNonNull(schemaEvidence, "schemaEvidence is required");
        Objects.requireNonNull(semanticEvidence, "semanticEvidence is required");
        if (!schemaEvidence.valid() || !semanticEvidence.valid()) {
            throw new IllegalArgumentException(
                    "complete-valid requires successful validation evidence"
            );
        }
    }
}
