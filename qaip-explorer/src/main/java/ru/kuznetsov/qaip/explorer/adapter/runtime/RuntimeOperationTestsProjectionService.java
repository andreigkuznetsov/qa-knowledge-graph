package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsAmbiguous;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsNoneQualified;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsQuery;
import ru.kuznetsov.qaip.explorer.application.GetOperationTestsService;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionNoneQualified;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionResult;

import java.util.Objects;

public final class RuntimeOperationTestsProjectionService implements GetOperationTestsService {
    private final OperationTestsQuery query;
    private final OperationTestsViewMapper mapper;

    public RuntimeOperationTestsProjectionService(OperationTestsQuery query, OperationTestsViewMapper mapper) {
        this.query = Objects.requireNonNull(query, "query");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public OperationTestsProjectionResult getOperationTests(String repositoryId, String operationId) {
        var result = query.execute(repositoryId, operationId);
        return switch (result) {
            case OperationTestsFound found -> new OperationTestsProjectionFound(mapper.map(found));
            case OperationTestsProjectNotFound notFound ->
                    new OperationTestsProjectionProjectNotFound(notFound.projectId());
            case OperationTestsOperationNotFound notFound ->
                    new OperationTestsProjectionOperationNotFound(notFound.projectId(), notFound.operationId());
            case OperationTestsNoneQualified none ->
                    new OperationTestsProjectionNoneQualified(none.projectId(), none.operationId());
            case OperationTestsAmbiguous ambiguous ->
                    new OperationTestsProjectionAmbiguous(ambiguous.projectId(), ambiguous.operationId());
        };
    }
}
