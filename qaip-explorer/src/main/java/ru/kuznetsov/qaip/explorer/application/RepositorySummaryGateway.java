package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;

public interface RepositorySummaryGateway {
    RepositorySummaryView getSummary(String repositoryId);
}
