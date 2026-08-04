package ru.kuznetsov.qaip.core.application.query.operationlist;

import java.util.List;
import java.util.Objects;

public record OperationListFound(List<OperationQueryResult> operations) implements OperationListQueryResult {
    public OperationListFound {
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }
}
