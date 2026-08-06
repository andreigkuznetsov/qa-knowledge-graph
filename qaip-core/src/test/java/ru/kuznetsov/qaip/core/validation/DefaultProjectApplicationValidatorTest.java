package ru.kuznetsov.qaip.core.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocument;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProjectApplicationValidatorTest {
    private final ProjectApplicationValidator validator = new DefaultProjectApplicationValidator();

    @Test
    void validate_valid_project_returns_success() {
        BoundProjectDocument bound = ValidationFixtures.bound(
                List.of(ValidationFixtures.node("BR-1", "BUSINESS_RULE")), List.of());
        Project before = bound.project();

        ApplicationValidationSuccess success = assertInstanceOf(
                ApplicationValidationSuccess.class, validator.validate(bound));
        assertInstanceOf(ApplicationValidProjectDocument.class, success.document());
        assertSame(before, success.document().project());
        assertTrue(success.warnings().isEmpty());
        assertEquals(before, bound.project());
    }

    @Test
    void duplicate_node_ids_report_every_later_occurrence_deterministically() {
        BoundProjectDocument bound = ValidationFixtures.bound(List.of(
                ValidationFixtures.node("A", "CHECK"),
                ValidationFixtures.node("A", "CHECK"),
                ValidationFixtures.node("B", "CHECK"),
                ValidationFixtures.node("B", "CHECK"),
                ValidationFixtures.node("B", "CHECK")), List.of());

        ApplicationValidationFailure first = failure(bound);
        ApplicationValidationFailure second = failure(bound);
        assertEquals(first, second);
        assertEquals(List.of("project.nodes[1].id", "project.nodes[3].id", "project.nodes[4].id"),
                first.findings().stream().map(ApplicationValidationFinding::location).toList());
        assertTrue(first.findings().stream().allMatch(finding -> finding.code().equals("DUPLICATE_NODE_ID")));
    }

    @Test
    void unknown_endpoints_are_all_reported_with_field_locations() {
        List<Node> nodes = List.of(ValidationFixtures.node("N-1", "BUSINESS_RULE"));
        List<Relationship> relationships = List.of(
                ValidationFixtures.relationship("R-1", "missing-from", "DEPENDS_ON", "N-1"),
                ValidationFixtures.relationship("R-2", "N-1", "DEPENDS_ON", "missing-to"),
                ValidationFixtures.relationship("R-3", "missing-a", "DEPENDS_ON", "missing-b"));

        ApplicationValidationFailure failure = failure(ValidationFixtures.bound(nodes, relationships));
        assertEquals(List.of("UNKNOWN_FROM_NODE", "UNKNOWN_FROM_NODE", "UNKNOWN_TO_NODE", "UNKNOWN_TO_NODE"),
                failure.findings().stream().map(ApplicationValidationFinding::code).toList());
        assertEquals(List.of("project.relationships[0].from", "project.relationships[2].from",
                        "project.relationships[1].to", "project.relationships[2].to"),
                failure.findings().stream().map(ApplicationValidationFinding::location).toList());
        assertTrue(failure.findings().stream().noneMatch(
                finding -> finding.code().equals("RELATIONSHIP_NOT_ALLOWED")));
    }

    @Test
    void canonical_allowed_relationship_passes_and_disallowed_relationship_fails() {
        List<Node> nodes = List.of(
                ValidationFixtures.node("US-1", "USER_STORY"),
                ValidationFixtures.node("BO-1", "BUSINESS_OPERATION"));
        ApplicationValidationSuccess success = assertInstanceOf(ApplicationValidationSuccess.class,
                validator.validate(ValidationFixtures.bound(nodes, List.of(
                        ValidationFixtures.relationship("R-1", "US-1", "DESCRIBES", "BO-1")))));
        assertTrue(success.warnings().isEmpty());

        ApplicationValidationFinding finding = failure(ValidationFixtures.bound(nodes, List.of(
                ValidationFixtures.relationship("R-2", "BO-1", "DESCRIBES", "US-1"))))
                .findings().getFirst();
        assertEquals("RELATIONSHIP_NOT_ALLOWED", finding.code());
        assertEquals("project.relationships[0].type", finding.location());
        assertTrue(finding.message().contains("BUSINESS_OPERATION"));
        assertTrue(finding.message().contains("DESCRIBES"));
        assertTrue(finding.message().contains("USER_STORY"));
    }

    @Test
    void canonical_direct_implementation_relationships_are_allowed() {
        Node controller = ValidationFixtures.node("TI-CONTROLLER", "TECHNICAL_IMPLEMENTATION");
        Node service = ValidationFixtures.node("TI-SERVICE", "TECHNICAL_IMPLEMENTATION");
        Node repository = ValidationFixtures.node("TI-REPOSITORY", "TECHNICAL_IMPLEMENTATION");

        assertInstanceOf(ApplicationValidationSuccess.class, validator.validate(ValidationFixtures.bound(
                List.of(controller, service), List.of(ValidationFixtures.relationship(
                        "R-CONTROLLER-SERVICE", "TI-CONTROLLER", "USES", "TI-SERVICE")))));
        assertInstanceOf(ApplicationValidationSuccess.class, validator.validate(ValidationFixtures.bound(
                List.of(service, repository), List.of(ValidationFixtures.relationship(
                        "R-SERVICE-REPOSITORY", "TI-SERVICE", "USES", "TI-REPOSITORY")))));
        assertInstanceOf(ApplicationValidationSuccess.class, validator.validate(ValidationFixtures.bound(
                List.of(controller, service, repository), List.of(
                        ValidationFixtures.relationship(
                                "R-CONTROLLER-SERVICE", "TI-CONTROLLER", "USES", "TI-SERVICE"),
                        ValidationFixtures.relationship(
                                "R-SERVICE-REPOSITORY", "TI-SERVICE", "USES", "TI-REPOSITORY")))));
    }

    @Test
    void canonical_messaging_relationships_between_technical_implementations_are_allowed() {
        Node producer = ValidationFixtures.node("TI-PRODUCER", "TECHNICAL_IMPLEMENTATION");
        Node destination = ValidationFixtures.node("TI-DESTINATION", "TECHNICAL_IMPLEMENTATION");
        Node consumer = ValidationFixtures.node("TI-CONSUMER", "TECHNICAL_IMPLEMENTATION");

        assertInstanceOf(ApplicationValidationSuccess.class, validator.validate(ValidationFixtures.bound(
                List.of(producer, destination), List.of(ValidationFixtures.relationship(
                        "R-PUBLISHES", "TI-PRODUCER", "PUBLISHES_TO", "TI-DESTINATION")))));
        assertInstanceOf(ApplicationValidationSuccess.class, validator.validate(ValidationFixtures.bound(
                List.of(consumer, destination), List.of(ValidationFixtures.relationship(
                        "R-CONSUMES", "TI-CONSUMER", "CONSUMES_FROM", "TI-DESTINATION")))));
    }

    @Test
    void messaging_relationships_reject_unapproved_node_type_combinations() {
        List<Node> nodes = List.of(
                ValidationFixtures.node("TI-1", "TECHNICAL_IMPLEMENTATION"),
                ValidationFixtures.node("TI-2", "TECHNICAL_IMPLEMENTATION"),
                ValidationFixtures.node("BO-1", "BUSINESS_OPERATION"));

        ApplicationValidationFailure failure = failure(ValidationFixtures.bound(nodes, List.of(
                ValidationFixtures.relationship("R-1", "BO-1", "PUBLISHES_TO", "TI-1"),
                ValidationFixtures.relationship("R-2", "TI-1", "PUBLISHES_TO", "BO-1"),
                ValidationFixtures.relationship("R-3", "BO-1", "CONSUMES_FROM", "TI-2"),
                ValidationFixtures.relationship("R-4", "TI-2", "CONSUMES_FROM", "BO-1"))));

        assertEquals(List.of(
                        "RELATIONSHIP_NOT_ALLOWED",
                        "RELATIONSHIP_NOT_ALLOWED",
                        "RELATIONSHIP_NOT_ALLOWED",
                        "RELATIONSHIP_NOT_ALLOWED"),
                failure.findings().stream().map(ApplicationValidationFinding::code).toList());
    }

    @Test
    void other_technical_relationships_remain_disallowed() {
        List<Node> nodes = List.of(
                ValidationFixtures.node("TI-1", "TECHNICAL_IMPLEMENTATION"),
                ValidationFixtures.node("TI-2", "TECHNICAL_IMPLEMENTATION"),
                ValidationFixtures.node("BO-1", "BUSINESS_OPERATION"));

        ApplicationValidationFailure failure = failure(ValidationFixtures.bound(nodes, List.of(
                ValidationFixtures.relationship("R-1", "TI-1", "IMPLEMENTED_BY", "TI-2"),
                ValidationFixtures.relationship("R-2", "TI-1", "USES", "BO-1"))));

        assertEquals(List.of("RELATIONSHIP_NOT_ALLOWED", "RELATIONSHIP_NOT_ALLOWED"),
                failure.findings().stream().map(ApplicationValidationFinding::code).toList());
        assertEquals(List.of("project.relationships[0].type", "project.relationships[1].type"),
                failure.findings().stream().map(ApplicationValidationFinding::location).toList());
    }

    @Test
    void scenario_coverage_is_warning_only_and_deterministic() {
        Node firstScenario = ValidationFixtures.node("SC-1", "SCENARIO");
        Node secondScenario = ValidationFixtures.node("SC-2", "SCENARIO");
        Node test = ValidationFixtures.node("T-1", "TEST_IMPLEMENTATION");
        ApplicationValidationSuccess partiallyCovered = assertInstanceOf(ApplicationValidationSuccess.class,
                validator.validate(ValidationFixtures.bound(List.of(firstScenario, secondScenario, test), List.of(
                        ValidationFixtures.relationship("R-1", "T-1", "VALIDATES", "SC-1")))));
        assertEquals(List.of("SCENARIO_WITHOUT_TEST"),
                partiallyCovered.warnings().stream().map(ApplicationValidationFinding::code).toList());
        assertEquals("project.nodes[1]", partiallyCovered.warnings().getFirst().location());

        ApplicationValidationSuccess uncovered = assertInstanceOf(ApplicationValidationSuccess.class,
                validator.validate(ValidationFixtures.bound(List.of(firstScenario, secondScenario), List.of())));
        assertEquals(List.of("project.nodes[0]", "project.nodes[1]"),
                uncovered.warnings().stream().map(ApplicationValidationFinding::location).toList());
    }

    @Test
    void mixed_errors_and_warnings_return_one_failure_without_proof() {
        List<Node> nodes = List.of(
                ValidationFixtures.node("SC-1", "SCENARIO"),
                ValidationFixtures.node("BR-1", "BUSINESS_RULE"));
        ApplicationValidationFailure failure = failure(ValidationFixtures.bound(nodes, List.of(
                ValidationFixtures.relationship("R-1", "BR-1", "DEPENDS_ON", "missing"))));
        assertEquals(List.of("UNKNOWN_TO_NODE", "SCENARIO_WITHOUT_TEST"),
                failure.findings().stream().map(ApplicationValidationFinding::code).toList());
        assertEquals(List.of(ApplicationValidationSeverity.ERROR, ApplicationValidationSeverity.WARNING),
                failure.findings().stream().map(ApplicationValidationFinding::severity).toList());
    }

    @Test
    void duplicate_endpoint_ids_skip_ambiguous_compatibility_without_throwing() {
        List<Node> nodes = List.of(
                ValidationFixtures.node("N-1", "USER_STORY"),
                ValidationFixtures.node("N-1", "BUSINESS_OPERATION"),
                ValidationFixtures.node("N-2", "CHECK"));
        ApplicationValidationFailure failure = failure(ValidationFixtures.bound(nodes, List.of(
                ValidationFixtures.relationship("R-1", "N-1", "DESCRIBES", "N-2"))));
        assertEquals(List.of("DUPLICATE_NODE_ID"),
                failure.findings().stream().map(ApplicationValidationFinding::code).toList());
    }

    @Test
    void null_document_is_a_programmer_error() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    private ApplicationValidationFailure failure(BoundProjectDocument document) {
        return assertInstanceOf(ApplicationValidationFailure.class, validator.validate(document));
    }
}
