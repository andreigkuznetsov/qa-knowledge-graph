package ru.kuznetsov.qaip.explorer.view;

import java.util.List;
import java.util.Objects;

public record OperationListView(
        String repositoryId,
        List<OperationListItemView> operations
) {
    public OperationListView {
        Objects.requireNonNull(repositoryId, "repositoryId");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }
}
