package ru.kuznetsov.qaip.core.application.query.relationship;

import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.Objects;

public final class RelationshipDetailsMapper {
    public RelationshipDetailsResult map(ProjectRelationships relationships) {
        Objects.requireNonNull(relationships, "relationships");
        return new RelationshipDetailsResult(
                relationships.incoming().stream().map(RelationshipDetailsMapper::mapOne).toList(),
                relationships.outgoing().stream().map(RelationshipDetailsMapper::mapOne).toList());
    }

    private static RelationshipDetails mapOne(Relationship relationship) {
        return new RelationshipDetails(
                relationship.id(), relationship.from(), relationship.to(), relationship.type());
    }
}
