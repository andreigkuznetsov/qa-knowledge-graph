package ru.kuznetsov.qaip.core.application.query.operationdetails;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared Runtime resolver for the conventional controller-service-repository path. */
public final class ConventionalImplementationPathResolver {
    private static final String TECHNICAL_IMPLEMENTATION = "TECHNICAL_IMPLEMENTATION";
    private static final String IMPLEMENTED_BY = "IMPLEMENTED_BY";
    private static final String USES = "USES";

    public Resolution resolve(Project project, String operationId) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(operationId, "operationId");
        Graph graph = new Graph(project);
        Level controller = graph.uniqueTechnicalTarget(operationId, IMPLEMENTED_BY);
        if (controller.reason() != null) return new Unavailable(controller.reason());
        Level service = graph.uniqueStagedTarget(controller.node().id(), USES, "SERVICE");
        if (service.reason() != null) return new Unavailable(service.reason());
        Level repository = graph.uniqueStagedTarget(service.node().id(), USES, "REPOSITORY");
        if (repository.reason() != null) return new Unavailable(repository.reason());
        return new Available(controller.node(), service.node(), repository.node());
    }

    public sealed interface Resolution permits Available, Unavailable { }

    public record Available(Node controller, Node service, Node repository) implements Resolution {
        public Available {
            Objects.requireNonNull(controller, "controller");
            Objects.requireNonNull(service, "service");
            Objects.requireNonNull(repository, "repository");
        }
    }

    public record Unavailable(OperationDetailsUnavailableReason reason) implements Resolution {
        public Unavailable {
            Objects.requireNonNull(reason, "reason");
        }
    }

    private record Level(Node node, OperationDetailsUnavailableReason reason) { }

    private static final class Graph {
        private final Map<String, Node> nodes = new HashMap<>();
        private final List<Relationship> relationships;

        private Graph(Project project) {
            project.nodes().forEach(node -> nodes.put(node.id(), node));
            relationships = project.relationships();
        }

        private Level uniqueTechnicalTarget(String sourceId, String relationshipType) {
            return uniqueTarget(sourceId, relationshipType, null);
        }

        private Level uniqueStagedTarget(String sourceId, String relationshipType, String stage) {
            return uniqueTarget(sourceId, relationshipType, stage);
        }

        private Level uniqueTarget(String sourceId, String relationshipType, String stage) {
            List<Node> candidates = relationships.stream()
                    .filter(relationship -> sourceId.equals(relationship.from()))
                    .filter(relationship -> relationshipType.equals(relationship.type()))
                    .map(relationship -> nodes.get(relationship.to()))
                    .filter(Objects::nonNull)
                    .filter(node -> TECHNICAL_IMPLEMENTATION.equals(node.type()))
                    .filter(node -> stage == null || stage.equals(flowStage(node)))
                    .distinct()
                    .toList();
            if (candidates.isEmpty()) {
                return new Level(null, OperationDetailsUnavailableReason.INCOMPLETE_PATH);
            }
            if (candidates.size() > 1) {
                return new Level(null, OperationDetailsUnavailableReason.AMBIGUOUS_PATH);
            }
            return new Level(candidates.getFirst(), null);
        }

        private static Object flowStage(Node node) {
            Object technical = node.attributes().get("technicalImplementation");
            if (!(technical instanceof Map<?, ?> technicalMap)) return null;
            Object details = technicalMap.get("details");
            if (!(details instanceof Map<?, ?> detailsMap)) return null;
            return detailsMap.get("flowStage");
        }
    }
}
