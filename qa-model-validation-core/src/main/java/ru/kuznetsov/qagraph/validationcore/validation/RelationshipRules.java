package ru.kuznetsov.qagraph.validationcore.validation;

import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;

import java.util.Set;

public final class RelationshipRules {

    private RelationshipRules() {
    }

    private static final Set<AllowedRelationship> ALLOWED = Set.of(
            rule(NodeType.USER_STORY, RelationshipType.DESCRIBES, NodeType.BUSINESS_OPERATION),
            rule(NodeType.BUSINESS_OPERATION, RelationshipType.GOVERNED_BY, NodeType.BUSINESS_RULE),
            rule(NodeType.BUSINESS_OPERATION, RelationshipType.SPECIFIED_BY, NodeType.SCENARIO),
            rule(NodeType.BUSINESS_OPERATION, RelationshipType.IMPLEMENTED_BY, NodeType.TECHNICAL_IMPLEMENTATION),
            rule(NodeType.TEST_IMPLEMENTATION, RelationshipType.VALIDATES, NodeType.SCENARIO),
            rule(NodeType.TEST_IMPLEMENTATION, RelationshipType.USES, NodeType.TECHNICAL_IMPLEMENTATION),
            rule(NodeType.TEST_IMPLEMENTATION, RelationshipType.HAS_CHECK, NodeType.CHECK),
            rule(NodeType.SCENARIO, RelationshipType.COVERS, NodeType.BUSINESS_RULE),
            rule(NodeType.SCENARIO, RelationshipType.REFINES, NodeType.SCENARIO),
            rule(NodeType.BUSINESS_RULE, RelationshipType.DEPENDS_ON, NodeType.BUSINESS_RULE),
            rule(NodeType.BUSINESS_RULE, RelationshipType.SUPERSEDES, NodeType.BUSINESS_RULE),
            new AllowedRelationship(NodeType.USER_STORY, RelationshipType.RELATED_TO, NodeType.USER_STORY, true),
            new AllowedRelationship(NodeType.BUSINESS_OPERATION, RelationshipType.RELATED_TO,
                    NodeType.BUSINESS_OPERATION, true)
    );

    public static boolean isAllowed(NodeType from, RelationshipType relationship, NodeType to) {
        return ALLOWED.stream().anyMatch(rule ->
                rule.from() == from
                        && rule.relationship() == relationship
                        && rule.to() == to
        );
    }

    public static boolean isSelfReferenceAllowed(NodeType from, RelationshipType relationship, NodeType to) {
        return ALLOWED.stream()
                .filter(rule -> rule.from() == from
                        && rule.relationship() == relationship
                        && rule.to() == to)
                .map(AllowedRelationship::selfReferenceAllowed)
                .findFirst()
                .orElse(false);
    }

    private static AllowedRelationship rule(NodeType from, RelationshipType relationship, NodeType to) {
        return new AllowedRelationship(from, relationship, to, false);
    }
}
