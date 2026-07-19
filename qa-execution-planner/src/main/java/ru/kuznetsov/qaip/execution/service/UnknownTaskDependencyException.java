package ru.kuznetsov.qaip.execution.service;

public class UnknownTaskDependencyException extends RuntimeException {

    public UnknownTaskDependencyException(
            String dependentTaskId,
            String missingDependencyTaskId
    ) {
        super("Roadmap task %s depends on unknown task %s"
                .formatted(dependentTaskId, missingDependencyTaskId));
    }
}
