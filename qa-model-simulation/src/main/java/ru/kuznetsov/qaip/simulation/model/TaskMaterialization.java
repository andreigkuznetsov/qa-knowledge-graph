package ru.kuznetsov.qaip.simulation.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record TaskMaterialization(String taskId, JsonNode futureNode) {

    public TaskMaterialization {
        Objects.requireNonNull(taskId, "taskId must not be null");
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        Objects.requireNonNull(futureNode, "futureNode must not be null");
        if (!futureNode.isObject()) {
            throw new IllegalArgumentException(
                    "futureNode must be a JSON object");
        }
        futureNode = futureNode.deepCopy();
    }

    @Override
    public JsonNode futureNode() {
        return futureNode.deepCopy();
    }
}
