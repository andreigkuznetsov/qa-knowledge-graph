package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsQuery;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsResult;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailable;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailableReason;
import ru.kuznetsov.qaip.explorer.application.InMemoryRepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsErrorCode;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsException;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisRecord;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeOperationDetailsAdapterTest {
    @Test
    void maps_found_details_and_all_verification_statuses() {
        assertEquals(OperationVerificationStatus.VERIFIED, mappedStatus(1, 1));
        assertEquals(OperationVerificationStatus.PARTIALLY_VERIFIED, mappedStatus(1, 0));
        assertEquals(OperationVerificationStatus.PARTIALLY_VERIFIED, mappedStatus(0, 1));
        assertEquals(OperationVerificationStatus.UNVERIFIED, mappedStatus(0, 0));
    }

    @Test
    void maps_approved_details_and_implementation_path() {
        OperationDetailsQuery query = mock(OperationDetailsQuery.class);
        when(query.execute("P-1", "OP-1")).thenReturn(new OperationDetailsFound(details(2, 3)));

        var view = adapter(query, analyzedCatalog()).getDetails("P-1", "OP-1");

        assertEquals("OP-1", view.operationId());
        assertEquals("OrdersController.create", view.implementationPath().controllerName());
        assertEquals("OrderService.create", view.implementationPath().serviceName());
        assertEquals("OrderRepository", view.implementationPath().repositoryName());
        verify(query).execute("P-1", "OP-1");
    }

    @Test
    void maps_all_typed_runtime_outcomes() {
        assertError(new OperationDetailsProjectNotFound("P-1"), OperationDetailsErrorCode.REPOSITORY_NOT_FOUND);
        assertError(new OperationDetailsOperationNotFound("P-1", "OP-1"),
                OperationDetailsErrorCode.OPERATION_NOT_FOUND);
        assertError(new OperationDetailsUnavailable(
                        "P-1", "OP-1", OperationDetailsUnavailableReason.INCOMPLETE_PATH),
                OperationDetailsErrorCode.INCOMPLETE_IMPLEMENTATION_PATH);
        assertError(new OperationDetailsUnavailable(
                        "P-1", "OP-1", OperationDetailsUnavailableReason.AMBIGUOUS_PATH),
                OperationDetailsErrorCode.AMBIGUOUS_IMPLEMENTATION_PATH);
    }

    @Test
    void reports_unavailable_analysis() {
        OperationDetailsQuery query = mock(OperationDetailsQuery.class);
        when(query.execute("P-1", "OP-1")).thenReturn(new OperationDetailsFound(details(0, 0)));

        OperationDetailsException exception = assertThrows(OperationDetailsException.class,
                () -> adapter(query, new InMemoryRepositoryAnalysisCatalog()).getDetails("P-1", "OP-1"));

        assertEquals(OperationDetailsErrorCode.ANALYSIS_UNAVAILABLE, exception.code());
    }

    @Test
    void sanitizes_runtime_failure() {
        OperationDetailsQuery query = mock(OperationDetailsQuery.class);
        when(query.execute("P-1", "OP-1")).thenThrow(new IllegalStateException("internal graph detail"));

        OperationDetailsException exception = assertThrows(OperationDetailsException.class,
                () -> adapter(query, analyzedCatalog()).getDetails("P-1", "OP-1"));

        assertEquals(OperationDetailsErrorCode.RUNTIME_FAILURE, exception.code());
        assertEquals("Operation details are unavailable", exception.getMessage());
    }

    private static OperationVerificationStatus mappedStatus(int tests, int checks) {
        OperationDetailsQuery query = mock(OperationDetailsQuery.class);
        when(query.execute("P-1", "OP-1")).thenReturn(new OperationDetailsFound(details(tests, checks)));
        return adapter(query, analyzedCatalog()).getDetails("P-1", "OP-1").verificationStatus();
    }

    private static void assertError(
            ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsQueryResult result,
            OperationDetailsErrorCode expectedCode) {
        OperationDetailsQuery query = mock(OperationDetailsQuery.class);
        when(query.execute("P-1", "OP-1")).thenReturn(result);
        OperationDetailsException exception = assertThrows(OperationDetailsException.class,
                () -> adapter(query, analyzedCatalog()).getDetails("P-1", "OP-1"));
        assertEquals(expectedCode, exception.code());
    }

    private static RuntimeOperationDetailsAdapter adapter(
            OperationDetailsQuery query, InMemoryRepositoryAnalysisCatalog catalog) {
        QaipRuntime runtime = mock(QaipRuntime.class);
        when(runtime.operationDetailsQuery()).thenReturn(query);
        return new RuntimeOperationDetailsAdapter(runtime, catalog);
    }

    private static InMemoryRepositoryAnalysisCatalog analyzedCatalog() {
        var catalog = new InMemoryRepositoryAnalysisCatalog();
        catalog.save(new RepositoryAnalysisRecord(
                "P-1", "P-1", RepositoryAnalysisStatus.COMPLETE, 1, List.of()));
        return catalog;
    }

    private static OperationDetailsResult details(int tests, int checks) {
        return new OperationDetailsResult("OP-1", "POST", "/orders", "POST /orders", tests, checks,
                "OrdersController.create", "OrderService.create", "OrderRepository");
    }
}
