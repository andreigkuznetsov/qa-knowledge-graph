package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryResult;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryTextRendererTest {
    private final ProjectSummaryTextRenderer renderer = new ProjectSummaryTextRenderer();

    @Test
    void renders_all_fields_in_exact_order_without_trailing_whitespace() {
        ProjectSummaryResult summary = new ProjectSummaryResult(
                " P-Id ", " Contract-V1 ", " Schema-V2 ", 11, 22, 33, 44, 55);
        String expected = String.join(System.lineSeparator(),
                "Project Summary", "Project ID:  P-Id ", "Contract Version:  Contract-V1 ",
                "Schema Version:  Schema-V2 ", "Sources: 11", "Nodes: 22", "Relationships: 33",
                "Evidence: 44", "Declared Changes: 55");
        assertEquals(expected, renderer.renderFound(summary));
        assertEquals(expected, renderer.renderFound(summary));
        assertFalse(renderer.renderFound(summary).contains("ProjectSummaryResult["));
    }

    @Test
    void renders_zero_counts_and_not_found_exactly() {
        assertEquals(String.join(System.lineSeparator(), "Project Summary", "Project ID: P",
                        "Contract Version: C", "Schema Version: S", "Sources: 0", "Nodes: 0",
                        "Relationships: 0", "Evidence: 0", "Declared Changes: 0"),
                renderer.renderFound(new ProjectSummaryResult("P", "C", "S", 0, 0, 0, 0, 0)));
        assertEquals("Project not found:  P ", renderer.renderNotFound(" P "));
    }
}
