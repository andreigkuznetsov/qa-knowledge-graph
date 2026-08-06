package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qaip.explorer.application.GetOperationTestsService;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionNoneQualified;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.view.OperationCheckView;
import ru.kuznetsov.qaip.explorer.view.OperationTestView;
import ru.kuznetsov.qaip.explorer.view.OperationTestsView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationTestsControllerTest {
    private GetOperationTestsService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(GetOperationTestsService.class);
        mvc = MockMvcBuilders.standaloneSetup(new OperationTestsController(service)).build();
    }

    @Test
    void returns_ordered_explorer_only_tests_checks_and_counts() throws Exception {
        when(service.getOperationTests("P-1", "OP-1"))
                .thenReturn(new OperationTestsProjectionFound(view()));

        mvc.perform(get("/api/v1/repositories/P-1/operations/OP-1/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value("P-1"))
                .andExpect(jsonPath("$.operationId").value("OP-1"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.testCount").value(2))
                .andExpect(jsonPath("$.checkCount").value(3))
                .andExpect(jsonPath("$.tests[0].testMethod").value("first"))
                .andExpect(jsonPath("$.tests[0].checks[0].displayName").value("First API"))
                .andExpect(jsonPath("$.tests[0].checks[1].displayName").value("First SQL"))
                .andExpect(jsonPath("$.tests[1].testMethod").value("second"))
                .andExpect(jsonPath("$.tests[1].checks[0].displayName").value("Second API"))
                .andExpect(jsonPath("$.tests[0].testId").doesNotExist())
                .andExpect(jsonPath("$.tests[0].checks[0].checkId").doesNotExist())
                .andExpect(jsonPath("$.relationships").doesNotExist())
                .andExpect(jsonPath("$.sourceReferences").doesNotExist());

        verify(service).getOperationTests("P-1", "OP-1");
        verifyNoMoreInteractions(service);
    }

    @Test
    void returns_normal_unverified_empty_200_when_no_tests_are_qualified() throws Exception {
        when(service.getOperationTests("P-1", "OP-EMPTY"))
                .thenReturn(new OperationTestsProjectionNoneQualified("P-1", "OP-EMPTY"));

        mvc.perform(get("/api/v1/repositories/P-1/operations/OP-EMPTY/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId").value("P-1"))
                .andExpect(jsonPath("$.operationId").value("OP-EMPTY"))
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.testCount").value(0))
                .andExpect(jsonPath("$.checkCount").value(0))
                .andExpect(jsonPath("$.tests").isEmpty());
    }

    @Test
    void maps_project_operation_and_ambiguity_to_explicit_errors() throws Exception {
        assertError("project", new OperationTestsProjectionProjectNotFound("P-1"),
                404, "PROJECT_NOT_FOUND");
        assertError("operation", new OperationTestsProjectionOperationNotFound("P-1", "operation"),
                404, "OPERATION_NOT_FOUND");
        assertError("ambiguous", new OperationTestsProjectionAmbiguous("P-1", "ambiguous"),
                409, "AMBIGUOUS_OPERATION_TESTS");
    }

    private void assertError(
            String operationId,
            ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionResult result,
            int statusCode,
            String code
    ) throws Exception {
        when(service.getOperationTests("P-1", operationId)).thenReturn(result);
        mvc.perform(get("/api/v1/repositories/P-1/operations/{operationId}/tests", operationId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private static OperationTestsView view() {
        OperationTestView first = test("first", List.of(
                new OperationCheckView("First API", "API"),
                new OperationCheckView("First SQL", "SQL")));
        OperationTestView second = test("second", List.of(new OperationCheckView("Second API", "API")));
        return new OperationTestsView("P-1", "OP-1", OperationVerificationStatus.VERIFIED,
                2, 3, List.of(first, second));
    }

    private static OperationTestView test(String method, List<OperationCheckView> checks) {
        return new OperationTestView("OrderApiIT." + method, "example.OrderApiIT",
                method, checks.size(), checks);
    }
}
