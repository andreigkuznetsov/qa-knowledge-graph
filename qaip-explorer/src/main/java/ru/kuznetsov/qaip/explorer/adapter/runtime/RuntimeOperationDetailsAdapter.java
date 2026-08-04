package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsResult;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailable;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsException;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsGateway;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.view.ImplementationPathView;
import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.Objects;

public final class RuntimeOperationDetailsAdapter implements OperationDetailsGateway {
    private final QaipRuntime runtime;
    private final RepositoryAnalysisCatalog catalog;

    public RuntimeOperationDetailsAdapter(QaipRuntime runtime, RepositoryAnalysisCatalog catalog) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public OperationDetailsView getDetails(String repositoryId, String operationId) {
        requireIds(repositoryId, operationId);
        try {
            var result = runtime.operationDetailsQuery().execute(repositoryId, operationId);
            if (result instanceof OperationDetailsProjectNotFound) {
                throw error(OperationDetailsErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
            }
            if (result instanceof OperationDetailsOperationNotFound) {
                throw error(OperationDetailsErrorCode.OPERATION_NOT_FOUND, "Operation was not found");
            }
            if (result instanceof OperationDetailsUnavailable unavailable) {
                throw switch (unavailable.reason()) {
                    case INCOMPLETE_PATH -> error(OperationDetailsErrorCode.INCOMPLETE_IMPLEMENTATION_PATH,
                            "Operation implementation path is incomplete");
                    case AMBIGUOUS_PATH -> error(OperationDetailsErrorCode.AMBIGUOUS_IMPLEMENTATION_PATH,
                            "Operation implementation path is ambiguous");
                };
            }
            catalog.findByRepositoryId(repositoryId).orElseThrow(() -> error(
                    OperationDetailsErrorCode.ANALYSIS_UNAVAILABLE, "Repository analysis is unavailable"));
            return map(((OperationDetailsFound) result).details());
        } catch (OperationDetailsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OperationDetailsException(
                    OperationDetailsErrorCode.RUNTIME_FAILURE, "Operation details are unavailable", exception);
        }
    }

    private static OperationDetailsView map(OperationDetailsResult details) {
        return new OperationDetailsView(
                details.operationId(), details.method(), details.path(), details.displayName(),
                OperationVerificationStatusMapper.fromCounts(details.testCount(), details.checkCount()),
                details.testCount(), details.checkCount(), new ImplementationPathView(
                details.controllerName(), details.serviceName(), details.repositoryName()));
    }

    private static void requireIds(String repositoryId, String operationId) {
        if (repositoryId == null || repositoryId.isBlank()) {
            throw error(OperationDetailsErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
        }
        if (operationId == null || operationId.isBlank()) {
            throw error(OperationDetailsErrorCode.OPERATION_NOT_FOUND, "Operation was not found");
        }
    }

    private static OperationDetailsException error(OperationDetailsErrorCode code, String message) {
        return new OperationDetailsException(code, message);
    }
}
