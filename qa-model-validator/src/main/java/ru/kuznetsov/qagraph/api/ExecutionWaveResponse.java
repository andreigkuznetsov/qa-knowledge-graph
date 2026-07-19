package ru.kuznetsov.qagraph.api;

import java.util.List;

public record ExecutionWaveResponse(
        int number,
        List<String> taskIds
) {
    public ExecutionWaveResponse {
        taskIds = List.copyOf(taskIds);
    }
}
