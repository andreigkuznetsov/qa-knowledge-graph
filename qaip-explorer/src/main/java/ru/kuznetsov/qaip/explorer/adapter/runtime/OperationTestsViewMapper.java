package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationCheck;
import ru.kuznetsov.qaip.core.application.query.operationtests.QualifiedOperationTest;
import ru.kuznetsov.qaip.explorer.view.OperationCheckView;
import ru.kuznetsov.qaip.explorer.view.OperationTestView;
import ru.kuznetsov.qaip.explorer.view.OperationTestsView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import java.util.List;
import java.util.Objects;

public final class OperationTestsViewMapper {
    public OperationTestsView map(OperationTestsFound found) {
        OperationTestsFound result = Objects.requireNonNull(found, "found");
        return map(result.projectId(), result.operationId(), result.verificationStatus(),
                result.testCount(), result.checkCount(), result.tests());
    }

    OperationTestsView map(
            String projectId,
            String operationId,
            ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus verificationStatus,
            int testCount,
            int checkCount,
            List<QualifiedOperationTest> tests
    ) {
        return new OperationTestsView(
                projectId, operationId,
                OperationVerificationStatus.valueOf(verificationStatus.name()),
                testCount, checkCount,
                tests.stream().map(OperationTestsViewMapper::test).toList());
    }

    private static OperationTestView test(QualifiedOperationTest test) {
        return new OperationTestView(test.displayName(), test.testClass(), test.testMethod(),
                test.qualifiedCheckCount(), test.checks().stream().map(OperationTestsViewMapper::check).toList());
    }

    private static OperationCheckView check(QualifiedOperationCheck check) {
        return new OperationCheckView(check.displayName(), check.checkType().name());
    }
}
