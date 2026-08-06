package ru.kuznetsov.qaip.core.application.query.eventpath;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventPathContractsTest {
    @Test
    void successful_result_preserves_identity_kind_and_exact_ordered_role_sequence() {
        EventPathResult path = path(steps());
        EventPathFound found = new EventPathFound(path);

        assertEquals("P-1", found.path().projectId());
        assertEquals("OP-1", found.path().operationId());
        assertEquals(EventPathKind.EVENT_DRIVEN, found.path().pathKind());
        assertEquals(List.of(
                        EventPathImplementationRole.REST_CONTROLLER,
                        EventPathImplementationRole.MESSAGE_PRODUCER,
                        EventPathImplementationRole.MESSAGE_DESTINATION,
                        EventPathImplementationRole.MESSAGE_CONSUMER,
                        EventPathImplementationRole.APPLICATION_SERVICE,
                        EventPathImplementationRole.REPOSITORY),
                found.path().steps().stream().map(EventPathStep::implementationRole).toList());
        assertEquals("Kafka", found.path().steps().get(1).technology());
        assertEquals(null, found.path().steps().get(4).technology());
    }

    @Test
    void successful_result_defensively_copies_and_exposes_immutable_steps() {
        List<EventPathStep> mutable = new ArrayList<>(steps());
        EventPathResult result = path(mutable);
        mutable.clear();

        assertEquals(6, result.steps().size());
        assertThrows(UnsupportedOperationException.class, () -> result.steps().clear());
    }

    @Test
    void successful_result_rejects_incomplete_or_wrong_ordered_paths() {
        assertThrows(IllegalArgumentException.class, () -> path(steps().subList(0, 5)));
        List<EventPathStep> reversed = new ArrayList<>(steps());
        java.util.Collections.swap(reversed, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> path(reversed));
    }

    @Test
    void unavailable_outcomes_are_typed_values_and_validate_query_identity() {
        assertEquals(new EventPathProjectNotFound("P"), new EventPathProjectNotFound("P"));
        assertEquals(new EventPathOperationNotFound("P", "OP"),
                new EventPathOperationNotFound("P", "OP"));
        assertEquals(new EventPathIncomplete("P", "OP"), new EventPathIncomplete("P", "OP"));
        assertEquals(new EventPathAmbiguous("P", "OP"), new EventPathAmbiguous("P", "OP"));
        assertEquals(new EventPathNotEventDriven("P", "OP"), new EventPathNotEventDriven("P", "OP"));
        assertThrows(IllegalArgumentException.class, () -> new EventPathProjectNotFound(" "));
        assertThrows(IllegalArgumentException.class, () -> new EventPathIncomplete("P", "\t"));
    }

    @Test
    void result_hierarchy_has_exact_expected_outcomes() {
        assertTrue(EventPathQueryResult.class.isSealed());
        assertEquals(Set.of(EventPathFound.class, EventPathProjectNotFound.class,
                        EventPathOperationNotFound.class, EventPathIncomplete.class,
                        EventPathAmbiguous.class, EventPathNotEventDriven.class),
                Arrays.stream(EventPathQueryResult.class.getPermittedSubclasses()).collect(Collectors.toSet()));
    }

    @Test
    void query_boundary_is_project_and_business_operation_identity_only() throws Exception {
        Method execute = EventPathQuery.class.getMethod("execute", String.class, String.class);

        assertEquals(EventPathQueryResult.class, execute.getReturnType());
        assertEquals(List.of(String.class, String.class), List.of(execute.getParameterTypes()));
        assertEquals(1, EventPathQuery.class.getMethods().length);
    }

    @Test
    void contracts_have_no_framework_explorer_or_persistence_types() {
        List<Class<?>> contracts = List.of(EventPathQuery.class, EventPathQueryResult.class,
                EventPathResult.class, EventPathStep.class, EventPathFound.class,
                EventPathProjectNotFound.class, EventPathOperationNotFound.class,
                EventPathIncomplete.class, EventPathAmbiguous.class, EventPathNotEventDriven.class);

        contracts.stream().flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .flatMap(method -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes())))
                .map(Class::getName)
                .forEach(name -> {
                    assertFalse(name.startsWith("org.springframework"));
                    assertFalse(name.startsWith("com.fasterxml.jackson"));
                    assertFalse(name.contains("explorer"));
                    assertFalse(name.contains("persistence"));
                });
    }

    @Test
    void existing_synchronous_operation_details_contract_remains_available() {
        OperationDetailsResult synchronous = new OperationDetailsResult(
                "OP", "POST", "/orders", "POST /orders", 1, 2,
                "OrdersController.create", "OrderService.create", "OrderRepository");

        assertInstanceOf(OperationDetailsResult.class, synchronous);
        assertEquals("OrderService.create", synchronous.serviceName());
        assertEquals(9, OperationDetailsResult.class.getRecordComponents().length);
    }

    private static EventPathResult path(List<EventPathStep> steps) {
        return new EventPathResult("P-1", "OP-1", EventPathKind.EVENT_DRIVEN, steps);
    }

    private static List<EventPathStep> steps() {
        return List.of(
                step("CONTROLLER", EventPathImplementationRole.REST_CONTROLLER,
                        "OrdersController.create", EventPathImplementationType.API, null),
                step("PRODUCER", EventPathImplementationRole.MESSAGE_PRODUCER,
                        "OrdersController.create", EventPathImplementationType.MESSAGE, "Kafka"),
                step("DESTINATION", EventPathImplementationRole.MESSAGE_DESTINATION,
                        "orders.created", EventPathImplementationType.MESSAGE, "Kafka"),
                step("CONSUMER", EventPathImplementationRole.MESSAGE_CONSUMER,
                        "OrderCreatedListener.listen", EventPathImplementationType.MESSAGE, "Kafka"),
                step("SERVICE", EventPathImplementationRole.APPLICATION_SERVICE,
                        "OrderProcessingService.process", EventPathImplementationType.OTHER, null),
                step("REPOSITORY", EventPathImplementationRole.REPOSITORY,
                        "ProcessedOrderRepository", EventPathImplementationType.DATABASE, null));
    }

    private static EventPathStep step(
            String id, EventPathImplementationRole role, String name,
            EventPathImplementationType type, String technology) {
        return new EventPathStep(id, role, name, type, technology);
    }
}
