package ru.kuznetsov.qaip.core.application.query.eventpath;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class DefaultEventPathQuery implements EventPathQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";
    private static final String TECHNICAL_IMPLEMENTATION = "TECHNICAL_IMPLEMENTATION";
    private static final String IMPLEMENTED_BY = "IMPLEMENTED_BY";
    private static final String USES = "USES";
    private static final String PUBLISHES_TO = "PUBLISHES_TO";
    private static final String CONSUMES_FROM = "CONSUMES_FROM";

    private final ProjectReader projectReader;

    public DefaultEventPathQuery(ProjectReader projectReader) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
    }

    @Override
    public EventPathQueryResult execute(String projectId, String operationId) {
        String requestedProjectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        String requestedOperationId = EventPathProjectNotFound.requireId(operationId, "operationId");
        var project = Objects.requireNonNull(
                projectReader.findById(requestedProjectId), "project reader result");
        if (project.isEmpty()) return new EventPathProjectNotFound(requestedProjectId);
        Project value = project.orElseThrow();
        if (value.nodes().stream().noneMatch(node ->
                requestedOperationId.equals(node.id()) && BUSINESS_OPERATION.equals(node.type()))) {
            return new EventPathOperationNotFound(requestedProjectId, requestedOperationId);
        }

        Graph graph = new Graph(value);
        Resolution controller = graph.forward(requestedOperationId, IMPLEMENTED_BY,
                EventPathImplementationRole.REST_CONTROLLER);
        EventPathQueryResult unavailable = unavailable(
                requestedProjectId, requestedOperationId, controller, false);
        if (unavailable != null) return unavailable;

        Resolution producer = graph.forward(controller.node().id(), USES,
                EventPathImplementationRole.MESSAGE_PRODUCER);
        unavailable = unavailable(requestedProjectId, requestedOperationId, producer, true);
        if (unavailable != null) return unavailable;

        Resolution destination = graph.forward(producer.node().id(), PUBLISHES_TO,
                EventPathImplementationRole.MESSAGE_DESTINATION);
        unavailable = unavailable(requestedProjectId, requestedOperationId, destination, false);
        if (unavailable != null) return unavailable;

        Resolution consumer = graph.reverse(destination.node().id(), CONSUMES_FROM,
                EventPathImplementationRole.MESSAGE_CONSUMER);
        unavailable = unavailable(requestedProjectId, requestedOperationId, consumer, false);
        if (unavailable != null) return unavailable;

        Resolution service = graph.forward(consumer.node().id(), USES,
                EventPathImplementationRole.APPLICATION_SERVICE);
        unavailable = unavailable(requestedProjectId, requestedOperationId, service, false);
        if (unavailable != null) return unavailable;

        Resolution repository = graph.forward(service.node().id(), USES,
                EventPathImplementationRole.REPOSITORY);
        unavailable = unavailable(requestedProjectId, requestedOperationId, repository, false);
        if (unavailable != null) return unavailable;

        return new EventPathFound(new EventPathResult(
                requestedProjectId, requestedOperationId, EventPathKind.EVENT_DRIVEN,
                List.of(step(controller.node()), step(producer.node()), step(destination.node()),
                        step(consumer.node()), step(service.node()), step(repository.node()))));
    }

    private static EventPathQueryResult unavailable(
            String projectId, String operationId, Resolution resolution, boolean producerStage) {
        if (resolution.status() == ResolutionStatus.AMBIGUOUS) {
            return new EventPathAmbiguous(projectId, operationId);
        }
        if (resolution.status() == ResolutionStatus.MISSING) {
            return producerStage
                    ? new EventPathNotEventDriven(projectId, operationId)
                    : new EventPathIncomplete(projectId, operationId);
        }
        return null;
    }

    private static EventPathStep step(Node node) {
        Map<?, ?> technical = technical(node);
        String role = requiredText(technical.get("implementationRole"), "implementationRole", node.id());
        String type = requiredText(technical.get("implementationType"), "implementationType", node.id());
        String technology = null;
        Object details = technical.get("details");
        if (details instanceof Map<?, ?> detailMap && detailMap.get("technology") instanceof String value
                && !value.isBlank()) technology = value;
        return new EventPathStep(node.id(), EventPathImplementationRole.valueOf(role),
                requiredText(node.name(), "name", node.id()), EventPathImplementationType.valueOf(type), technology);
    }

    private static String requiredText(Object value, String field, String nodeId) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("qualified node " + nodeId + " has no " + field);
        }
        return text;
    }

    private static Map<?, ?> technical(Node node) {
        Object value = node.attributes().get("technicalImplementation");
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("qualified node " + node.id() + " has no technicalImplementation");
        }
        return map;
    }

    private enum ResolutionStatus { FOUND, MISSING, AMBIGUOUS }

    private record Resolution(Node node, ResolutionStatus status) {
        private static Resolution found(Node node) { return new Resolution(node, ResolutionStatus.FOUND); }
        private static Resolution missing() { return new Resolution(null, ResolutionStatus.MISSING); }
        private static Resolution ambiguous() { return new Resolution(null, ResolutionStatus.AMBIGUOUS); }
    }

    private static final class Graph {
        private final Map<String, Node> nodes = new HashMap<>();
        private final List<Relationship> relationships;

        private Graph(Project project) {
            project.nodes().forEach(node -> nodes.put(node.id(), node));
            relationships = List.copyOf(project.relationships());
        }

        private Resolution forward(String sourceId, String relationshipType,
                                   EventPathImplementationRole requiredRole) {
            List<String> candidateIds = relationships.stream()
                    .filter(relationship -> sourceId.equals(relationship.from()))
                    .filter(relationship -> relationshipType.equals(relationship.type()))
                    .map(Relationship::to).toList();
            return qualify(candidateIds, requiredRole);
        }

        private Resolution reverse(String targetId, String relationshipType,
                                   EventPathImplementationRole requiredRole) {
            List<String> candidateIds = relationships.stream()
                    .filter(relationship -> targetId.equals(relationship.to()))
                    .filter(relationship -> relationshipType.equals(relationship.type()))
                    .map(Relationship::from).toList();
            return qualify(candidateIds, requiredRole);
        }

        private Resolution qualify(List<String> candidateIds, EventPathImplementationRole requiredRole) {
            Map<String, Node> qualified = new TreeMap<>();
            for (String id : candidateIds) {
                Node node = nodes.get(id);
                if (node == null || !TECHNICAL_IMPLEMENTATION.equals(node.type())) continue;
                Object value = node.attributes().get("technicalImplementation");
                if (!(value instanceof Map<?, ?> technical)) continue;
                if (!requiredRole.name().equals(technical.get("implementationRole"))) continue;
                qualified.putIfAbsent(node.id(), node);
            }
            if (qualified.isEmpty()) return Resolution.missing();
            if (qualified.size() > 1) return Resolution.ambiguous();
            return Resolution.found(qualified.values().iterator().next());
        }
    }
}
