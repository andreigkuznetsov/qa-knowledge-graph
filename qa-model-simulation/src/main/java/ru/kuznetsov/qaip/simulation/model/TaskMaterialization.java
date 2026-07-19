package ru.kuznetsov.qaip.simulation.model;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;

import java.util.Objects;

public record TaskMaterialization(String taskId, JsonNode futureNode) {

    public TaskMaterialization {
        Objects.requireNonNull(taskId, "taskId must not be null");
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        Objects.requireNonNull(futureNode, "futureNode must not be null");
        if (!futureNode.isObject()) {
            throw new SimulationException(
                    SimulationErrorCode.MATERIALIZATION_PAYLOAD_INVALID,
                    "futureNode must be a JSON object", taskId, null, null);
        }
        futureNode = futureNode.deepCopy();
    }

    @Override
    public JsonNode futureNode() {
        return futureNode.deepCopy();
    }
}
