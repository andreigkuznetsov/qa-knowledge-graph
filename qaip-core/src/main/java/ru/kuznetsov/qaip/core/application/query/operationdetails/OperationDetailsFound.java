package ru.kuznetsov.qaip.core.application.query.operationdetails;

import java.util.Objects;

public record OperationDetailsFound(OperationDetailsResult details) implements OperationDetailsQueryResult {
    public OperationDetailsFound {
        Objects.requireNonNull(details, "details");
    }
}
