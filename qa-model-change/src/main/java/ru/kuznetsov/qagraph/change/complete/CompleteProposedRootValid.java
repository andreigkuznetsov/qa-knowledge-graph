package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;
import java.util.Objects;

/** Complete validation success created only by {@link CompleteProposedRootValidator}. */
public final class CompleteProposedRootValid implements CompleteProposedRootValidationResult {
    private final ProposedRootReconstructed reconstructedRoot;
    private final SchemaValidationEvidence schemaEvidence;
    private final SemanticValidationEvidence semanticEvidence;
    CompleteProposedRootValid(ProposedRootReconstructed root, SchemaValidationEvidence schema, SemanticValidationEvidence semantic) {
        reconstructedRoot = Objects.requireNonNull(root);
        schemaEvidence = Objects.requireNonNull(schema);
        semanticEvidence = Objects.requireNonNull(semantic);
        if (!schema.valid() || !semantic.valid()) throw new IllegalArgumentException("complete-valid requires successful validation evidence");
    }
    public ProposedRootReconstructed reconstructedRoot() { return reconstructedRoot; }
    public SchemaValidationEvidence schemaEvidence() { return schemaEvidence; }
    public SemanticValidationEvidence semanticEvidence() { return semanticEvidence; }
    @Override public boolean equals(Object o) { return o instanceof CompleteProposedRootValid that && reconstructedRoot.equals(that.reconstructedRoot) && schemaEvidence.equals(that.schemaEvidence) && semanticEvidence.equals(that.semanticEvidence); }
    @Override public int hashCode() { return Objects.hash(reconstructedRoot, schemaEvidence, semanticEvidence); }
}
