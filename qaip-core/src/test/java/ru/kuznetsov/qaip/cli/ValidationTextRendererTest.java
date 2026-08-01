package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationIssueResult;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationReportResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTextRendererTest {
    private final ValidationTextRenderer renderer = new ValidationTextRenderer();

    @Test
    void renders_exact_invalid_report_order_duplicates_and_all_reference_shapes() {
        ValidationIssueResult project = issue("PROJECT", "P", "ERROR", "Project message.", null, null);
        ValidationIssueResult node = issue("NODE", "N", "WARNING", "Node message.", " N ", null);
        ValidationIssueResult relationship = issue("REL", "R", "WARNING", "Rel message.", null, " R ");
        ValidationIssueResult both = issue("BOTH", "B", "ERROR", "Both message.", "N", "R");
        ValidationReportResult report = new ValidationReportResult(false, 2, 2,
                List.of(node, project, relationship, both, node));
        String expected = String.join(System.lineSeparator(),
                "Validation Report", "Project ID:  P ", "Status: INVALID", "Errors: 2", "Warnings: 2", "",
                "Issues:",
                "[WARNING] N | rule=NODE | Node message. | node= N ",
                "[ERROR] P | rule=PROJECT | Project message.",
                "[WARNING] R | rule=REL | Rel message. | relationship= R ",
                "[ERROR] B | rule=BOTH | Both message. | node=N | relationship=R",
                "[WARNING] N | rule=NODE | Node message. | node= N ");
        assertEquals(expected, renderer.renderCompleted(" P ", report));
        assertEquals(expected, renderer.renderCompleted(" P ", report));
        assertFalse(expected.contains("ValidationIssueResult["));
    }

    @Test
    void renders_valid_empty_report_with_none() {
        String output = renderer.renderCompleted("P", new ValidationReportResult(true, 0, 0, List.of()));
        assertEquals(String.join(System.lineSeparator(), "Validation Report", "Project ID: P", "Status: VALID",
                "Errors: 0", "Warnings: 0", "", "Issues:", "(none)"), output);
    }

    private static ValidationIssueResult issue(String rule, String code, String severity, String message,
                                                String node, String relationship) {
        return new ValidationIssueResult(rule, code, severity, message, node, relationship);
    }
}
