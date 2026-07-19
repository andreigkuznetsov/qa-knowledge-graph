package ru.kuznetsov.qagraph.api;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;

@Component
public class ExecutionPlanResponseMapper {

    public RegisteredModelExecutionPlanResponse map(
            String modelId,
            ExecutionPlan plan,
            QaModelValidationResult validation
    ) {
        return new RegisteredModelExecutionPlanResponse(
                modelId,
                plan.planned(),
                plan.schemaVersion(),
                new ExecutionPlanSummaryResponse(
                        plan.summary().totalTasks(),
                        plan.summary().totalWaves(),
                        plan.summary().parallelizableTasks(),
                        plan.summary().sequentialTasks(),
                        plan.summary().maximumParallelism()
                ),
                plan.waves().stream().map(this::map).toList(),
                new RoadmapSummaryResponse(
                        plan.sourceRoadmapSummary().totalTasks(),
                        plan.sourceRoadmapSummary().plannedTasks(),
                        plan.sourceRoadmapSummary().tasksWithDependencies()
                ),
                validation
        );
    }

    private ExecutionWaveResponse map(ExecutionWave wave) {
        return new ExecutionWaveResponse(wave.number(), wave.taskIds());
    }
}
