package ru.kuznetsov.qaip.core.application.query.operationoverview;

public record OperationOverviewImplementation(
        String controllerName,
        String serviceName,
        String repositoryName
) {
    public OperationOverviewImplementation {
        controllerName = OperationOverviewContract.requireId(controllerName, "controllerName");
        serviceName = OperationOverviewContract.requireId(serviceName, "serviceName");
        repositoryName = OperationOverviewContract.requireId(repositoryName, "repositoryName");
    }
}
