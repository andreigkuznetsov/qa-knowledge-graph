package ru.kuznetsov.qagraph.change.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Mutation-safe snapshot of one recognizable Canonical QA Model artifact.
 * Construction does not imply full model validation or change verification.
 */
public sealed interface ArtifactState
        permits NodeArtifactState, RelationshipArtifactState {

    ArtifactCategory category();

    CanonicalIdentity identity();

    CanonicalQaModelVersion schemaVersion();

    JsonNode snapshot();
}
