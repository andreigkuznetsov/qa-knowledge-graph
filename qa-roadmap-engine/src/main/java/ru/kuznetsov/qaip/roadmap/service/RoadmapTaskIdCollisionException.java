package ru.kuznetsov.qaip.roadmap.service;

import ru.kuznetsov.qaip.findings.model.Finding;

public class RoadmapTaskIdCollisionException extends RuntimeException {

    public RoadmapTaskIdCollisionException(
            String taskId,
            Finding existing,
            Finding conflicting
    ) {
        super("Different findings resolve to roadmap task ID %s: %s/%s and %s/%s"
                .formatted(
                        taskId,
                        existing.code(),
                        existing.nodeId(),
                        conflicting.code(),
                        conflicting.nodeId()
                ));
    }
}
