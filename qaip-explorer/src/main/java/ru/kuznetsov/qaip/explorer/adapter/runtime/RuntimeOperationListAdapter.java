package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationQueryResult;
import ru.kuznetsov.qaip.explorer.application.OperationListErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationListException;
import ru.kuznetsov.qaip.explorer.application.OperationListGateway;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.view.OperationListItemView;
import ru.kuznetsov.qaip.explorer.view.OperationListView;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.Objects;

public final class RuntimeOperationListAdapter implements OperationListGateway {
    private final QaipRuntime runtime;
    private final RepositoryAnalysisCatalog catalog;

    public RuntimeOperationListAdapter(QaipRuntime runtime, RepositoryAnalysisCatalog catalog) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public OperationListView getOperations(String repositoryId) {
        requireRepositoryId(repositoryId);
        try {
            var result = runtime.operationListQuery().execute(repositoryId);
            if (result instanceof OperationListProjectNotFound) {
                throw new OperationListException(
                        OperationListErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
            }
            catalog.findByRepositoryId(repositoryId).orElseThrow(() -> new OperationListException(
                    OperationListErrorCode.ANALYSIS_UNAVAILABLE, "Repository analysis is unavailable"));
            var operations = ((OperationListFound) result).operations().stream()
                    .map(RuntimeOperationListAdapter::map)
                    .toList();
            return new OperationListView(repositoryId, operations);
        } catch (OperationListException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OperationListException(
                    OperationListErrorCode.RUNTIME_FAILURE, "Repository operations are unavailable", exception);
        }
    }

    private static OperationListItemView map(OperationQueryResult operation) {
        return new OperationListItemView(
                operation.operationId(),
                operation.method(),
                operation.path(),
                operation.displayName(),
                OperationVerificationStatusMapper.fromCounts(operation.testCount(), operation.checkCount()),
                operation.testCount(),
                operation.checkCount());
    }

    private static void requireRepositoryId(String repositoryId) {
        if (repositoryId == null || repositoryId.isBlank()) {
            throw new OperationListException(
                    OperationListErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
        }
    }
}
