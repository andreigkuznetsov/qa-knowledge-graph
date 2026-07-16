package ru.kuznetsov.qagraph.validationcore.validation;

import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;

public record AllowedRelationship(
        NodeType from,
        RelationshipType relationship,
        NodeType to,
        boolean selfReferenceAllowed
) {
}
