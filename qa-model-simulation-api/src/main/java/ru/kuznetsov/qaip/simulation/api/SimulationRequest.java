package ru.kuznetsov.qaip.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

public record SimulationRequest(
        JsonNode currentModel,
        ImpactReport impactReport,
        TaskMaterializationSet taskMaterializationSet
) {
}
