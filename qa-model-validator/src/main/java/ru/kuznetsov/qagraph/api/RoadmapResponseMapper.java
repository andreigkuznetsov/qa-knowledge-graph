package ru.kuznetsov.qagraph.api;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;

@Component
public class RoadmapResponseMapper {

    public RegisteredModelRoadmapResponse map(
            String modelId,
            RoadmapReport report,
            QaModelValidationResult validation
    ) {
        return new RegisteredModelRoadmapResponse(
                modelId,
                report.planned(),
                report.schemaVersion(),
                new RoadmapSummaryResponse(
                        report.summary().totalTasks(),
                        report.summary().plannedTasks(),
                        report.summary().tasksWithDependencies()
                ),
                report.tasks().stream().map(this::map).toList(),
                map(report.sourceFindingsSummary()),
                validation
        );
    }

    private RemediationTaskResponse map(RemediationTask task) {
        return new RemediationTaskResponse(
                task.id(),
                task.type().name(),
                task.status().name(),
                task.sourceFindingCode().name(),
                task.targetNodeId(),
                task.targetNodeType(),
                task.description(),
                task.dependsOn()
        );
    }

    private FindingsSummaryResponse map(FindingsSummary summary) {
        return new FindingsSummaryResponse(
                summary.total(),
                summary.high(),
                summary.medium(),
                summary.low()
        );
    }
}
