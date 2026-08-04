package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationListView;

import java.util.Objects;

public final class DefaultOperationListService implements OperationListService {
    private final OperationListGateway gateway;

    public DefaultOperationListService(OperationListGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public OperationListView getOperations(String repositoryId) {
        return gateway.getOperations(repositoryId);
    }
}
