package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryQueryResultTest {
    @Test
    void found_preserves_exact_summary_and_has_value_semantics() {
        ProjectSummaryResult summary = summary();
        ProjectSummaryFound first = new ProjectSummaryFound(summary);
        ProjectSummaryFound equal = new ProjectSummaryFound(summary);
        assertSame(summary, first.summary());
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertThrows(NullPointerException.class, () -> new ProjectSummaryFound(null));
        assertEquals(1, ProjectSummaryFound.class.getRecordComponents().length);
        assertEquals(ProjectSummaryResult.class, ProjectSummaryFound.class.getRecordComponents()[0].getType());
    }

    @Test
    void not_found_validates_and_preserves_exact_id_with_value_semantics() {
        ProjectSummaryNotFound first = new ProjectSummaryNotFound(" P-Id ");
        ProjectSummaryNotFound equal = new ProjectSummaryNotFound(" P-Id ");
        assertEquals(" P-Id ", first.projectId());
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertThrows(NullPointerException.class, () -> new ProjectSummaryNotFound(null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectSummaryNotFound(" \t"));
        assertEquals(1, ProjectSummaryNotFound.class.getRecordComponents().length);
        assertEquals(String.class, ProjectSummaryNotFound.class.getRecordComponents()[0].getType());
    }

    @Test
    void hierarchy_is_sealed_with_exactly_two_outcomes() {
        assertTrue(ProjectSummaryQueryResult.class.isSealed());
        assertEquals(Set.of(ProjectSummaryFound.class, ProjectSummaryNotFound.class),
                java.util.Arrays.stream(ProjectSummaryQueryResult.class.getPermittedSubclasses())
                        .collect(Collectors.toSet()));
    }

    private static ProjectSummaryResult summary() {
        return new ProjectSummaryResult("P", "contract", "schema", 0, 0, 0, 0, 0);
    }
}
