package ru.kuznetsov.qaip.core.application.query.operationtests;

import java.util.Objects;

public record QualifiedOperationCheck(
        String checkId,
        String displayName,
        OperationCheckType checkType
) {
    public QualifiedOperationCheck {
        checkId = OperationTestsProjectNotFound.requireId(checkId, "checkId");
        displayName = OperationTestsProjectNotFound.requireId(displayName, "displayName");
        Objects.requireNonNull(checkType, "checkType");
    }
}
