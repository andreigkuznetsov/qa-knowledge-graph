package ru.kuznetsov.qagraph.change.model;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.Objects;

/**
 * Mutation-safe snapshot of a recognizable Canonical QA Model relationship.
 */
public final class RelationshipArtifactState implements ArtifactState {

    private final CanonicalQaModelVersion schemaVersion;
    private final CanonicalIdentity identity;
    private final JsonNode snapshot;

    public RelationshipArtifactState(
            CanonicalQaModelVersion schemaVersion,
            JsonNode snapshot
    ) {
        this.schemaVersion = Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
        JsonNode copy = ArtifactStateSupport.objectCopy(snapshot);
        String type = ArtifactStateSupport.requiredText(copy, "type");
        if (RelationshipType.from(type) == null) {
            throw new IllegalArgumentException(
                    "snapshot type must be a Canonical QA Model relationship type"
            );
        }
        this.identity = ArtifactStateSupport.identity(copy);
        this.snapshot = copy;
    }

    @Override
    public ArtifactCategory category() {
        return ArtifactCategory.RELATIONSHIP;
    }

    @Override
    public CanonicalIdentity identity() {
        return identity;
    }

    @Override
    public CanonicalQaModelVersion schemaVersion() {
        return schemaVersion;
    }

    @Override
    public JsonNode snapshot() {
        return snapshot.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RelationshipArtifactState that)) {
            return false;
        }
        return schemaVersion.equals(that.schemaVersion)
                && identity.equals(that.identity)
                && snapshot.equals(that.snapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, identity, snapshot);
    }

    @Override
    public String toString() {
        return "RelationshipArtifactState[identity=" + identity
                + ", schemaVersion=" + schemaVersion + "]";
    }
}
