package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;

public interface RepositorySummaryService {
    RepositorySummaryView getSummary(String repositoryId);
}
