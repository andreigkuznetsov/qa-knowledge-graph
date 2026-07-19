package ru.kuznetsov.qaip.execution.model;

import java.util.List;

public record ExecutionWave(
        int number,
        List<String> taskIds
) {
    public ExecutionWave {
        taskIds = List.copyOf(taskIds);
    }
}
