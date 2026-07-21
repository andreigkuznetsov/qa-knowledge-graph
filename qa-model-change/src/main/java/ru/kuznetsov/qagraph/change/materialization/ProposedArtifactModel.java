package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable candidate artifact view produced by deterministic materialization.
 * It does not imply complete Canonical QA Model validity.
 */
public record ProposedArtifactModel(
        CanonicalQaModelVersion schemaVersion,
        List<NodeArtifactState> nodes,
        List<RelationshipArtifactState> relationships
) {

    public ProposedArtifactModel {
        Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
        nodes = copyAndSort(nodes, "nodes", schemaVersion);
        relationships = copyAndSort(
                relationships,
                "relationships",
                schemaVersion
        );
    }

    public Optional<ArtifactState> lookup(
            ArtifactCategory category,
            CanonicalIdentity identity
    ) {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        return switch (category) {
            case NODE -> nodes.stream()
                    .filter(value -> value.identity().equals(identity))
                    .map(value -> (ArtifactState) value)
                    .findFirst();
            case RELATIONSHIP -> relationships.stream()
                    .filter(value -> value.identity().equals(identity))
                    .map(value -> (ArtifactState) value)
                    .findFirst();
        };
    }

    private static <T extends ArtifactState> List<T> copyAndSort(
            List<T> values,
            String name,
            CanonicalQaModelVersion schemaVersion
    ) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    name + " must not contain null members"
            );
        }
        if (values.stream().anyMatch(value ->
                !value.schemaVersion().equals(schemaVersion))) {
            throw new IllegalArgumentException(
                    name + " must use the Proposed Model version"
            );
        }
        Set<CanonicalIdentity> identities = new HashSet<>();
        if (values.stream().anyMatch(value ->
                !identities.add(value.identity()))) {
            throw new IllegalArgumentException(
                    name + " must not contain duplicate identities"
            );
        }
        return values.stream()
                .sorted(Comparator.comparing(value ->
                        value.identity().value()))
                .toList();
    }
}
