package ru.kuznetsov.qaip.core.application.query.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.ValidationIssue;
import ru.kuznetsov.qaip.core.application.validation.ValidationReport;
import ru.kuznetsov.qaip.core.application.validation.ValidationSeverity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationReportMapperTest {
    private final ValidationReportMapper mapper = new ValidationReportMapper();

    @Test
    void maps_empty_warning_error_and_mixed_reports_with_frozen_aggregate_queries() {
        assertEquals(new ValidationReportResult(true, 0, 0, List.of()),
                mapper.map(new ValidationReport(List.of())));
        ValidationIssue warning = issue(" W ", " WC ", ValidationSeverity.WARNING, " warning ", null, null);
        ValidationIssue error = issue("E", "EC", ValidationSeverity.ERROR, "error", " N ", " R ");
        ValidationReport warningReport = new ValidationReport(List.of(warning));
        ValidationReport errorReport = new ValidationReport(List.of(error));
        assertEquals(new ValidationReportResult(true, 0, 1, List.of(
                new ValidationIssueResult(" W ", " WC ", "WARNING", " warning ", null, null))),
                mapper.map(warningReport));
        assertEquals(new ValidationReportResult(false, 1, 0, List.of(
                new ValidationIssueResult("E", "EC", "ERROR", "error", " N ", " R "))),
                mapper.map(errorReport));
        assertEquals(new ValidationReportResult(false, 1, 1, List.of(
                new ValidationIssueResult(" W ", " WC ", "WARNING", " warning ", null, null),
                new ValidationIssueResult("E", "EC", "ERROR", "error", " N ", " R "))),
                mapper.map(new ValidationReport(List.of(warning, error))));
    }

    @Test
    void maps_all_optional_reference_shapes_and_preserves_order_duplicates_and_exact_text() {
        ValidationIssue project = issue("RULE", "PROJECT", ValidationSeverity.WARNING, "project", null, null);
        ValidationIssue node = issue("RULE", "NODE", ValidationSeverity.ERROR, "node", "N", null);
        ValidationIssue relationship = issue("RULE", "REL", ValidationSeverity.WARNING, "rel", null, "R");
        ValidationIssue both = issue("RULE", "BOTH", ValidationSeverity.ERROR, "both", "N", "R");
        ValidationReport source = new ValidationReport(List.of(project, node, relationship, both, project));
        ValidationReport before = source;
        ValidationReportResult first = mapper.map(source);
        assertEquals(List.of("PROJECT", "NODE", "REL", "BOTH", "PROJECT"), first.issues().stream()
                .map(ValidationIssueResult::code).toList());
        assertNull(first.issues().get(0).nodeId());
        assertNull(first.issues().get(0).relationshipId());
        assertEquals("N", first.issues().get(1).nodeId());
        assertEquals("R", first.issues().get(2).relationshipId());
        assertEquals("N", first.issues().get(3).nodeId());
        assertEquals("R", first.issues().get(3).relationshipId());
        assertEquals(first.issues().getFirst(), first.issues().getLast());
        assertEquals(first, mapper.map(source));
        assertEquals(before, source);
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    @Test
    void issue_result_enforces_required_values_and_preserves_optional_values() {
        ValidationIssueResult value = new ValidationIssueResult(" RULE ", " CODE ", " WARNING ",
                " message ", null, " R ");
        assertEquals(" RULE ", value.ruleId());
        assertNull(value.nodeId());
        assertEquals(value, new ValidationIssueResult(" RULE ", " CODE ", " WARNING ",
                " message ", null, " R "));
        assertEquals(value.hashCode(), new ValidationIssueResult(" RULE ", " CODE ", " WARNING ",
                " message ", null, " R ").hashCode());
        assertThrows(NullPointerException.class,
                () -> new ValidationIssueResult(null, "C", "ERROR", "m", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationIssueResult(" ", "C", "ERROR", "m", null, null));
        assertThrows(NullPointerException.class,
                () -> new ValidationIssueResult("R", null, "ERROR", "m", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationIssueResult("R", "C", "\t", "m", null, null));
        assertThrows(NullPointerException.class,
                () -> new ValidationIssueResult("R", "C", "ERROR", null, null, null));
    }

    @Test
    void report_result_enforces_counts_consistency_and_immutable_defensive_list() {
        ValidationIssueResult issue = new ValidationIssueResult("R", "C", "WARNING", "m", null, null);
        var source = new ArrayList<>(List.of(issue));
        ValidationReportResult result = new ValidationReportResult(true, 0, 7, source);
        source.clear();
        assertEquals(List.of(issue), result.issues());
        assertThrows(UnsupportedOperationException.class, () -> result.issues().clear());
        assertThrows(IllegalArgumentException.class, () -> new ValidationReportResult(true, -1, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ValidationReportResult(true, 0, -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ValidationReportResult(true, 1, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ValidationReportResult(false, 0, 0, List.of()));
        assertThrows(NullPointerException.class, () -> new ValidationReportResult(true, 0, 0, null));
        var withNull = new ArrayList<ValidationIssueResult>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ValidationReportResult(true, 0, 0, withNull));
    }

    private static ValidationIssue issue(String rule, String code, ValidationSeverity severity, String message,
                                         String nodeId, String relationshipId) {
        return new ValidationIssue(rule, code, severity, message, nodeId, relationshipId);
    }
}
