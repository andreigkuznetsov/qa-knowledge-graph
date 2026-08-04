package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.OperationListView;

public interface OperationListGateway {
    OperationListView getOperations(String repositoryId);
}
