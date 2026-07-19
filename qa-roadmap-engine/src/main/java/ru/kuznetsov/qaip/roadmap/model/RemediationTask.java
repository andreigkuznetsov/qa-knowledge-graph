package ru.kuznetsov.qaip.roadmap.model;

import ru.kuznetsov.qaip.findings.model.FindingCode;

import java.util.List;

public record RemediationTask(
        String id,
        RemediationTaskType type,
        RemediationTaskStatus status,
        FindingCode sourceFindingCode,
        String targetNodeId,
        String targetNodeType,
        String description,
        List<String> dependsOn
) {
    public RemediationTask {
        dependsOn = List.copyOf(dependsOn);
    }
}
