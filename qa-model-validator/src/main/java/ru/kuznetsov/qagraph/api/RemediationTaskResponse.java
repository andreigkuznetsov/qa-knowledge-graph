package ru.kuznetsov.qagraph.api;

import java.util.List;

public record RemediationTaskResponse(
        String id,
        String type,
        String status,
        String sourceFindingCode,
        String targetNodeId,
        String targetNodeType,
        String description,
        List<String> dependsOn
) {
    public RemediationTaskResponse {
        dependsOn = List.copyOf(dependsOn);
    }
}
