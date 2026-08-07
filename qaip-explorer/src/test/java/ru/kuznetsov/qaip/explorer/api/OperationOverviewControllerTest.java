package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.kuznetsov.qaip.explorer.application.GetOperationOverviewService;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathNotApplicableView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewIdentityView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationIncompleteView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationAvailableView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewView;
import ru.kuznetsov.qaip.explorer.view.OperationTestsView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationOverviewControllerTest {
    private GetOperationOverviewService service;
    private OperationOverviewController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(GetOperationOverviewService.class);
        controller = new OperationOverviewController(service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returns_the_explorer_overview_unchanged_with_explicit_partial_section_states() throws Exception {
        OperationOverviewView overview = overview();
        when(service.getOperationOverview("P-1", "OP-1"))
                .thenReturn(new OperationOverviewProjectionFound(overview));

        assertThat(controller.operationOverview("P-1", "OP-1").getBody()).isSameAs(overview);

        mvc.perform(get(OperationOverviewApiContract.PATH, "P-1", "OP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.repositoryId").value("P-1"))
                .andExpect(jsonPath("$.identity.operationId").value("OP-1"))
                .andExpect(jsonPath("$.identity.method").value("POST"))
                .andExpect(jsonPath("$.identity.path").value("/api/orders"))
                .andExpect(jsonPath("$.identity.displayName").value("POST /api/orders"))
                .andExpect(jsonPath("$.implementation.state").value("INCOMPLETE"))
                .andExpect(jsonPath("$.eventPath.state").value("NOT_APPLICABLE"))
                .andExpect(jsonPath("$.verification.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.verification.verification.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.verification.verification.testCount").value(0))
                .andExpect(jsonPath("$.verification.verification.checkCount").value(0))
                .andExpect(jsonPath("$.verification.verification.tests").isEmpty());

        verify(service, org.mockito.Mockito.times(2)).getOperationOverview("P-1", "OP-1");
        verifyNoMoreInteractions(service);
    }

    @Test
    void maps_project_not_found_to_the_contract_404_response() throws Exception {
        when(service.getOperationOverview("missing", "OP-1"))
                .thenReturn(new OperationOverviewProjectionProjectNotFound("missing"));

        mvc.perform(get(OperationOverviewApiContract.PATH, "missing", "OP-1"))
                .andExpect(status().is(OperationOverviewApiContract.PROJECT_NOT_FOUND_STATUS))
                .andExpect(jsonPath("$.code").value(OperationOverviewApiContract.PROJECT_NOT_FOUND_CODE))
                .andExpect(jsonPath("$.message").value("Repository was not found"));

        verify(service).getOperationOverview("missing", "OP-1");
        verifyNoMoreInteractions(service);
    }

    @Test
    void maps_operation_not_found_to_the_contract_404_response() throws Exception {
        when(service.getOperationOverview("P-1", "missing"))
                .thenReturn(new OperationOverviewProjectionOperationNotFound("P-1", "missing"));

        mvc.perform(get(OperationOverviewApiContract.PATH, "P-1", "missing"))
                .andExpect(status().is(OperationOverviewApiContract.OPERATION_NOT_FOUND_STATUS))
                .andExpect(jsonPath("$.code").value(OperationOverviewApiContract.OPERATION_NOT_FOUND_CODE))
                .andExpect(jsonPath("$.message").value("Operation was not found"));

        verify(service).getOperationOverview("P-1", "missing");
        verifyNoMoreInteractions(service);
    }

    private static OperationOverviewView overview() {
        return new OperationOverviewView(
                new OperationOverviewIdentityView(
                        "P-1", "OP-1", "POST", "/api/orders", "POST /api/orders"),
                new OperationOverviewImplementationIncompleteView(
                        OperationOverviewImplementationState.INCOMPLETE),
                new OperationOverviewEventPathNotApplicableView(
                        OperationOverviewEventPathState.NOT_APPLICABLE),
                new OperationOverviewVerificationAvailableView(
                        OperationOverviewVerificationState.AVAILABLE,
                        new OperationTestsView(
                                "P-1", "OP-1", OperationVerificationStatus.UNVERIFIED,
                                0, 0, List.of())));
    }
}
