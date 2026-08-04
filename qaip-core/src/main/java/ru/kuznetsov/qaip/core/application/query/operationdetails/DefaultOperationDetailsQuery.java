package ru.kuznetsov.qaip.core.application.query.operationdetails;

import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationQueryResult;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultOperationDetailsQuery implements OperationDetailsQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";
    private static final String TECHNICAL_IMPLEMENTATION = "TECHNICAL_IMPLEMENTATION";
    private static final String IMPLEMENTED_BY = "IMPLEMENTED_BY";
    private static final String USES = "USES";

    private final ProjectReader projectReader;
    private final OperationListProjector operationProjector;

    public DefaultOperationDetailsQuery(ProjectReader projectReader, OperationListProjector operationProjector) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.operationProjector = Objects.requireNonNull(operationProjector, "operationProjector");
    }

    @Override
    public OperationDetailsQueryResult execute(String projectId, String operationId) {
        OperationDetailsProjectNotFound.requireId(projectId, "projectId");
        OperationDetailsProjectNotFound.requireId(operationId, "operationId");
        var project = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (project.isEmpty()) return new OperationDetailsProjectNotFound(projectId);
        Project value = project.orElseThrow();
        if (value.nodes().stream().noneMatch(node ->
                operationId.equals(node.id()) && BUSINESS_OPERATION.equals(node.type()))) {
            return new OperationDetailsOperationNotFound(projectId, operationId);
        }

        OperationQueryResult operation = operationProjector.project(value).stream()
                .filter(candidate -> operationId.equals(candidate.operationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Operation projector omitted an existing operation"));
        PathResolution path = resolvePath(value, operationId);
        if (path.reason() != null) {
            return new OperationDetailsUnavailable(projectId, operationId, path.reason());
        }
        return new OperationDetailsFound(new OperationDetailsResult(
                operation.operationId(), operation.method(), operation.path(), operation.displayName(),
                operation.testCount(), operation.checkCount(), path.controller().name(),
                path.service().name(), path.repository().name()));
    }

    private static PathResolution resolvePath(Project project, String operationId) {
        Graph graph = new Graph(project);
        Level controller = graph.uniqueTechnicalTarget(operationId, IMPLEMENTED_BY);
        if (controller.reason() != null) return PathResolution.unavailable(controller.reason());
        Level service = graph.uniqueStagedTarget(controller.node().id(), USES, "SERVICE");
        if (service.reason() != null) return PathResolution.unavailable(service.reason());
        Level repository = graph.uniqueStagedTarget(service.node().id(), USES, "REPOSITORY");
        if (repository.reason() != null) return PathResolution.unavailable(repository.reason());
        return new PathResolution(controller.node(), service.node(), repository.node(), null);
    }

    private record PathResolution(
            Node controller,
            Node service,
            Node repository,
            OperationDetailsUnavailableReason reason
    ) {
        private static PathResolution unavailable(OperationDetailsUnavailableReason reason) {
            return new PathResolution(null, null, null, reason);
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
