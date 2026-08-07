package ru.kuznetsov.qaip.core.application.query.operationoverview;

import java.util.Objects;

public record OperationOverviewFound(
        OperationOverviewIdentity identity,
        OperationOverviewImplementationSection implementation,
        OperationOverviewEventPathSection eventPath,
        OperationOverviewVerificationSection verification
) implements OperationOverviewQueryResult {
    public OperationOverviewFound {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(eventPath, "eventPath");
        Objects.requireNonNull(verification, "verification");
        if (eventPath instanceof OperationOverviewEventPathAvailable available
                && (!identity.projectId().equals(available.path().projectId())
                || !identity.operationId().equals(available.path().operationId()))) {
            throw new IllegalArgumentException("event path must belong to the overview operation");
        }
    }
}
