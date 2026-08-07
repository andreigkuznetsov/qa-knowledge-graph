package ru.kuznetsov.qaip.explorer.application;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.explorer.api.OperationOverviewApiContract;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathNotApplicableView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewIdentityView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationIncompleteView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationAmbiguousView;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationState;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewView;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationOverviewProjectionContractTest {
    @Test
    void result_hierarchy_defines_found_project_missing_and_operation_missing() {
        assertThat(OperationOverviewProjectionResult.class.isSealed()).isTrue();
        assertThat(Arrays.stream(OperationOverviewProjectionResult.class.getPermittedSubclasses())
                .collect(Collectors.toSet())).isEqualTo(Set.of(
                OperationOverviewProjectionFound.class,
                OperationOverviewProjectionProjectNotFound.class,
                OperationOverviewProjectionOperationNotFound.class));

        assertThat(new OperationOverviewProjectionFound(overview()).overview()).isEqualTo(overview());
        assertThat(new OperationOverviewProjectionProjectNotFound("R").repositoryId()).isEqualTo("R");
        assertThat(new OperationOverviewProjectionOperationNotFound("R", "OP").operationId()).isEqualTo("OP");
    }

    @Test
    void endpoint_contract_fixes_path_success_and_not_found_mappings() {
        assertThat(OperationOverviewApiContract.PATH)
                .isEqualTo("/api/v1/repositories/{repositoryId}/operations/{operationId}/overview");
        assertThat(OperationOverviewApiContract.FOUND_STATUS).isEqualTo(200);
        assertThat(OperationOverviewApiContract.PROJECT_NOT_FOUND_STATUS).isEqualTo(404);
        assertThat(OperationOverviewApiContract.OPERATION_NOT_FOUND_STATUS).isEqualTo(404);
        assertThat(OperationOverviewApiContract.PROJECT_NOT_FOUND_CODE).isEqualTo("PROJECT_NOT_FOUND");
        assertThat(OperationOverviewApiContract.OPERATION_NOT_FOUND_CODE).isEqualTo("OPERATION_NOT_FOUND");
    }

    @Test
    void result_contract_rejects_missing_identity_or_overview_values() {
        assertThatThrownBy(() -> new OperationOverviewProjectionFound(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OperationOverviewProjectionProjectNotFound(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationOverviewProjectionOperationNotFound("R", null))
                .isInstanceOf(NullPointerException.class);
    }

    private static OperationOverviewView overview() {
        return new OperationOverviewView(
                new OperationOverviewIdentityView("R", "OP", "GET", "/", "GET /"),
                new OperationOverviewImplementationIncompleteView(
                        OperationOverviewImplementationState.INCOMPLETE),
                new OperationOverviewEventPathNotApplicableView(
                        OperationOverviewEventPathState.NOT_APPLICABLE),
                new OperationOverviewVerificationAmbiguousView(
                        OperationOverviewVerificationState.AMBIGUOUS));
    }
}
