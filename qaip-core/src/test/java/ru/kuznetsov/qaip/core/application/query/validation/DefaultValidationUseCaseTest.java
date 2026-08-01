package ru.kuznetsov.qaip.core.application.query.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.*;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultValidationUseCaseTest {
    @Test
    void existing_project_executes_once_preserves_exact_id_and_returns_completed_for_all_report_states() {
        Project project = project(" P ", List.of(), List.of());
        CountingReader reader = new CountingReader(Optional.of(project));
        ValidationUseCase emptyUseCase = useCase(reader, new ValidationEngine(List.of()));
        ValidationCompleted empty = assertInstanceOf(ValidationCompleted.class, emptyUseCase.execute(" P "));
        assertEquals(" P ", empty.projectId());
        assertEquals(new ValidationReportResult(true, 0, 0, List.of()), empty.report());
        assertEquals(1, reader.calls);
        assertEquals(" P ", reader.projectId);

        ValidationCompleted warnings = assertInstanceOf(ValidationCompleted.class,
                useCase(id -> Optional.of(project), engine(issue("R", "W", ValidationSeverity.WARNING)))
                        .execute("P"));
        assertTrue(warnings.report().valid());
        assertEquals(1, warnings.report().warningCount());

        ValidationCompleted errors = assertInstanceOf(ValidationCompleted.class,
                useCase(id -> Optional.of(project), engine(issue("R", "E", ValidationSeverity.ERROR)))
                        .execute("P"));
        assertFalse(errors.report().valid());
        assertEquals(1, errors.report().errorCount());
    }

    @Test
    void missing_project_short_circuits_to_exact_typed_result() {
        CountingReader reader = new CountingReader(Optional.empty());
        assertEquals(new ValidationProjectNotFound(" missing "),
                useCase(reader, new ValidationEngine(List.of())).execute(" missing "));
        assertEquals(1, reader.calls);
        assertEquals(" missing ", reader.projectId);
    }

    @Test
    void constructor_and_input_contracts_fail_before_reader_invocation() {
        ProjectReader reader = id -> Optional.empty();
        ValidationEngine engine = new ValidationEngine(List.of());
        ValidationReportMapper mapper = new ValidationReportMapper();
        assertThrows(NullPointerException.class, () -> new DefaultValidationUseCase(null, engine, mapper));
        assertThrows(NullPointerException.class, () -> new DefaultValidationUseCase(reader, null, mapper));
        assertThrows(NullPointerException.class, () -> new DefaultValidationUseCase(reader, engine, null));
        CountingReader counting = new CountingReader(Optional.empty());
        ValidationUseCase useCase = useCase(counting, engine);
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" \t"));
        assertEquals(0, counting.calls);
    }

    @Test
    void reader_null_persistence_and_engine_failures_propagate_without_typed_conversion() {
        assertThrows(NullPointerException.class,
                () -> useCase(id -> null, new ValidationEngine(List.of())).execute("P"));
        ProjectPersistenceException persistence = new ProjectPersistenceException("read failed");
        assertSame(persistence, assertThrows(ProjectPersistenceException.class,
                () -> useCase(id -> { throw persistence; }, new ValidationEngine(List.of())).execute("P")));
        RuntimeException engineFailure = new IllegalStateException("rule failed");
        ProjectValidationRule failingRule = new ProjectValidationRule() {
            public String ruleId() { return "FAIL"; }
            public List<ValidationIssue> validate(Project project) { throw engineFailure; }
        };
        assertSame(engineFailure, assertThrows(RuntimeException.class,
                () -> useCase(id -> Optional.of(project("P", List.of(), List.of())),
                        new ValidationEngine(List.of(failingRule))).execute("P")));
    }

    @Test
    void result_hierarchy_contracts_and_value_semantics_accept_valid_warning_and_invalid_reports() {
        ValidationReportResult valid = new ValidationReportResult(true, 0, 0, List.of());
        ValidationReportResult warning = new ValidationReportResult(true, 0, 1, List.of());
        ValidationReportResult invalid = new ValidationReportResult(false, 1, 0, List.of());
        assertDoesNotThrow(() -> new ValidationCompleted("P", valid));
        assertDoesNotThrow(() -> new ValidationCompleted("P", warning));
        assertDoesNotThrow(() -> new ValidationCompleted("P", invalid));
        assertEquals(new ValidationCompleted("P", valid), new ValidationCompleted("P", valid));
        assertEquals(new ValidationCompleted("P", valid).hashCode(), new ValidationCompleted("P", valid).hashCode());
        assertThrows(NullPointerException.class, () -> new ValidationCompleted(null, valid));
        assertThrows(IllegalArgumentException.class, () -> new ValidationCompleted(" ", valid));
        assertThrows(NullPointerException.class, () -> new ValidationCompleted("P", null));
        assertThrows(NullPointerException.class, () -> new ValidationProjectNotFound(null));
        assertThrows(IllegalArgumentException.class, () -> new ValidationProjectNotFound(""));
    }

    private static ValidationEngine engine(ValidationIssue issue) {
        return new ValidationEngine(List.of(new ProjectValidationRule() {
            public String ruleId() { return issue.ruleId(); }
            public List<ValidationIssue> validate(Project project) { return List.of(issue); }
        }));
    }

    private static ValidationIssue issue(String rule, String code, ValidationSeverity severity) {
        return new ValidationIssue(rule, code, severity, "message", null, null);
    }

    static DefaultValidationUseCase useCase(ProjectReader reader, ValidationEngine engine) {
        return new DefaultValidationUseCase(reader, engine, new ValidationReportMapper());
    }

    static Node node(String id, String type) {
        return new Node(id, type, id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    static Project project(String id, List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata(id, id, null, null, Map.of()), List.of(),
                new Subject("local"), nodes, relationships, new EvidenceManifest("e", "s", Map.of(),
                "n", "c", "f", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static final class CountingReader implements ProjectReader {
        private final Optional<Project> result;
        private int calls;
        private String projectId;
        private CountingReader(Optional<Project> result) { this.result = result; }
        public Optional<Project> findById(String value) { calls++; projectId = value; return result; }
    }
}
