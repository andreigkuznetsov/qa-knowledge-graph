package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;

import java.util.Objects;

public final class DefaultOperationDetailsService implements OperationDetailsService {
    private final OperationDetailsGateway gateway;

    public DefaultOperationDetailsService(OperationDetailsGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public OperationDetailsView getDetails(String repositoryId, String operationId) {
        return gateway.getDetails(repositoryId, operationId);
    }
}
