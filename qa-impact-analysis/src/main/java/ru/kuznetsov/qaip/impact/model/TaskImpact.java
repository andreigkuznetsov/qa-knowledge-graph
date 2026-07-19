package ru.kuznetsov.qaip.impact.model;

import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;

import java.util.List;
import java.util.Objects;

public record TaskImpact(
        String taskId,
        RemediationTaskType taskType,
        String targetNodeId,
        FindingCode sourceFindingCode,
        StructuralGap structuralGap,
        ExpectedStructuralChange expectedChange,
        int executionWave,
        List<String> dependsOn
) {
    public TaskImpact {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        Objects.requireNonNull(sourceFindingCode,
                "sourceFindingCode must not be null");
        Objects.requireNonNull(structuralGap,
                "structuralGap must not be null");
        Objects.requireNonNull(expectedChange,
                "expectedChange must not be null");
        Objects.requireNonNull(dependsOn, "dependsOn must not be null");
        dependsOn = List.copyOf(dependsOn);
    }
}
