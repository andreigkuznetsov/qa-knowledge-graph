package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet;
import ru.kuznetsov.qagraph.model.NodeType;
import java.util.Objects;

/** Exact accepted declaration proving direct impact. */
public record DirectChangeProof(VerifiedChangeSet verifiedChangeSet, int declarationIndex,
        CanonicalIdentity identity, ArtifactCategory category, NodeType nodeType, ChangeKind changeKind)
        implements ImpactProof {
    public DirectChangeProof {
        Objects.requireNonNull(verifiedChangeSet); Objects.requireNonNull(identity);
        Objects.requireNonNull(category); Objects.requireNonNull(nodeType); Objects.requireNonNull(changeKind);
        if (declarationIndex < 0 || declarationIndex >= verifiedChangeSet.declaredChangeSet().changes().size())
            throw new IllegalArgumentException("declarationIndex is outside the accepted change set");
        DeclaredChange actual = verifiedChangeSet.declaredChangeSet().changes().get(declarationIndex);
        if (!actual.identity().equals(identity) || actual.category() != category || actual.kind() != changeKind)
            throw new IllegalArgumentException("proof fields do not match the accepted declaration");
    }
}
