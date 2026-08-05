package ru.kuznetsov.qagraph.validationcore.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.NodeType;
import ru.kuznetsov.qagraph.validationcore.model.RelationshipType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipRulesTest {

    @Test
    void shouldAllowUserStoryToDescribeBusinessOperation() {
        assertTrue(RelationshipRules.isAllowed(
                NodeType.USER_STORY,
                RelationshipType.DESCRIBES,
                NodeType.BUSINESS_OPERATION
        ));
    }

    @Test
    void shouldRejectReverseDescribeRelationship() {
        assertFalse(RelationshipRules.isAllowed(
                NodeType.BUSINESS_OPERATION,
                RelationshipType.DESCRIBES,
                NodeType.USER_STORY
        ));
    }

    @Test
    void shouldAllowTechnicalImplementationToUseTechnicalImplementation() {
        assertTrue(RelationshipRules.isAllowed(
                NodeType.TECHNICAL_IMPLEMENTATION,
                RelationshipType.USES,
                NodeType.TECHNICAL_IMPLEMENTATION
        ));
    }

    @Test
    void shouldAllowTechnicalImplementationToPublishToTechnicalImplementation() {
        assertTrue(RelationshipRules.isAllowed(
                NodeType.TECHNICAL_IMPLEMENTATION,
                RelationshipType.PUBLISHES_TO,
                NodeType.TECHNICAL_IMPLEMENTATION
        ));
    }
}
