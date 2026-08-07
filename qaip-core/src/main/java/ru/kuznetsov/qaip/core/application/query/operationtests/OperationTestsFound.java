package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.List;
import java.util.Objects;

public record OperationTestsFound(
        String projectId,
        String operationId,
        OperationVerificationStatus verificationStatus,
        List<QualifiedOperationTest> tests
) implements OperationTestsQueryResult {
    public OperationTestsFound {
        projectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        operationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
        Objects.requireNonNull(verificationStatus, "verificationStatus");
        OperationVerificationSemantics.Summary verification =
                OperationVerificationSemantics.summarize(tests);
        if (verification.tests().isEmpty()) throw new IllegalArgumentException("tests must not be empty");
        if (verificationStatus != verification.verificationStatus()) {
            throw new IllegalArgumentException("verificationStatus must derive from returned tests and checks");
        }
        tests = verification.tests();
    }

    public int testCount() {
        return tests.size();
    }

    public int checkCount() {
        return OperationVerificationSemantics.summarize(tests).checkCount();
    }
}
