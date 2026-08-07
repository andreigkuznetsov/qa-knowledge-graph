package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.List;
import java.util.Objects;
import java.util.Comparator;

public record QualifiedOperationTest(
        String testId,
        String displayName,
        String testClass,
        String testMethod,
        int qualifiedCheckCount,
        List<QualifiedOperationCheck> checks
) {
    public QualifiedOperationTest {
        testId = OperationTestsProjectNotFound.requireId(testId, "testId");
        displayName = OperationTestsProjectNotFound.requireId(displayName, "displayName");
        testClass = OperationTestsProjectNotFound.requireId(testClass, "testClass");
        testMethod = OperationTestsProjectNotFound.requireId(testMethod, "testMethod");
        checks = Objects.requireNonNull(checks, "checks").stream()
                .sorted(Comparator.comparing(QualifiedOperationCheck::checkId))
                .toList();
        if (qualifiedCheckCount != checks.size()) {
            throw new IllegalArgumentException("qualifiedCheckCount must equal checks.size");
        }
    }
}
