package ru.kuznetsov.qaip.roadmap.service;

import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RoadmapService {

    private static final Comparator<RemediationTask> TASK_ORDER =
            Comparator.comparing(RemediationTask::type)
                    .thenComparing(RemediationTask::targetNodeId)
                    .thenComparing(RemediationTask::id);

    public RoadmapReport plan(FindingsReport findingsReport) {
        Objects.requireNonNull(findingsReport, "findingsReport must not be null");

        Map<String, Finding> findingsByTaskId = new LinkedHashMap<>();
        Map<String, RemediationTask> tasksById = new LinkedHashMap<>();

        for (Finding finding : findingsReport.findings()) {
            RemediationTask task = toTask(finding);
            Finding existing = findingsByTaskId.putIfAbsent(task.id(), finding);

            if (existing == null) {
                tasksById.put(task.id(), task);
            } else if (!existing.equals(finding)) {
                throw new RoadmapTaskIdCollisionException(
                        task.id(),
                        existing,
                        finding
                );
            }
        }

        List<RemediationTask> tasks = tasksById.values().stream()
                .sorted(TASK_ORDER)
                .toList();

        return new RoadmapReport(
                true,
                findingsReport.schemaVersion(),
                summarize(tasks),
                tasks,
                findingsReport.summary()
        );
    }

    private RemediationTask toTask(Finding finding) {
        Objects.requireNonNull(finding, "finding must not be null");
        TaskTemplate template = template(finding.code());

        if (!template.expectedNodeType().equals(finding.nodeType())) {
            throw new InvalidFindingTargetException(
                    finding,
                    template.expectedNodeType()
            );
        }

        String normalizedNodeId = normalizeNodeId(finding);
        String taskId = "TASK-"
                + template.type().name().replace('_', '-')
                + "-" + normalizedNodeId;

        return new RemediationTask(
                taskId,
                template.type(),
                RemediationTaskStatus.PLANNED,
                finding.code(),
                finding.nodeId(),
                finding.nodeType(),
                template.description().formatted(finding.nodeId()),
                List.of()
        );
    }

    private TaskTemplate template(FindingCode code) {
        return switch (code) {
            case BUSINESS_RULE_WITHOUT_SCENARIO -> new TaskTemplate(
                    RemediationTaskType.CREATE_SCENARIO,
                    "BUSINESS_RULE",
                    "Create a scenario that covers business rule %s."
            );
            case SCENARIO_WITHOUT_TEST -> new TaskTemplate(
                    RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                    "SCENARIO",
                    "Create a test implementation that validates scenario %s."
            );
            case TEST_WITHOUT_CHECK -> new TaskTemplate(
                    RemediationTaskType.CREATE_CHECK,
                    "TEST_IMPLEMENTATION",
                    "Create at least one check for test implementation %s."
            );
        };
    }

    private String normalizeNodeId(Finding finding) {
        String nodeId = finding.nodeId();
        if (nodeId == null) {
            throw new InvalidFindingTargetException(finding);
        }

        String normalized = nodeId.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isEmpty()) {
            throw new InvalidFindingTargetException(finding);
        }
        return normalized;
    }

    private RoadmapSummary summarize(List<RemediationTask> tasks) {
        int planned = (int) tasks.stream()
                .filter(task -> task.status() == RemediationTaskStatus.PLANNED)
                .count();
        int withDependencies = (int) tasks.stream()
                .filter(task -> !task.dependsOn().isEmpty())
                .count();
        return new RoadmapSummary(tasks.size(), planned, withDependencies);
    }

    private record TaskTemplate(
            RemediationTaskType type,
            String expectedNodeType,
            String description
    ) {
    }
}
