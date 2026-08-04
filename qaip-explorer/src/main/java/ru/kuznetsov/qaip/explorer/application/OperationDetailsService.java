package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;

public interface OperationDetailsService {
    OperationDetailsView getDetails(String repositoryId, String operationId);
}
