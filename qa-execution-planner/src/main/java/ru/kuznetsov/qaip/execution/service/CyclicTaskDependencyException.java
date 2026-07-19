package ru.kuznetsov.qaip.execution.service;

import java.util.List;

public class CyclicTaskDependencyException extends RuntimeException {

    private final List<String> unresolvedTaskIds;

    public CyclicTaskDependencyException(List<String> unresolvedTaskIds) {
        super("Cyclic roadmap task dependencies: "
                + String.join(", ", unresolvedTaskIds));
        this.unresolvedTaskIds = List.copyOf(unresolvedTaskIds);
    }

    public List<String> unresolvedTaskIds() {
        return unresolvedTaskIds;
    }
}
