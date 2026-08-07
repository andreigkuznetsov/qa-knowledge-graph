package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationCheckType;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsAmbiguous;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsNoneQualified;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsQuery;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationCheck;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationTest;
import ru.kuznetsov.qaip.explorer.application.GetOperationTestsService;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionNoneQualified;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionResult;
import ru.kuznetsov.qaip.explorer.view.OperationCheckView;
import ru.kuznetsov.qaip.explorer.view.OperationTestView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RuntimeOperationTestsProjectionServiceTest {
    private final OperationTestsQuery query = mock(OperationTestsQuery.class);
    private final OperationTestsViewMapper mapper = new OperationTestsViewMapper();
    private final GetOperationTestsService service = new RuntimeOperationTestsProjectionService(query, mapper);

    @Test
    void invokes_runtime_once_and_projects_counts_status_tests_and_checks_in_runtime_order() {
        when(query.execute("P-1", "OP-1")).thenReturn(found());

        OperationTestsProjectionFound result = (OperationTestsProjectionFound)
                service.getOperationTests("P-1", "OP-1");

        assertThat(result.operationTests().repositoryId()).isEqualTo("P-1");
        assertThat(result.operationTests().operationId()).isEqualTo("OP-1");
        assertThat(result.operationTests().verificationStatus())
                .isEqualTo(ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus.VERIFIED);
        assertThat(result.operationTests().testCount()).isEqualTo(2);
        assertThat(result.operationTests().checkCount()).isEqualTo(3);
        assertThat(result.operationTests().tests()).extracting(OperationTestView::testMethod)
                .containsExactly("first", "second");
        assertThat(result.operationTests().tests().getFirst().checks()).extracting(OperationCheckView::displayName)
                .containsExactly("First API check", "First SQL check");
        assertThat(result.operationTests().tests().get(1).checks()).extracting(OperationCheckView::displayName)
                .containsExactly("Second API check");
        assertThatThrownBy(() -> result.operationTests().tests().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.operationTests().tests().getFirst().checks().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(result.operationTests().tests()).allSatisfy(test ->
                assertThat(test.getClass().getRecordComponents()).noneMatch(component ->
                        component.getName().toLowerCase().contains("id")));
        verify(query).execute("P-1", "OP-1");
        verifyNoMoreInteractions(query);
    }

    @Test
    void maps_every_expected_runtime_outcome_to_an_explorer_owned_result() {
        assertMaps(new OperationTestsProjectNotFound("P"),
                new OperationTestsProjectionProjectNotFound("P"));
        assertMaps(new OperationTestsOperationNotFound("P", "OP"),
                new OperationTestsProjectionOperationNotFound("P", "OP"));
        assertMaps(new OperationTestsNoneQualified("P", "OP"),
                new OperationTestsProjectionNoneQualified("P", "OP"));
        assertMaps(new OperationTestsAmbiguous("P", "OP"),
                new OperationTestsProjectionAmbiguous("P", "OP"));
    }

    @Test
    void service_has_no_graph_fallback_or_traversal_collaborator() {
        assertThat(RuntimeOperationTestsProjectionService.class.getDeclaredFields())
                .extracting(field -> field.getType().getName())
                .containsExactlyInAnyOrder(OperationTestsQuery.class.getName(), OperationTestsViewMapper.class.getName());
        assertThat(GetOperationTestsService.class.getDeclaredMethods()).hasSize(1);
        assertThat(OperationTestsProjectionResult.class.isSealed()).isTrue();
        assertThat(OperationTestsProjectionResult.class.getPermittedSubclasses()).containsExactlyInAnyOrder(
                OperationTestsProjectionFound.class, OperationTestsProjectionProjectNotFound.class,
                OperationTestsProjectionOperationNotFound.class, OperationTestsProjectionNoneQualified.class,
                OperationTestsProjectionAmbiguous.class);
    }

    private void assertMaps(
            ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsQueryResult runtime,
            OperationTestsProjectionResult expected) {
        when(query.execute("P", "OP")).thenReturn(runtime);

        assertThat(service.getOperationTests("P", "OP")).isEqualTo(expected);

        verify(query).execute("P", "OP");
        clearInvocations(query);
    }

    private static OperationTestsFound found() {
        QualifiedOperationTest second = test("TEST-2", "second", List.of(
                check("CHECK-3", "Second API check", OperationCheckType.API)));
        QualifiedOperationTest first = test("TEST-1", "first", List.of(
                check("CHECK-2", "First SQL check", OperationCheckType.SQL),
                check("CHECK-1", "First API check", OperationCheckType.API)));
        return new OperationTestsFound("P-1", "OP-1", OperationVerificationStatus.VERIFIED,
                List.of(second, first));
    }

    private static QualifiedOperationTest test(
            String id, String method, List<QualifiedOperationCheck> checks) {
        return new QualifiedOperationTest(id, "example.OrderApiIT." + method,
                "example.OrderApiIT", method, checks.size(), checks);
    }

    private static QualifiedOperationCheck check(String id, String name, OperationCheckType type) {
        return new QualifiedOperationCheck(id, name, type);
    }
}
