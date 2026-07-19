package ru.kuznetsov.qaip.roadmap.service;

import ru.kuznetsov.qaip.findings.model.Finding;

public class InvalidFindingTargetException extends RuntimeException {

    public InvalidFindingTargetException(
            Finding finding,
            String expectedNodeType
    ) {
        super("Finding %s has incompatible target type %s; expected %s for node %s"
                .formatted(
                        finding.code(),
                        finding.nodeType(),
                        expectedNodeType,
                        finding.nodeId()
                ));
    }

    public InvalidFindingTargetException(Finding finding) {
        super("Finding %s has node ID that cannot form a task ID: %s"
                .formatted(finding.code(), finding.nodeId()));
    }
}
