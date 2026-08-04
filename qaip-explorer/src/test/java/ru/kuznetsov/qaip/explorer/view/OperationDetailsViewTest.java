package ru.kuznetsov.qaip.explorer.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OperationDetailsViewTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void createsValidOperationDetails() {
        ImplementationPathView implementation = implementationPath();

        OperationDetailsView view = details(implementation);

        assertThat(view.operationId()).isEqualTo("OP-1");
        assertThat(view.method()).isEqualTo("POST");
        assertThat(view.path()).isEqualTo("/orders");
        assertThat(view.displayName()).isEqualTo("Create order");
        assertThat(view.verificationStatus()).isEqualTo(OperationVerificationStatus.VERIFIED);
        assertThat(view.testCount()).isEqualTo(2);
        assertThat(view.checkCount()).isEqualTo(3);
        assertThat(view.implementationPath()).isSameAs(implementation);
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                null, "POST", "/orders", "Create order", OperationVerificationStatus.VERIFIED,
                2, 3, implementationPath()));
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                "OP-1", null, "/orders", "Create order", OperationVerificationStatus.VERIFIED,
                2, 3, implementationPath()));
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", null, "Create order", OperationVerificationStatus.VERIFIED,
                2, 3, implementationPath()));
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", null, OperationVerificationStatus.VERIFIED,
                2, 3, implementationPath()));
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", null, 2, 3, implementationPath()));
        assertThatNullPointerException().isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", OperationVerificationStatus.VERIFIED,
                2, 3, null));
        assertThatNullPointerException().isThrownBy(() -> new ImplementationPathView(
                null, "OrderService", "OrderRepository"));
        assertThatNullPointerException().isThrownBy(() -> new ImplementationPathView(
                "OrdersController", null, "OrderRepository"));
        assertThatNullPointerException().isThrownBy(() -> new ImplementationPathView(
                "OrdersController", "OrderService", null));
    }

    @Test
    void rejectsBlankMethodPathAndDisplayName() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new OperationDetailsView(
                "OP-1", " ", "/orders", "Create order", OperationVerificationStatus.UNVERIFIED,
                0, 0, implementationPath()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "\t", "Create order", OperationVerificationStatus.UNVERIFIED,
                0, 0, implementationPath()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", "", OperationVerificationStatus.UNVERIFIED,
                0, 0, implementationPath()));
    }

    @Test
    void rejectsNegativeCounts() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", OperationVerificationStatus.UNVERIFIED,
                -1, 0, implementationPath()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", OperationVerificationStatus.UNVERIFIED,
                0, -1, implementationPath()));
    }

    @Test
    void implementationPathIsAnImmutableValue() {
        ImplementationPathView path = implementationPath();

        assertThat(path).isEqualTo(new ImplementationPathView(
                "OrdersController", "OrderService", "OrderRepository"));
        assertThat(ImplementationPathView.class.isRecord()).isTrue();
        assertThat(ImplementationPathView.class.getRecordComponents()).hasSize(3);
    }

    @Test
    void hasDeterministicRecordValueEquality() {
        OperationDetailsView left = details(implementationPath());
        OperationDetailsView right = details(implementationPath());

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }

    @Test
    void serializesOnlyApprovedFields() throws Exception {
        OperationDetailsView view = details(implementationPath());

        JsonNode json = JSON.valueToTree(view);

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "operationId", "method", "path", "displayName", "verificationStatus",
                "testCount", "checkCount", "implementationPath");
        assertThat(json.get("implementationPath").fieldNames()).toIterable().containsExactlyInAnyOrder(
                "controllerName", "serviceName", "repositoryName");
        assertThat(json.get("verificationStatus").textValue()).isEqualTo("VERIFIED");
        assertThat(JSON.treeToValue(json, OperationDetailsView.class)).isEqualTo(view);
    }

    private static OperationDetailsView details(ImplementationPathView implementationPath) {
        return new OperationDetailsView(
                "OP-1", "POST", "/orders", "Create order", OperationVerificationStatus.VERIFIED,
                2, 3, implementationPath);
    }

    private static ImplementationPathView implementationPath() {
        return new ImplementationPathView("OrdersController", "OrderService", "OrderRepository");
    }
}
