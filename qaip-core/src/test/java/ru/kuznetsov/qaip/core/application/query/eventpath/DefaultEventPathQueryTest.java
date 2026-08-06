package ru.kuznetsov.qaip.core.application.query.eventpath;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultEventPathQueryTest {
    @Test
    void resolves_complete_ordered_path_with_reverse_consumer_lookup_and_minimal_projection() {
        EventPathFound found = assertInstanceOf(EventPathFound.class, query(complete()).execute("P", "OP"));

        assertEquals(EventPathKind.EVENT_DRIVEN, found.path().pathKind());
        assertEquals(List.of("C", "P", "D", "M", "S", "R"),
                found.path().steps().stream().map(EventPathStep::nodeId).toList());
        assertEquals(List.of(EventPathImplementationRole.REST_CONTROLLER,
                        EventPathImplementationRole.MESSAGE_PRODUCER,
                        EventPathImplementationRole.MESSAGE_DESTINATION,
                        EventPathImplementationRole.MESSAGE_CONSUMER,
                        EventPathImplementationRole.APPLICATION_SERVICE,
                        EventPathImplementationRole.REPOSITORY),
                found.path().steps().stream().map(EventPathStep::implementationRole).toList());
        assertEquals(List.of("Controller.create", "Controller.create", "orders.created",
                        "Listener.listen", "Service.process", "Repository"),
                found.path().steps().stream().map(EventPathStep::name).toList());
        assertEquals(List.of(EventPathImplementationType.API, EventPathImplementationType.MESSAGE,
                        EventPathImplementationType.MESSAGE, EventPathImplementationType.MESSAGE,
                        EventPathImplementationType.OTHER, EventPathImplementationType.DATABASE),
                found.path().steps().stream().map(EventPathStep::implementationType).toList());
        assertEquals(java.util.Arrays.asList(null, "Kafka", "Kafka", "Kafka", null, null),
                found.path().steps().stream().map(EventPathStep::technology).toList());
        assertThrows(UnsupportedOperationException.class, () -> found.path().steps().clear());
    }

    @Test
    void repeated_and_shuffled_graph_queries_are_equal() {
        Project original = complete();
        List<Node> nodes = new ArrayList<>(original.nodes());
        List<Relationship> relationships = new ArrayList<>(original.relationships());
        Collections.reverse(nodes);
        Collections.reverse(relationships);
        EventPathQuery first = query(original);

        assertEquals(first.execute("P", "OP"), first.execute("P", "OP"));
        assertEquals(first.execute("P", "OP"), query(project(nodes, relationships)).execute("P", "OP"));
    }

    @Test
    void returns_project_and_operation_not_found() {
        assertEquals(new EventPathProjectNotFound("missing"),
                new DefaultEventPathQuery(id -> Optional.empty()).execute("missing", "OP"));
        assertEquals(new EventPathOperationNotFound("P", "missing"),
                query(complete()).execute("P", "missing"));
    }

    @Test
    void synchronous_or_missing_producer_is_not_event_driven() {
        Project complete = complete();
        assertEquals(new EventPathNotEventDriven("P", "OP"),
                query(without(complete, "C-P")).execute("P", "OP"));
        List<Node> nodes = new ArrayList<>(complete.nodes());
        nodes.add(technical("SYNC-S", "SyncService", EventPathImplementationRole.APPLICATION_SERVICE,
                EventPathImplementationType.OTHER, null, Map.of("flowStage", "SERVICE")));
        List<Relationship> relationships = new ArrayList<>(without(complete, "C-P").relationships());
        relationships.add(relationship("C-SYNC", "C", "USES", "SYNC-S"));
        assertEquals(new EventPathNotEventDriven("P", "OP"),
                query(project(nodes, relationships)).execute("P", "OP"));
    }

    @Test
    void first_missing_stage_after_producer_is_incomplete() {
        Project complete = complete();
        for (String relationship : List.of("P-D", "M-D", "M-S", "S-R")) {
            assertEquals(new EventPathIncomplete("P", "OP"),
                    query(without(complete, relationship)).execute("P", "OP"), relationship);
        }
        assertEquals(new EventPathIncomplete("P", "OP"),
                query(without(complete, "OP-C")).execute("P", "OP"));
    }

    @Test
    void every_multiple_required_stage_is_ambiguous() {
        assertAmbiguous("C2", EventPathImplementationRole.REST_CONTROLLER,
                EventPathImplementationType.API, "OP", "IMPLEMENTED_BY", false);
        assertAmbiguous("P2", EventPathImplementationRole.MESSAGE_PRODUCER,
                EventPathImplementationType.MESSAGE, "C", "USES", false);
        assertAmbiguous("D2", EventPathImplementationRole.MESSAGE_DESTINATION,
                EventPathImplementationType.MESSAGE, "P", "PUBLISHES_TO", false);
        assertAmbiguous("M2", EventPathImplementationRole.MESSAGE_CONSUMER,
                EventPathImplementationType.MESSAGE, "D", "CONSUMES_FROM", true);
        assertAmbiguous("S2", EventPathImplementationRole.APPLICATION_SERVICE,
                EventPathImplementationType.OTHER, "M", "USES", false);
        assertAmbiguous("R2", EventPathImplementationRole.REPOSITORY,
                EventPathImplementationType.DATABASE, "S", "USES", false);
    }

    @Test
    void wrong_direction_type_role_and_non_technical_nodes_do_not_qualify() {
        Project complete = complete();
        assertEquals(new EventPathNotEventDriven("P", "OP"), query(replaceRelationship(
                complete, "C-P", relationship("WRONG-DIRECTION", "P", "USES", "C"))).execute("P", "OP"));
        assertEquals(new EventPathNotEventDriven("P", "OP"), query(replaceRelationship(
                complete, "C-P", relationship("WRONG-TYPE", "C", "RELATED_TO", "P"))).execute("P", "OP"));

        List<Node> wrongRole = replaceNode(complete.nodes(), technical("P", "Controller.create",
                EventPathImplementationRole.APPLICATION_SERVICE, EventPathImplementationType.MESSAGE,
                "Kafka", Map.of()));
        assertEquals(new EventPathNotEventDriven("P", "OP"),
                query(project(wrongRole, complete.relationships())).execute("P", "OP"));

        List<Node> nonTechnical = replaceNode(complete.nodes(), node("P", "BUSINESS_RULE", "Producer"));
        assertEquals(new EventPathNotEventDriven("P", "OP"),
                query(project(nonTechnical, complete.relationships())).execute("P", "OP"));
    }

    @Test
    void implementation_type_or_flow_stage_alone_and_unrelated_nodes_do_not_qualify() {
        Project complete = complete();
        Node withoutRole = technicalWithoutRole("P", "Producer", EventPathImplementationType.MESSAGE,
                Map.of("flowStage", "PRODUCER"));
        assertEquals(new EventPathNotEventDriven("P", "OP"), query(project(
                replaceNode(complete.nodes(), withoutRole), complete.relationships())).execute("P", "OP"));

        List<Node> nodes = new ArrayList<>(complete.nodes());
        nodes.add(technical("UNRELATED", "OtherProducer", EventPathImplementationRole.MESSAGE_PRODUCER,
                EventPathImplementationType.MESSAGE, "Kafka", Map.of()));
        assertInstanceOf(EventPathFound.class, query(project(nodes, complete.relationships())).execute("P", "OP"));
    }

    private static void assertAmbiguous(
            String id, EventPathImplementationRole role, EventPathImplementationType type,
            String adjacent, String relationshipType, boolean reverse) {
        Project complete = complete();
        List<Node> nodes = new ArrayList<>(complete.nodes());
        nodes.add(technical(id, id, role, type, type == EventPathImplementationType.MESSAGE ? "Kafka" : null,
                Map.of()));
        List<Relationship> relationships = new ArrayList<>(complete.relationships());
        relationships.add(reverse
                ? relationship("EXTRA-" + id, id, relationshipType, adjacent)
                : relationship("EXTRA-" + id, adjacent, relationshipType, id));
        assertEquals(new EventPathAmbiguous("P", "OP"),
                query(project(nodes, relationships)).execute("P", "OP"));
    }

    private static EventPathQuery query(Project project) {
        return new DefaultEventPathQuery(id -> Optional.of(project));
    }

    private static Project complete() {
        List<Node> nodes = List.of(
                node("OP", "BUSINESS_OPERATION", "POST /orders"),
                technical("C", "Controller.create", EventPathImplementationRole.REST_CONTROLLER,
                        EventPathImplementationType.API, null, Map.of()),
                technical("P", "Controller.create", EventPathImplementationRole.MESSAGE_PRODUCER,
                        EventPathImplementationType.MESSAGE, "Kafka", Map.of()),
                technical("D", "orders.created", EventPathImplementationRole.MESSAGE_DESTINATION,
                        EventPathImplementationType.MESSAGE, "Kafka", Map.of()),
                technical("M", "Listener.listen", EventPathImplementationRole.MESSAGE_CONSUMER,
                        EventPathImplementationType.MESSAGE, "Kafka", Map.of()),
                technical("S", "Service.process", EventPathImplementationRole.APPLICATION_SERVICE,
                        EventPathImplementationType.OTHER, null, Map.of()),
                technical("R", "Repository", EventPathImplementationRole.REPOSITORY,
                        EventPathImplementationType.DATABASE, null, Map.of()));
        List<Relationship> relationships = List.of(
                relationship("OP-C", "OP", "IMPLEMENTED_BY", "C"),
                relationship("C-P", "C", "USES", "P"),
                relationship("P-D", "P", "PUBLISHES_TO", "D"),
                relationship("M-D", "M", "CONSUMES_FROM", "D"),
                relationship("M-S", "M", "USES", "S"),
                relationship("S-R", "S", "USES", "R"));
        return project(nodes, relationships);
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P", "Project", null, null, Map.of()),
                List.of(), new Subject("OP"), nodes, relationships,
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Project without(Project project, String relationshipId) {
        return project(project.nodes(), project.relationships().stream()
                .filter(value -> !relationshipId.equals(value.id())).toList());
    }

    private static Project replaceRelationship(Project project, String id, Relationship replacement) {
        List<Relationship> values = new ArrayList<>(project.relationships().stream()
                .filter(value -> !id.equals(value.id())).toList());
        values.add(replacement);
        return project(project.nodes(), values);
    }

    private static List<Node> replaceNode(List<Node> nodes, Node replacement) {
        List<Node> values = new ArrayList<>(nodes.stream()
                .filter(value -> !replacement.id().equals(value.id())).toList());
        values.add(replacement);
        return values;
    }

    private static Node technical(
            String id, String name, EventPathImplementationRole role, EventPathImplementationType type,
            String technology, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new java.util.LinkedHashMap<>(additionalDetails);
        if (technology != null) details.put("technology", technology);
        return new Node(id, "TECHNICAL_IMPLEMENTATION", name, null, "CONFIRMED", List.of(), List.of(), Map.of(),
                Map.of("technicalImplementation", Map.of("implementationRole", role.name(),
                        "implementationType", type.name(), "system", "system", "details", details)));
    }

    private static Node technicalWithoutRole(
            String id, String name, EventPathImplementationType type, Map<String, Object> details) {
        return new Node(id, "TECHNICAL_IMPLEMENTATION", name, null, "CONFIRMED", List.of(), List.of(), Map.of(),
                Map.of("technicalImplementation", Map.of("implementationType", type.name(),
                        "system", "system", "details", details)));
    }

    private static Node node(String id, String type, String name) {
        return new Node(id, type, name, null, "CONFIRMED", List.of(), List.of(), Map.of(), Map.of());
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }
}
