package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationQueryResult;
import ru.kuznetsov.qaip.explorer.application.InMemoryRepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.OperationListErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationListException;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisRecord;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeOperationListAdapterTest {
    @Test
    void maps_list_preserving_runtime_order_and_all_verification_statuses() {
        OperationListQuery query = mock(OperationListQuery.class);
        when(query.execute("PROJECT-1")).thenReturn(new OperationListFound(List.of(
                operation("OP-1", "GET", "/a", 1, 1),
                operation("OP-2", "GET", "/b", 1, 0),
                operation("OP-3", "POST", "/c", 0, 1),
                operation("OP-4", "POST", "/d", 0, 0))));

        var view = adapter(query, analyzedCatalog()).getOperations("PROJECT-1");

        assertEquals("PROJECT-1", view.repositoryId());
        assertEquals(List.of("OP-1", "OP-2", "OP-3", "OP-4"),
                view.operations().stream().map(item -> item.operationId()).toList());
        assertEquals(List.of(OperationVerificationStatus.VERIFIED,
                        OperationVerificationStatus.PARTIALLY_VERIFIED,
                        OperationVerificationStatus.PARTIALLY_VERIFIED,
                        OperationVerificationStatus.UNVERIFIED),
                view.operations().stream().map(item -> item.verificationStatus()).toList());
        verify(query).execute("PROJECT-1");
    }

    @Test
    void maps_empty_operation_list() {
        OperationListQuery query = mock(OperationListQuery.class);
        when(query.execute("PROJECT-1")).thenReturn(new OperationListFound(List.of()));

        assertEquals(List.of(), adapter(query, analyzedCatalog()).getOperations("PROJECT-1").operations());
    }

    @Test
    void reports_repository_not_found() {
        OperationListQuery query = mock(OperationListQuery.class);
        when(query.execute("missing")).thenReturn(new OperationListProjectNotFound("missing"));

        OperationListException exception = assertThrows(OperationListException.class,
                () -> adapter(query, new InMemoryRepositoryAnalysisCatalog()).getOperations("missing"));

        assertEquals(OperationListErrorCode.REPOSITORY_NOT_FOUND, exception.code());
    }

    @Test
    void reports_unavailable_analysis() {
        OperationListQuery query = mock(OperationListQuery.class);
        when(query.execute("PROJECT-1")).thenReturn(new OperationListFound(List.of()));

        OperationListException exception = assertThrows(OperationListException.class,
                () -> adapter(query, new InMemoryRepositoryAnalysisCatalog()).getOperations("PROJECT-1"));

        assertEquals(OperationListErrorCode.ANALYSIS_UNAVAILABLE, exception.code());
    }

    @Test
    void sanitizes_runtime_failure() {
        OperationListQuery query = mock(OperationListQuery.class);
        when(query.execute("PROJECT-1")).thenThrow(new IllegalStateException("database credentials leaked"));

        OperationListException exception = assertThrows(OperationListException.class,
                () -> adapter(query, analyzedCatalog()).getOperations("PROJECT-1"));

        assertEquals(OperationListErrorCode.RUNTIME_FAILURE, exception.code());
        assertEquals("Repository operations are unavailable", exception.getMessage());
    }

    private static RuntimeOperationListAdapter adapter(
            OperationListQuery query, InMemoryRepositoryAnalysisCatalog catalog) {
        QaipRuntime runtime = mock(QaipRuntime.class);
        when(runtime.operationListQuery()).thenReturn(query);
        return new RuntimeOperationListAdapter(runtime, catalog);
    }

    private static InMemoryRepositoryAnalysisCatalog analyzedCatalog() {
        var catalog = new InMemoryRepositoryAnalysisCatalog();
        catalog.save(new RepositoryAnalysisRecord(
                "PROJECT-1", "PROJECT-1", RepositoryAnalysisStatus.COMPLETE, 4, List.of()));
        return catalog;
    }

    private static OperationQueryResult operation(
            String id, String method, String path, int testCount, int checkCount) {
        return new OperationQueryResult(id, method, path, id + " display", testCount, checkCount);
    }
}
