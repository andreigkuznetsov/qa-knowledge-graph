package ru.kuznetsov.qaip.explorer.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OperationListViewTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void createsValidOrderedList() {
        OperationListItemView first = operation("op-1", "GET", "/orders", "List orders",
                OperationVerificationStatus.VERIFIED, 3, 5);
        OperationListItemView second = operation("op-2", "POST", "/orders", "Create order",
                OperationVerificationStatus.PARTIALLY_VERIFIED, 1, 2);

        OperationListView view = new OperationListView("repository-1", List.of(first, second));

        assertThat(view.repositoryId()).isEqualTo("repository-1");
        assertThat(view.operations()).containsExactly(first, second);
    }

    @Test
    void allowsEmptyList() {
        OperationListView view = new OperationListView("repository-1", List.of());

        assertThat(view.operations()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThatNullPointerException().isThrownBy(() -> new OperationListView(null, List.of()));
        assertThatNullPointerException().isThrownBy(() -> new OperationListView("repository-1", null));
        assertThatNullPointerException().isThrownBy(() -> operation(null, "GET", "/orders", "List orders",
                OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> operation("op-1", null, "/orders", "List orders",
                OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> operation("op-1", "GET", null, "List orders",
                OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> operation("op-1", "GET", "/orders", null,
                OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> operation("op-1", "GET", "/orders", "List orders",
                null, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> new OperationListView("repository-1",
                java.util.Arrays.asList((OperationListItemView) null)));
    }

    @Test
    void rejectsBlankMethodPathAndDisplayName() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> operation("op-1", " ", "/orders", "List orders",
                        OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> operation("op-1", "GET", "\t", "List orders",
                        OperationVerificationStatus.VERIFIED, 0, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> operation("op-1", "GET", "/orders", "",
                        OperationVerificationStatus.VERIFIED, 0, 0));
    }

    @Test
    void rejectsNegativeCounts() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> operation("op-1", "GET", "/orders", "List orders",
                        OperationVerificationStatus.UNVERIFIED, -1, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> operation("op-1", "GET", "/orders", "List orders",
                        OperationVerificationStatus.UNVERIFIED, 0, -1));
    }

    @Test
    void snapshotsOperationsAsAnImmutableList() {
        OperationListItemView first = operation("op-1", "GET", "/orders", "List orders",
                OperationVerificationStatus.VERIFIED, 3, 5);
        List<OperationListItemView> source = new ArrayList<>();
        source.add(first);

        OperationListView view = new OperationListView("repository-1", source);
        source.clear();

        assertThat(view.operations()).containsExactly(first);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> view.operations().add(first));
    }

    @Test
    void hasDeterministicRecordValueEquality() {
        OperationListView left = new OperationListView("repository-1", List.of(operation(
                "op-1", "GET", "/orders", "List orders", OperationVerificationStatus.VERIFIED, 3, 5)));
        OperationListView right = new OperationListView("repository-1", List.of(operation(
                "op-1", "GET", "/orders", "List orders", OperationVerificationStatus.VERIFIED, 3, 5)));

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }

    @Test
    void serializesOnlyApprovedFields() throws Exception {
        OperationListView view = new OperationListView("repository-1", List.of(operation(
                "op-1", "GET", "/orders", "List orders", OperationVerificationStatus.VERIFIED, 3, 5)));

        JsonNode json = JSON.valueToTree(view);

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder("repositoryId", "operations");
        assertThat(json.get("operations").get(0).fieldNames()).toIterable().containsExactlyInAnyOrder(
                "operationId", "method", "path", "displayName", "verificationStatus", "testCount", "checkCount");
        assertThat(json.get("operations").get(0).get("verificationStatus").textValue()).isEqualTo("VERIFIED");
        assertThat(JSON.treeToValue(json, OperationListView.class)).isEqualTo(view);
    }

    private static OperationListItemView operation(
            String operationId,
            String method,
            String path,
            String displayName,
            OperationVerificationStatus verificationStatus,
            int testCount,
            int checkCount
    ) {
        return new OperationListItemView(
                operationId, method, path, displayName, verificationStatus, testCount, checkCount);
    }
}
