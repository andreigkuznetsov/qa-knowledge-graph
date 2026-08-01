package ru.kuznetsov.qaip.core.application.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationIssueTest {
    @Test
    void supports_project_node_relationship_and_combined_diagnostics() {
        ValidationIssue project = issue(null, null);
        ValidationIssue node = issue(" N-1 ", null);
        ValidationIssue relationship = issue(null, " R-1 ");
        ValidationIssue both = issue("N", "R");
        assertNull(project.nodeId());
        assertNull(project.relationshipId());
        assertEquals(" N-1 ", node.nodeId());
        assertEquals(" R-1 ", relationship.relationshipId());
        assertEquals("N", both.nodeId());
        assertEquals("R", both.relationshipId());
    }

    @Test
    void preserves_exact_required_text_and_has_value_semantics() {
        ValidationIssue issue = new ValidationIssue(" RULE ", " CODE ", ValidationSeverity.WARNING,
                " message ", " node ", " relationship ");
        assertEquals(" RULE ", issue.ruleId());
        assertEquals(" CODE ", issue.code());
        assertEquals(" message ", issue.message());
        assertEquals(issue, new ValidationIssue(" RULE ", " CODE ", ValidationSeverity.WARNING,
                " message ", " node ", " relationship "));
        assertEquals(issue.hashCode(), new ValidationIssue(" RULE ", " CODE ", ValidationSeverity.WARNING,
                " message ", " node ", " relationship ").hashCode());
    }

    @Test
    void rejects_null_and_blank_required_values_while_optional_ids_accept_null() {
        assertThrows(NullPointerException.class,
                () -> new ValidationIssue(null, "C", ValidationSeverity.ERROR, "message", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationIssue(" ", "C", ValidationSeverity.ERROR, "message", null, null));
        assertThrows(NullPointerException.class,
                () -> new ValidationIssue("R", null, ValidationSeverity.ERROR, "message", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationIssue("R", "\t", ValidationSeverity.ERROR, "message", null, null));
        assertThrows(NullPointerException.class,
                () -> new ValidationIssue("R", "C", null, "message", null, null));
        assertThrows(NullPointerException.class,
                () -> new ValidationIssue("R", "C", ValidationSeverity.ERROR, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationIssue("R", "C", ValidationSeverity.ERROR, "", null, null));
        assertDoesNotThrow(() -> issue(null, null));
    }

    private static ValidationIssue issue(String nodeId, String relationshipId) {
        return new ValidationIssue("RULE", "CODE", ValidationSeverity.ERROR, "message", nodeId, relationshipId);
    }
}
