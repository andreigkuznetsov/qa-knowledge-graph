package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationTestsView;

import java.util.Objects;

public record OperationTestsProjectionFound(OperationTestsView operationTests)
        implements OperationTestsProjectionResult {
    public OperationTestsProjectionFound {
        Objects.requireNonNull(operationTests, "operationTests");
    }
}
