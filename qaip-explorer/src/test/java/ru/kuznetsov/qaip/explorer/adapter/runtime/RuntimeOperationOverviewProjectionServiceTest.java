package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewEventPathNotApplicable;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewFound;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewIdentity;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewImplementationIncomplete;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewQuery;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewQueryResult;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewVerificationAmbiguous;
import ru.kuznetsov.qaip.explorer.application.GetOperationOverviewService;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RuntimeOperationOverviewProjectionServiceTest {
    private final OperationOverviewQuery query = mock(OperationOverviewQuery.class);
    private final OperationOverviewViewMapper mapper = mock(OperationOverviewViewMapper.class);
    private final GetOperationOverviewService service =
            new RuntimeOperationOverviewProjectionService(query, mapper);

    @Test
    void invokes_runtime_once_and_returns_the_mapper_result_unchanged() {
        OperationOverviewFound runtimeResult = found();
        OperationOverviewProjectionResult projected =
                new OperationOverviewProjectionProjectNotFound("projected-result");
        when(query.execute("P-1", "OP-1")).thenReturn(runtimeResult);
        when(mapper.map(runtimeResult)).thenReturn(projected);

        assertThat(service.getOperationOverview("P-1", "OP-1")).isSameAs(projected);

        verify(query).execute("P-1", "OP-1");
        verify(mapper).map(runtimeResult);
        verifyNoMoreInteractions(query, mapper);
    }

    @Test
    void delegates_project_and_operation_not_found_results_to_the_same_mapper() {
        assertDelegates(
                new OperationOverviewProjectNotFound("P-404"),
                new OperationOverviewProjectionProjectNotFound("P-404"));
        assertDelegates(
                new OperationOverviewOperationNotFound("P-1", "OP-404"),
                new OperationOverviewProjectionOperationNotFound("P-1", "OP-404"));
    }

    @Test
    void service_boundary_has_only_the_runtime_query_and_projection_mapper_dependencies() {
        assertThat(RuntimeOperationOverviewProjectionService.class.getDeclaredFields())
                .extracting(field -> field.getType().getName())
                .containsExactlyInAnyOrder(
                        OperationOverviewQuery.class.getName(),
                        OperationOverviewViewMapper.class.getName());
        assertThat(GetOperationOverviewService.class.getDeclaredMethods()).hasSize(1);
    }

    @Test
    void rejects_missing_dependencies() {
        assertThatNullPointerException().isThrownBy(() ->
                new RuntimeOperationOverviewProjectionService(null, mapper));
        assertThatNullPointerException().isThrownBy(() ->
                new RuntimeOperationOverviewProjectionService(query, null));
    }

    private void assertDelegates(
            OperationOverviewQueryResult runtimeResult,
            OperationOverviewProjectionResult projected
    ) {
        when(query.execute("P", "OP")).thenReturn(runtimeResult);
        when(mapper.map(runtimeResult)).thenReturn(projected);

        assertThat(service.getOperationOverview("P", "OP")).isSameAs(projected);

        verify(query).execute("P", "OP");
        verify(mapper).map(runtimeResult);
        org.mockito.Mockito.clearInvocations(query, mapper);
    }

    private static OperationOverviewFound found() {
        return new OperationOverviewFound(
                new OperationOverviewIdentity("P-1", "OP-1", "POST", "/api/orders", "POST /api/orders"),
                new OperationOverviewImplementationIncomplete(),
                new OperationOverviewEventPathNotApplicable(),
                new OperationOverviewVerificationAmbiguous());
    }
}
