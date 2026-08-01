package ru.kuznetsov.qaip.core.application.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineTest {
    @Test
    void construction_validates_rule_collection_and_exact_unique_ids() {
        assertDoesNotThrow(() -> new ValidationEngine(List.of()));
        assertThrows(NullPointerException.class, () -> new ValidationEngine(null));
        var withNull = new ArrayList<ProjectValidationRule>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ValidationEngine(withNull));
        assertThrows(NullPointerException.class, () -> new ValidationEngine(List.of(rule(null, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> new ValidationEngine(List.of(rule(" \t", List.of()))));
        assertThrows(IllegalArgumentException.class, () -> new ValidationEngine(List.of(
                rule("DUP", List.of()), rule("DUP", List.of()))));
        assertDoesNotThrow(() -> new ValidationEngine(List.of(
                rule("RULE", List.of()), rule("rule", List.of()), rule(" RULE ", List.of()))));
    }

    @Test
    void source_list_mutation_does_not_change_configuration_and_empty_engine_is_empty() {
        var source = new ArrayList<ProjectValidationRule>();
        ValidationEngine engine = new ValidationEngine(source);
        source.add(rule("LATE", List.of(issue("LATE", "L"))));
        assertEquals(new ValidationReport(List.of()), engine.validate(project()));
    }

    @Test
    void executes_each_rule_once_with_exact_project_and_preserves_all_order_and_duplicates() {
        Project project = project();
        Project before = project;
        ValidationIssue a1 = issue("A", "A1", ValidationSeverity.WARNING);
        ValidationIssue duplicate = issue("A", "DUP", ValidationSeverity.ERROR);
        ValidationIssue b1 = issue("B", "B1", ValidationSeverity.WARNING);
        RecordingRule a = new RecordingRule("A", List.of(a1, duplicate, duplicate));
        RecordingRule b = new RecordingRule("B", List.of(b1));
        ValidationEngine engine = new ValidationEngine(List.of(a, b));

        ValidationReport first = engine.validate(project);

        assertEquals(List.of(a1, duplicate, duplicate, b1), first.issues());
        assertEquals(1, a.validateCalls.get());
        assertEquals(1, b.validateCalls.get());
        assertSame(project, a.project);
        assertSame(project, b.project);
        assertEquals(before, project);
        assertEquals(first, engine.validate(project));
        assertEquals(2, a.validateCalls.get());
        assertEquals(2, b.validateCalls.get());
    }

    @Test
    void rejects_null_project_and_invalid_rule_results_without_partial_report() {
        ValidationEngine engine = new ValidationEngine(List.of(rule("A", List.of())));
        assertThrows(NullPointerException.class, () -> engine.validate(null));
        assertThrows(NullPointerException.class,
                () -> new ValidationEngine(List.of(rule("NULL", null))).validate(project()));
        var withNull = new ArrayList<ValidationIssue>();
        withNull.add(null);
        assertThrows(NullPointerException.class,
                () -> new ValidationEngine(List.of(rule("NULL_ELEMENT", withNull))).validate(project()));
        IllegalStateException mismatch = assertThrows(IllegalStateException.class,
                () -> new ValidationEngine(List.of(rule("EXECUTING", List.of(issue("OTHER", "C")))))
                        .validate(project()));
        assertTrue(mismatch.getMessage().contains("EXECUTING"));
        assertTrue(mismatch.getMessage().contains("OTHER"));
    }

    @Test
    void rule_failure_propagates_unchanged_and_stops_later_rules() {
        RuntimeException failure = new IllegalStateException("failed");
        AtomicInteger laterCalls = new AtomicInteger();
        ProjectValidationRule failing = new ProjectValidationRule() {
            public String ruleId() { return "FAIL"; }
            public List<ValidationIssue> validate(Project project) { throw failure; }
        };
        ProjectValidationRule later = new ProjectValidationRule() {
            public String ruleId() { return "LATER"; }
            public List<ValidationIssue> validate(Project project) {
                laterCalls.incrementAndGet();
                return List.of();
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> new ValidationEngine(List.of(failing, later)).validate(project())));
        assertEquals(0, laterCalls.get());
    }

    private static ProjectValidationRule rule(String id, List<ValidationIssue> issues) {
        return new ProjectValidationRule() {
            public String ruleId() { return id; }
            public List<ValidationIssue> validate(Project project) { return issues; }
        };
    }

    private static ValidationIssue issue(String ruleId, String code) {
        return issue(ruleId, code, ValidationSeverity.ERROR);
    }

    private static ValidationIssue issue(String ruleId, String code, ValidationSeverity severity) {
        return new ValidationIssue(ruleId, code, severity, "message", null, null);
    }

    private static Project project() {
        return new Project("contract", "schema", new Metadata("P", "P", null, null, Map.of()), List.of(),
                new Subject("local"), List.of(), List.of(), new EvidenceManifest("e", "s", Map.of(),
                "n", "c", "f", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static final class RecordingRule implements ProjectValidationRule {
        private final String id;
        private final List<ValidationIssue> issues;
        private final AtomicInteger validateCalls = new AtomicInteger();
        private Project project;
        private RecordingRule(String id, List<ValidationIssue> issues) { this.id = id; this.issues = issues; }
        public String ruleId() { return id; }
        public List<ValidationIssue> validate(Project value) {
            validateCalls.incrementAndGet();
            project = value;
            return issues;
        }
    }
}
