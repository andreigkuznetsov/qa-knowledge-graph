package ru.kuznetsov.qaip.core.application.query.operationlist;

import ru.kuznetsov.qaip.core.domain.Node;

import java.util.Objects;

/** Shared Runtime semantics for projecting one business-operation identity. */
public final class OperationIdentityResolver {
    public ResolvedOperationIdentity resolve(Node operation) {
        Node node = Objects.requireNonNull(operation, "operation");
        String[] parts = Objects.requireNonNull(
                node.name(), "business operation name").trim().split("\\s+", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "business operation name must contain HTTP method and path: " + node.id());
        }
        return new ResolvedOperationIdentity(node.id(), parts[0], parts[1], node.name());
    }

    public record ResolvedOperationIdentity(
            String operationId,
            String method,
            String path,
            String displayName
    ) {
        public ResolvedOperationIdentity {
            Objects.requireNonNull(operationId, "operationId");
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(displayName, "displayName");
        }
    }
}
