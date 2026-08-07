package ru.kuznetsov.qaip.core.application.query.operationoverview;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationlist.DefaultOperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.application.query.operationdetails.DefaultOperationDetailsQuery;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailable;
import ru.kuznetsov.qaip.core.application.query.eventpath.DefaultEventPathQuery;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathAmbiguous;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationRole;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationType;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathIncomplete;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathNotEventDriven;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultOperationOverviewQueryTest {
    @Test
    void returns_project_not_found_after_exactly_one_read() {
        CountingProjectReader reader = new CountingProjectReader(Optional.empty());

        var result = new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1");

        assertEquals(new OperationOverviewProjectNotFound("P-1"), result);
        assertEquals(1, reader.readCount());
    }

    @Test
    void returns_operation_not_found_from_the_single_snapshot() {
        CountingProjectReader reader = new CountingProjectReader(Optional.of(project(operation("OP-OTHER"))));

        var result = new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1");

        assertEquals(new OperationOverviewOperationNotFound("P-1", "OP-1"), result);
        assertEquals(1, reader.readCount());
    }

    @Test
    void resolves_identity_and_transitional_sections_from_one_snapshot() {
        CountingProjectReader reader = new CountingProjectReader(Optional.of(project(operation("OP-1"))));

        OperationOverviewFound found = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));

        assertEquals(new OperationOverviewIdentity(
                "P-1", "OP-1", "POST", "/orders", "POST /orders"), found.identity());
        assertInstanceOf(OperationOverviewImplementationIncomplete.class, found.implementation());
        assertInstanceOf(OperationOverviewEventPathIncomplete.class, found.eventPath());
        OperationOverviewVerificationAvailable verification = assertInstanceOf(
                OperationOverviewVerificationAvailable.class, found.verification());
        assertEquals(0, verification.testCount());
        assertEquals(0, verification.checkCount());
        assertEquals(List.of(), verification.tests());
        assertEquals(1, reader.readCount());
    }

    @Test
    void repeated_resolution_is_deterministic() {
        Project snapshot = project(operation("OP-1"));
        OperationOverviewQuery query = new DefaultOperationOverviewQuery(id -> Optional.of(snapshot));

        OperationOverviewQueryResult first = query.execute("P-1", "OP-1");
        OperationOverviewQueryResult second = query.execute("P-1", "OP-1");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void identity_values_match_the_existing_operation_list_semantics() {
        Project snapshot = project(operation("OP-1"));
        ProjectReader reader = id -> Optional.of(snapshot);
        OperationOverviewFound overview = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));
        OperationListFound list = assertInstanceOf(OperationListFound.class,
                new DefaultOperationListQuery(reader, new OperationListProjector()).execute("P-1"));
        var existing = list.operations().getFirst();

        assertEquals(existing.operationId(), overview.identity().operationId());
        assertEquals(existing.method(), overview.identity().method());
        assertEquals(existing.path(), overview.identity().path());
        assertEquals(existing.displayName(), overview.identity().displayName());
    }

    @Test
    void resolves_available_conventional_path_in_controller_service_repository_order() {
        Project snapshot = synchronousProject();
        CountingProjectReader reader = new CountingProjectReader(Optional.of(snapshot));

        OperationOverviewFound found = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));
        OperationOverviewImplementationAvailable available = assertInstanceOf(
                OperationOverviewImplementationAvailable.class, found.implementation());

        assertEquals(new OperationOverviewImplementation(
                "OrdersController.create", "OrderService.create", "OrderRepository"),
                available.implementation());
        assertEquals(1, reader.readCount());
        assertInstanceOf(OperationOverviewEventPathNotApplicable.class, found.eventPath());
        assertEquals(0, ((OperationOverviewVerificationAvailable) found.verification()).testCount());
    }

    @Test
    void maps_incomplete_and_ambiguous_conventional_paths_without_affecting_other_sections() {
        OperationOverviewFound incomplete = overview(project(operation("OP-1")));
        Project ambiguousProject = project(
                List.of(operation("OP-1"), technical("C-1", "ControllerOne", null),
                        technical("C-2", "ControllerTwo", null)),
                List.of(relationship("OP-C-1", "OP-1", "IMPLEMENTED_BY", "C-1"),
                        relationship("OP-C-2", "OP-1", "IMPLEMENTED_BY", "C-2")));
        OperationOverviewFound ambiguous = overview(ambiguousProject);

        assertInstanceOf(OperationOverviewImplementationIncomplete.class, incomplete.implementation());
        assertInstanceOf(OperationOverviewImplementationAmbiguous.class, ambiguous.implementation());
        for (OperationOverviewFound found : List.of(incomplete, ambiguous)) {
            assertEquals(0, ((OperationOverviewVerificationAvailable) found.verification()).testCount());
        }
        assertInstanceOf(OperationOverviewEventPathIncomplete.class, incomplete.eventPath());
        assertInstanceOf(OperationOverviewEventPathIncomplete.class, ambiguous.eventPath());
    }

    @Test
    void available_and_unavailable_values_match_existing_operation_details_semantics() {
        Project availableProject = synchronousProject();
        assertSameImplementation(availableProject);
        assertSameImplementation(project(operation("OP-1")));
        Project ambiguousProject = project(
                List.of(operation("OP-1"), technical("C-1", "ControllerOne", null),
                        technical("C-2", "ControllerTwo", null)),
                List.of(relationship("OP-C-1", "OP-1", "IMPLEMENTED_BY", "C-1"),
                        relationship("OP-C-2", "OP-1", "IMPLEMENTED_BY", "C-2")));
        assertSameImplementation(ambiguousProject);
    }

    @Test
    void event_driven_post_orders_preserves_available_six_stage_path_and_partial_implementation() {
        Project eventDriven = eventDrivenProject();
        CountingProjectReader reader = new CountingProjectReader(Optional.of(eventDriven));

        OperationOverviewFound found = assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(reader).execute("P-1", "OP-1"));

        assertEquals("POST /api/orders", found.identity().displayName());
        assertInstanceOf(OperationOverviewImplementationIncomplete.class, found.implementation());
        OperationOverviewEventPathAvailable available = assertInstanceOf(
                OperationOverviewEventPathAvailable.class, found.eventPath());
        assertEquals(List.of("CONTROLLER", "PRODUCER", "DESTINATION", "CONSUMER", "EVENT-SERVICE",
                        "EVENT-REPOSITORY"),
                available.path().steps().stream().map(step -> step.nodeId()).toList());
        assertEquals(0, ((OperationOverviewVerificationAvailable) found.verification()).testCount());
        assertEquals(1, reader.readCount());
    }

    @Test
    void maps_incomplete_and_ambiguous_event_paths() {
        OperationOverviewFound incomplete = overview(project(
                List.of(operation("OP-1"), eventTechnical("CONTROLLER", "Controller",
                        EventPathImplementationRole.REST_CONTROLLER, EventPathImplementationType.API)),
                List.of(relationship("OP-C", "OP-1", "IMPLEMENTED_BY", "CONTROLLER"))));
        Project ambiguousProject = project(
                List.of(operation("OP-1"),
                        eventTechnical("C-1", "ControllerOne", EventPathImplementationRole.REST_CONTROLLER,
                                EventPathImplementationType.API),
                        eventTechnical("C-2", "ControllerTwo", EventPathImplementationRole.REST_CONTROLLER,
                                EventPathImplementationType.API)),
                List.of(relationship("OP-C-1", "OP-1", "IMPLEMENTED_BY", "C-1"),
                        relationship("OP-C-2", "OP-1", "IMPLEMENTED_BY", "C-2")));
        OperationOverviewFound ambiguous = overview(ambiguousProject);

        assertInstanceOf(OperationOverviewEventPathNotApplicable.class, incomplete.eventPath());
        assertInstanceOf(OperationOverviewEventPathAmbiguous.class, ambiguous.eventPath());
    }

    @Test
    void event_path_values_and_states_match_existing_query_semantics() {
        assertSameEventPath(eventDrivenProject());
        assertSameEventPath(synchronousProject());
        Project incomplete = project(
                List.of(operation("OP-1"),
                        eventTechnical("CONTROLLER", "Controller", EventPathImplementationRole.REST_CONTROLLER,
                                EventPathImplementationType.API),
                        eventTechnical("PRODUCER", "Producer", EventPathImplementationRole.MESSAGE_PRODUCER,
                                EventPathImplementationType.MESSAGE)),
                List.of(relationship("OP-C", "OP-1", "IMPLEMENTED_BY", "CONTROLLER"),
                        relationship("C-P", "CONTROLLER", "USES", "PRODUCER")));
        assertSameEventPath(incomplete);
    }

    private static Project project(Node operation) {
        return project(List.of(operation), List.of());
    }

    private static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("contract", "schema", new Metadata("P-1", "Project", null, null, Map.of()),
                List.of(), new Subject("OP-1"), nodes, relationships,
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static Project synchronousProject() {
        return project(
                List.of(operation("OP-1"),
                        eventTechnical("CONTROLLER", "OrdersController.create",
                                EventPathImplementationRole.REST_CONTROLLER, EventPathImplementationType.API),
                        technical("SERVICE", "OrderService.create", "SERVICE"),
                        technical("REPOSITORY", "OrderRepository", "REPOSITORY")),
                List.of(relationship("OP-C", "OP-1", "IMPLEMENTED_BY", "CONTROLLER"),
                        relationship("C-S", "CONTROLLER", "USES", "SERVICE"),
                        relationship("S-R", "SERVICE", "USES", "REPOSITORY")));
    }

    private static Node technical(String id, String name, String stage) {
        Map<String, Object> details = stage == null ? Map.of() : Map.of("flowStage", stage);
        return new Node(id, "TECHNICAL_IMPLEMENTATION", name, null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("technicalImplementation", Map.of(
                "implementationType", "OTHER", "system", "orders", "details", details)));
    }

    private static Node eventTechnical(
            String id,
            String name,
            EventPathImplementationRole role,
            EventPathImplementationType type
    ) {
        return new Node(id, "TECHNICAL_IMPLEMENTATION", name, null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("technicalImplementation", Map.of(
                "implementationRole", role.name(), "implementationType", type.name(),
                "system", "orders", "details", Map.of("technology", "Kafka"))));
    }

    private static Project eventDrivenProject() {
        return project(
                List.of(new Node("OP-1", "BUSINESS_OPERATION", "POST /api/orders", null, "CONFIRMED",
                                List.of(), List.of(), Map.of(), Map.of()),
                        eventTechnical("CONTROLLER", "OrdersController.create",
                                EventPathImplementationRole.REST_CONTROLLER, EventPathImplementationType.API),
                        eventTechnical("PRODUCER", "OrdersController.create",
                                EventPathImplementationRole.MESSAGE_PRODUCER, EventPathImplementationType.MESSAGE),
                        eventTechnical("DESTINATION", "orders.created",
                                EventPathImplementationRole.MESSAGE_DESTINATION,
                                EventPathImplementationType.MESSAGE),
                        eventTechnical("CONSUMER", "OrderCreatedListener.listen",
                                EventPathImplementationRole.MESSAGE_CONSUMER,
                                EventPathImplementationType.MESSAGE),
                        eventTechnical("EVENT-SERVICE", "OrderService.process",
                                EventPathImplementationRole.APPLICATION_SERVICE,
                                EventPathImplementationType.OTHER),
                        eventTechnical("EVENT-REPOSITORY", "OrderRepository",
                                EventPathImplementationRole.REPOSITORY,
                                EventPathImplementationType.DATABASE)),
                List.of(relationship("OP-C", "OP-1", "IMPLEMENTED_BY", "CONTROLLER"),
                        relationship("C-P", "CONTROLLER", "USES", "PRODUCER"),
                        relationship("P-D", "PRODUCER", "PUBLISHES_TO", "DESTINATION"),
                        relationship("M-D", "CONSUMER", "CONSUMES_FROM", "DESTINATION"),
                        relationship("M-S", "CONSUMER", "USES", "EVENT-SERVICE"),
                        relationship("S-R", "EVENT-SERVICE", "USES", "EVENT-REPOSITORY")));
    }

    private static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    private static OperationOverviewFound overview(Project project) {
        return assertInstanceOf(OperationOverviewFound.class,
                new DefaultOperationOverviewQuery(id -> Optional.of(project)).execute("P-1", "OP-1"));
    }

    private static void assertSameImplementation(Project project) {
        ProjectReader reader = id -> Optional.of(project);
        var overview = overview(project).implementation();
        var details = new DefaultOperationDetailsQuery(reader, new OperationListProjector())
                .execute("P-1", "OP-1");
        if (details instanceof OperationDetailsFound found) {
            var available = assertInstanceOf(OperationOverviewImplementationAvailable.class, overview);
            assertEquals(List.of(found.details().controllerName(), found.details().serviceName(),
                            found.details().repositoryName()),
                    List.of(available.implementation().controllerName(),
                            available.implementation().serviceName(),
                            available.implementation().repositoryName()));
        } else {
            var unavailable = assertInstanceOf(OperationDetailsUnavailable.class, details);
            assertEquals(unavailable.reason().name().replace("_PATH", ""), overview.state().name());
        }
    }

    private static void assertSameEventPath(Project project) {
        ProjectReader reader = id -> Optional.of(project);
        var overview = overview(project).eventPath();
        var specialized = new DefaultEventPathQuery(reader).execute("P-1", "OP-1");
        if (specialized instanceof EventPathFound found) {
            assertEquals(found.path(),
                    assertInstanceOf(OperationOverviewEventPathAvailable.class, overview).path());
        } else if (specialized instanceof EventPathNotEventDriven) {
            assertInstanceOf(OperationOverviewEventPathNotApplicable.class, overview);
        } else if (specialized instanceof EventPathIncomplete) {
            assertInstanceOf(OperationOverviewEventPathIncomplete.class, overview);
        } else {
            assertInstanceOf(EventPathAmbiguous.class, specialized);
            assertInstanceOf(OperationOverviewEventPathAmbiguous.class, overview);
        }
    }

    private static Node operation(String id) {
        return new Node(id, "BUSINESS_OPERATION", "POST /orders", null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of("operation", Map.of("code", id)));
    }

    private static final class CountingProjectReader implements ProjectReader {
        private final Optional<Project> result;
        private int readCount;

        private CountingProjectReader(Optional<Project> result) {
            this.result = result;
        }

        @Override
        public Optional<Project> findById(String projectId) {
            readCount++;
            return result;
        }

        private int readCount() {
            return readCount;
        }
    }
}
