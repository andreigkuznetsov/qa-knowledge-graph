package ru.kuznetsov.qaip.explorer.view;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationTestsViewTest {
    @Test
    void constructs_successful_model_with_exact_presentation_fields() {
        OperationTestsView view = view(List.of(
                test("FirstIT", "first", List.of(check("Status", "API"))),
                test("SecondIT", "second", List.of(check("Database", "SQL")))));

        assertThat(view.repositoryId()).isEqualTo("REPOSITORY-1");
        assertThat(view.operationId()).isEqualTo("OPERATION-1");
        assertThat(view.verificationStatus()).isEqualTo(OperationVerificationStatus.VERIFIED);
        assertThat(view.testCount()).isEqualTo(2);
        assertThat(view.checkCount()).isEqualTo(2);
        assertThat(view.tests().getFirst().displayName()).isEqualTo("FirstIT.first");
        assertThat(view.tests().getFirst().checks().getFirst())
                .isEqualTo(new OperationCheckView("Status", "API"));
    }

    @Test
    void defensively_copies_and_exposes_immutable_test_and_check_collections() {
        List<OperationCheckView> mutableChecks = new ArrayList<>(List.of(check("Status", "API")));
        OperationTestView test = test("FirstIT", "first", mutableChecks);
        List<OperationTestView> mutableTests = new ArrayList<>(List.of(test));
        OperationTestsView view = view(mutableTests);
        mutableChecks.clear();
        mutableTests.clear();

        assertThat(view.tests()).hasSize(1);
        assertThat(view.tests().getFirst().checks()).hasSize(1);
        assertThatThrownBy(() -> view.tests().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> view.tests().getFirst().checks().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preserves_caller_supplied_test_and_check_order_without_reordering() {
        OperationTestView second = test("SecondIT", "second", List.of(
                check("Check Z", "SQL"), check("Check A", "API")));
        OperationTestView first = test("FirstIT", "first", List.of(check("Check B", "API")));

        OperationTestsView view = view(List.of(second, first));

        assertThat(view.tests()).extracting(OperationTestView::testMethod)
                .containsExactly("second", "first");
        assertThat(view.tests().getFirst().checks()).extracting(OperationCheckView::displayName)
                .containsExactly("Check Z", "Check A");
    }

    @Test
    void validates_copied_counts_and_requires_supported_check_metadata() {
        OperationTestView test = test("FirstIT", "first", List.of(check("Status", "API")));

        assertThatThrownBy(() -> new OperationTestView(
                test.displayName(), test.testClass(), test.testMethod(), 0, test.checks()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationTestsView(
                "R", "OP", OperationVerificationStatus.VERIFIED, 2, 1, List.of(test)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationCheckView(null, "API"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OperationCheckView("Status", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposes_no_canonical_runtime_relationship_source_or_persistence_fields() {
        Set<String> components = List.of(OperationTestsView.class, OperationTestView.class, OperationCheckView.class)
                .stream().flatMap(type -> Arrays.stream(type.getRecordComponents()))
                .map(RecordComponent::getName).collect(Collectors.toSet());

        assertThat(components).containsExactlyInAnyOrder(
                "repositoryId", "operationId", "verificationStatus", "testCount", "checkCount", "tests",
                "displayName", "testClass", "testMethod", "checks", "checkType");
        assertThat(components).noneMatch(name -> name.equals("testId") || name.equals("checkId")
                || name.toLowerCase().contains("relationship") || name.toLowerCase().contains("source")
                || name.toLowerCase().contains("persistence"));
        List.of(OperationTestsView.class, OperationTestView.class, OperationCheckView.class).forEach(type -> {
            assertThat(type.getAnnotations()).isEmpty();
            Arrays.stream(type.getRecordComponents()).map(component -> component.getType().getName())
                    .forEach(name -> {
                        assertThat(name).doesNotContain("qaip.core");
                        assertThat(name).doesNotContain("springframework");
                        assertThat(name).doesNotContain("swagger");
                    });
        });
    }

    @Test
    void existing_explorer_models_remain_constructible() {
        EventPathView eventPath = new EventPathView("P", "OP", "EVENT_DRIVEN", List.of());
        OperationDetailsView details = new OperationDetailsView(
                "OP", "POST", "/orders", "Create order", OperationVerificationStatus.VERIFIED,
                1, 1, new ImplementationPathView("Controller", "Service", "Repository"));

        assertThat(eventPath.operationId()).isEqualTo("OP");
        assertThat(details.testCount()).isEqualTo(1);
    }

    private static OperationTestsView view(List<OperationTestView> tests) {
        int checks = tests.stream().mapToInt(OperationTestView::checkCount).sum();
        return new OperationTestsView("REPOSITORY-1", "OPERATION-1",
                OperationVerificationStatus.VERIFIED, tests.size(), checks, tests);
    }

    private static OperationTestView test(
            String testClass, String testMethod, List<OperationCheckView> checks) {
        return new OperationTestView(testClass + '.' + testMethod, "example." + testClass,
                testMethod, checks.size(), checks);
    }

    private static OperationCheckView check(String displayName, String checkType) {
        return new OperationCheckView(displayName, checkType);
    }
}
