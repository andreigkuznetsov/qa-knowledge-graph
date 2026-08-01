package ru.kuznetsov.qaip.core.application.query.node;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Objects;
import java.util.Optional;

public final class ProjectNodeLookup {
    public Optional<Node> findById(Project project, String nodeId) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(nodeId, "nodeId");
        if (nodeId.isBlank()) throw new IllegalArgumentException("nodeId must not be blank");
        for (Node node : project.nodes()) {
            if (node.id().equals(nodeId)) return Optional.of(node);
        }
        return Optional.empty();
    }
}
