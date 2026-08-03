package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;

import java.util.Objects;

public final class DefaultRepositorySummaryService implements RepositorySummaryService {
    private final RepositorySummaryGateway gateway;

    public DefaultRepositorySummaryService(RepositorySummaryGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public RepositorySummaryView getSummary(String repositoryId) {
        return gateway.getSummary(repositoryId);
    }
}
