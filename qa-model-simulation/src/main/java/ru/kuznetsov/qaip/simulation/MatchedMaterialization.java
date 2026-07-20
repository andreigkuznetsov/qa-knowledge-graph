package ru.kuznetsov.qaip.simulation;

import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.simulation.model.TaskMaterialization;

import java.util.Objects;

record MatchedMaterialization(
        TaskImpact taskImpact,
        TaskMaterialization taskMaterialization
) {
    MatchedMaterialization {
        Objects.requireNonNull(taskImpact, "taskImpact must not be null");
        Objects.requireNonNull(taskMaterialization,
                "taskMaterialization must not be null");
    }
}
