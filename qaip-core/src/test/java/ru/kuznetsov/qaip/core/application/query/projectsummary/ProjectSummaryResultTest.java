package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryResultTest {
    @Test
    void is_a_scalar_value_with_exact_text_and_record_semantics() {
        ProjectSummaryResult first = result(" P-Id ", " Contract-V1 ", " Schema-V1 ", 0, 1, 2, 3, 4);
        ProjectSummaryResult equal = result(" P-Id ", " Contract-V1 ", " Schema-V1 ", 0, 1, 2, 3, 4);
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertEquals(" P-Id ", first.projectId());
        assertEquals(" Contract-V1 ", first.projectContractVersion());
        assertEquals(" Schema-V1 ", first.schemaVersion());
    }

    @Test
    void rejects_null_or_blank_text_independently() {
        assertThrows(NullPointerException.class, () -> result(null, "c", "s", 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result(" ", "c", "s", 0, 0, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> result("p", null, "s", 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "\t", "s", 0, 0, 0, 0, 0));
        assertThrows(NullPointerException.class, () -> result("p", "c", null, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "\n", 0, 0, 0, 0, 0));
    }

    @Test
    void rejects_each_negative_count_and_accepts_zero_and_positive_counts() {
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "s", -1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "s", 0, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "s", 0, 0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "s", 0, 0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> result("p", "c", "s", 0, 0, 0, 0, -1));
        assertDoesNotThrow(() -> result("p", "c", "s", 0, 0, 0, 0, 0));
        assertDoesNotThrow(() -> result("p", "c", "s", 1, 2, 3, 4, 5));
    }

    private static ProjectSummaryResult result(String id, String contract, String schema,
                                                int sources, int nodes, int relationships,
                                                int evidence, int changes) {
        return new ProjectSummaryResult(id, contract, schema, sources, nodes, relationships, evidence, changes);
    }
}
