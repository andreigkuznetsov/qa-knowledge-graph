package ru.kuznetsov.qaip.roadmap.service;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.findings.model.FindingSeverity;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapServiceTest {

    private final RoadmapService service = new RoadmapService();

    @Test
    void emptyFindingsShouldProduceSuccessfulEmptyRoadmap() {
        FindingsReport findings = report(List.of());

        var roadmap = service.plan(findings);

        assertTrue(roadmap.planned());
        assertEquals("0.1", roadmap.schemaVersion());
        assertEquals(0, roadmap.summary().totalTasks());
        assertEquals(0, roadmap.summary().plannedTasks());
        assertEquals(0, roadmap.summary().tasksWithDependencies());
        assertTrue(roadmap.tasks().isEmpty());
        assertSame(findings.summary(), roadmap.sourceFindingsSummary());
    }

    @Test
    void shouldMapEveryFindingToExactTaskContract() {
        var roadmap = service.plan(report(List.of(
                testFinding("TEST-001"),
                ruleFinding("BR-001"),
                scenarioFinding("SC-001")
        )));

        var scenarioTask = roadmap.tasks().get(0);
        assertEquals("TASK-CREATE-SCENARIO-BR-001", scenarioTask.id());
        assertEquals(RemediationTaskType.CREATE_SCENARIO, scenarioTask.type());
        assertEquals(RemediationTaskStatus.PLANNED, scenarioTask.status());
        assertEquals(FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                scenarioTask.sourceFindingCode());
        assertEquals("BR-001", scenarioTask.targetNodeId());
        assertEquals("BUSINESS_RULE", scenarioTask.targetNodeType());
        assertEquals("Create a scenario that covers business rule BR-001.",
                scenarioTask.description());

        var testImplementationTask = roadmap.tasks().get(1);
        assertEquals("TASK-CREATE-TEST-IMPLEMENTATION-SC-001",
                testImplementationTask.id());
        assertEquals(RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                testImplementationTask.type());
        assertEquals(
                "Create a test implementation that validates scenario SC-001.",
                testImplementationTask.description()
        );

        var checkTask = roadmap.tasks().get(2);
        assertEquals("TASK-CREATE-CHECK-TEST-001", checkTask.id());
        assertEquals(RemediationTaskType.CREATE_CHECK, checkTask.type());
        assertEquals(
                "Create at least one check for test implementation TEST-001.",
                checkTask.description()
        );

        roadmap.tasks().forEach(task -> {
            assertEquals(RemediationTaskStatus.PLANNED, task.status());
            assertTrue(task.dependsOn().isEmpty());
        });
    }

    @Test
    void shouldSortByLifecycleTypeThenTargetNodeId() {
        var roadmap = service.plan(report(List.of(
                testFinding("TEST-002"),
                scenarioFinding("SC-002"),
                ruleFinding("BR-002"),
                testFinding("TEST-001"),
                scenarioFinding("SC-001"),
                ruleFinding("BR-001")
        )));

        assertEquals(List.of(
                        "TASK-CREATE-SCENARIO-BR-001",
                        "TASK-CREATE-SCENARIO-BR-002",
                        "TASK-CREATE-TEST-IMPLEMENTATION-SC-001",
                        "TASK-CREATE-TEST-IMPLEMENTATION-SC-002",
                        "TASK-CREATE-CHECK-TEST-001",
                        "TASK-CREATE-CHECK-TEST-002"
                ), roadmap.tasks().stream().map(task -> task.id()).toList());
    }

    @Test
    void differentInputOrderAndRepeatedPlanningShouldBeEqual() {
        FindingsReport first = report(List.of(
                testFinding("TEST-001"),
                ruleFinding("BR-001"),
                scenarioFinding("SC-001")
        ));
        FindingsReport second = report(List.of(
                scenarioFinding("SC-001"),
                testFinding("TEST-001"),
                ruleFinding("BR-001")
        ));

        assertEquals(service.plan(first), service.plan(second));
        assertEquals(service.plan(first), service.plan(first));
    }

    @Test
    void duplicateIdenticalFindingShouldProduceOneTask() {
        Finding finding = ruleFinding("BR-001");

        var roadmap = service.plan(report(List.of(finding, finding)));

        assertEquals(1, roadmap.tasks().size());
        assertEquals(1, roadmap.summary().totalTasks());
        assertEquals(1, roadmap.summary().plannedTasks());
    }

    @Test
    void normalizedIdsShouldBeStableHumanReadableAndDistinct() {
        String first = service.plan(report(List.of(ruleFinding(" br:001 "))))
                .tasks().getFirst().id();
        String repeated = service.plan(report(List.of(ruleFinding(" br:001 "))))
                .tasks().getFirst().id();
        String different = service.plan(report(List.of(ruleFinding("BR-002"))))
                .tasks().getFirst().id();

        assertEquals("TASK-CREATE-SCENARIO-BR-001", first);
        assertEquals(first, repeated);
        assertNotEquals(first, different);
    }

    @Test
    void normalizedIdCollisionBetweenDifferentFindingsShouldFail() {
        assertThrows(
                RoadmapTaskIdCollisionException.class,
                () -> service.plan(report(List.of(
                        ruleFinding("BR/001"),
                        ruleFinding("BR-001")
                )))
        );
    }

    @Test
    void incompatibleFindingTargetShouldFailWithDiagnosticMessage() {
        Finding malformed = new Finding(
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                FindingSeverity.HIGH,
                "BR-001",
                "SCENARIO",
                "message",
                "recommendation"
        );

        InvalidFindingTargetException exception = assertThrows(
                InvalidFindingTargetException.class,
                () -> service.plan(report(List.of(malformed)))
        );

        assertTrue(exception.getMessage().contains(
                "BUSINESS_RULE_WITHOUT_SCENARIO"));
        assertTrue(exception.getMessage().contains("SCENARIO"));
        assertTrue(exception.getMessage().contains("BUSINESS_RULE"));
        assertTrue(exception.getMessage().contains("BR-001"));
    }

    @Test
    void unusableNodeIdShouldFailExplicitly() {
        assertThrows(
                InvalidFindingTargetException.class,
                () -> service.plan(report(List.of(ruleFinding("///"))))
        );
    }

    @Test
    void taskAndDependencyCollectionsShouldBeImmutable() {
        var roadmap = service.plan(report(List.of(ruleFinding("BR-001"))));

        assertThrows(
                UnsupportedOperationException.class,
                () -> roadmap.tasks().add(roadmap.tasks().getFirst())
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> roadmap.tasks().getFirst().dependsOn().add("TASK-X")
        );
    }

    @Test
    void inputReportShouldNotBeMutated() {
        List<Finding> mutableFindings = new ArrayList<>();
        mutableFindings.add(ruleFinding("BR-001"));
        FindingsReport report = report(mutableFindings);
        FindingsReport before = report(List.copyOf(report.findings()));

        service.plan(report);

        assertEquals(before, report);
    }

    @Test
    void shouldRejectNullInputAndKeepExactEnumVocabulary() {
        assertThrows(NullPointerException.class, () -> service.plan(null));
        assertArrayEquals(
                new RemediationTaskType[]{
                        RemediationTaskType.CREATE_SCENARIO,
                        RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                        RemediationTaskType.CREATE_CHECK
                },
                RemediationTaskType.values()
        );
        assertArrayEquals(
                new RemediationTaskStatus[]{RemediationTaskStatus.PLANNED},
                RemediationTaskStatus.values()
        );
    }

    private FindingsReport report(List<Finding> findings) {
        int high = (int) findings.stream()
                .filter(finding -> finding.severity() == FindingSeverity.HIGH)
                .count();
        int medium = findings.size() - high;
        return new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(findings.size(), high, medium, 0),
                findings,
                null
        );
    }

    private Finding ruleFinding(String nodeId) {
        return finding(
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                FindingSeverity.HIGH,
                nodeId,
                "BUSINESS_RULE"
        );
    }

    private Finding scenarioFinding(String nodeId) {
        return finding(
                FindingCode.SCENARIO_WITHOUT_TEST,
                FindingSeverity.MEDIUM,
                nodeId,
                "SCENARIO"
        );
    }

    private Finding testFinding(String nodeId) {
        return finding(
                FindingCode.TEST_WITHOUT_CHECK,
                FindingSeverity.MEDIUM,
                nodeId,
                "TEST_IMPLEMENTATION"
        );
    }

    private Finding finding(
            FindingCode code,
            FindingSeverity severity,
            String nodeId,
            String nodeType
    ) {
        return new Finding(
                code,
                severity,
                nodeId,
                nodeType,
                "message",
                "recommendation"
        );
    }
}
