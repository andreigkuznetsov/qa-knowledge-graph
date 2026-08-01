package ru.kuznetsov.qaip.core.application.query.nodedetails;

import ru.kuznetsov.qaip.core.domain.Node;

import java.util.Objects;

public final class NodeDetailsMapper {
    public NodeDetailsResult map(Node node) {
        Objects.requireNonNull(node, "node");
        return new NodeDetailsResult(
                node.id(),
                node.type(),
                node.name(),
                node.description(),
                node.status());
    }
}
