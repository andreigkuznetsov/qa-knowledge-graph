package ru.kuznetsov.qaip.core.application.validation.rule;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.*;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInValidationRulesTest {
    private final IsolatedNodeValidationRule isolatedRule = new IsolatedNodeValidationRule();
    private final ScenarioWithoutTestValidationRule scenarioRule = new ScenarioWithoutTestValidationRule();

    @Test
    void isolated_nodes_produce_ordered_warnings_for_all_types_with_exact_ids() {
        Node scenario = node(" SC ", "SCENARIO");
        Node test = node("T", "TEST_IMPLEMENTATION");
        Node custom = node("C", "CUSTOM");
        Project project = project(List.of(scenario, test, custom), List.of());
        Project before = project;
        List<ValidationIssue> issues = isolatedRule.validate(project);
        assertEquals(List.of(" SC ", "T", "C"), issues.stream().map(ValidationIssue::nodeId).toList());
        assertTrue(issues.stream().allMatch(issue -> issue.ruleId().equals("ISOLATED_NODE")
                && issue.code().equals("NODE_WITHOUT_RELATIONSHIPS")
                && issue.severity() == ValidationSeverity.WARNING
                && issue.relationshipId() == null));
        assertEquals("Node ' SC ' has no relationships.", issues.getFirst().message());
        assertEquals(before, project);
    }

    @Test
    void incoming_outgoing_and_self_reference_all_prevent_isolation() {
        Project project = project(List.of(node("IN", "A"), node("OUT", "B"), node("SELF", "C")), List.of(
                relationship("R1", "OUT", "LINK", "IN"),
                relationship("R2", "SELF", "LINK", "SELF")));
        assertTrue(isolatedRule.validate(project).isEmpty());
        assertTrue(isolatedRule.validate(project(List.of(), List.of())).isEmpty());
    }

    @Test
    void isolated_rule_rejects_null_and_returns_immutable_results() {
        assertThrows(NullPointerException.class, () -> isolatedRule.validate(null));
        List<ValidationIssue> result = isolatedRule.validate(project(List.of(node("A", "TYPE")), List.of()));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void canonical_direct_test_validates_scenario_coverage() {
        Node scenario = node("S", "SCENARIO");
        Node test = node("T", "TEST_IMPLEMENTATION");
        Project project = project(List.of(scenario, test), List.of(relationship("R", "T", "VALIDATES", "S")));
        assertTrue(scenarioRule.validate(project).isEmpty());
    }

    @Test
    void uncovered_scenarios_produce_ordered_errors_and_ignore_other_nodes() {
        Node first = node(" S-2 ", "SCENARIO");
        Node other = node("O", "BUSINESS_RULE");
        Node second = node("S-1", "SCENARIO");
        Project project = project(List.of(first, other, second), List.of());
        Project before = project;
        List<ValidationIssue> issues = scenarioRule.validate(project);
        assertEquals(List.of(" S-2 ", "S-1"), issues.stream().map(ValidationIssue::nodeId).toList());
        assertTrue(issues.stream().allMatch(issue -> issue.ruleId().equals("SCENARIO_REQUIRES_TEST")
                && issue.code().equals("SCENARIO_WITHOUT_TEST")
                && issue.severity() == ValidationSeverity.ERROR
                && issue.relationshipId() == null));
        assertEquals("Scenario ' S-2 ' has no validating test implementation.", issues.getFirst().message());
        assertEquals(before, project);
        assertTrue(scenarioRule.validate(project(List.of(other), List.of())).isEmpty());
    }

    @Test
    void wrong_type_direction_or_counterpart_and_unrelated_test_do_not_cover_scenario() {
        Node scenario = node("S", "SCENARIO");
        Node test = node("T", "TEST_IMPLEMENTATION");
        Node nonTest = node("N", "BUSINESS_RULE");
        for (Relationship relationship : List.of(
                relationship("WRONG_TYPE", "T", "RELATED_TO", "S"),
                relationship("WRONG_DIRECTION", "S", "VALIDATES", "T"),
                relationship("NON_TEST", "N", "VALIDATES", "S"),
                relationship("UNRELATED_TEST", "T", "VALIDATES", "OTHER"))) {
            assertEquals(List.of("S"), scenarioRule.validate(project(
                    List.of(scenario, test, nonTest), List.of(relationship))).stream()
                    .map(ValidationIssue::nodeId).toList());
        }
    }

    @Test
    void scenario_rule_rejects_null_and_returns_immutable_results() {
        assertThrows(NullPointerException.class, () -> scenarioRule.validate(null));
        List<ValidationIssue> result = scenarioRule.validate(project(List.of(node("S", "SCENARIO")), List.of()));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void real_engine_preserves_configured_rule_order_and_is_deterministic() {
        Project project = project(List.of(node("S", "SCENARIO"), node("I", "BUSINESS_RULE")), List.of());
        ValidationEngine warningFirst = new ValidationEngine(List.of(isolatedRule, scenarioRule));
        ValidationReport first = warningFirst.validate(project);
        assertEquals(List.of("ISOLATED_NODE", "ISOLATED_NODE", "SCENARIO_REQUIRES_TEST"),
                first.issues().stream().map(ValidationIssue::ruleId).toList());
        assertEquals(List.of(ValidationSeverity.WARNING, ValidationSeverity.WARNING, ValidationSeverity.ERROR),
                first.issues().stream().map(ValidationIssue::severity).toList());
        assertEquals(first, warningFirst.validate(project));
        assertEquals(List.of("SCENARIO_REQUIRES_TEST", "ISOLATED_NODE", "ISOLATED_NODE"),
                new ValidationEngine(List.of(scenarioRule, isolatedRule)).validate(project).issues().stream()
                        .map(ValidationIssue::ruleId).toList());
    }

    private static Node node(String id, String type) {
        return new Node(id, type, id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P", "P", null, null, Map.of()), List.of(),
                new Subject("local"), nodes, relationships, new EvidenceManifest("e", "s", Map.of(),
                "n", "c", "f", List.of(), List.of(), List.of()), List.of(), Map.of());
    }
}
