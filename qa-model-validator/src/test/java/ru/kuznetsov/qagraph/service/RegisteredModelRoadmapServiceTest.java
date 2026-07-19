package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.api.RoadmapResponseMapper;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.findings.service.FindingsService;
import ru.kuznetsov.qaip.roadmap.model.RemediationTask;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskStatus;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisteredModelRoadmapServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRunEachStageOnceReuseReportsAndPreserveOrder()
            throws Exception {
        Fixture fixture = fixture(populatedRoadmap());

        var response = fixture.service().plan("model-1");

        assertEquals("model-1", response.modelId());
        assertEquals(List.of(
                        "TASK-CREATE-SCENARIO-BR-001",
                        "TASK-CREATE-CHECK-TEST-001"
                ), response.tasks().stream()
                        .map(task -> task.id()).toList());
        assertEquals(2, response.sourceFindingsSummary().total());
        assertEquals(fixture.coverageReport().validation(),
                response.validation());
        verify(fixture.repository(), times(1)).findById("model-1");
        verify(fixture.coverageService(), times(1)).analyze(fixture.model());
        verify(fixture.findingsService(), times(1))
                .analyze(fixture.coverageReport());
        verify(fixture.findingsService(), never())
                .analyze(any(JsonNode.class));
        verify(fixture.roadmapService(), times(1))
                .plan(fixture.findingsReport());
        assertEquals(fixture.before(), fixture.model());
    }

    @Test
    void repeatedCallsShouldReturnEqualResponses() throws Exception {
        Fixture fixture = fixture(populatedRoadmap());

        assertEquals(
                fixture.service().plan("model-1"),
                fixture.service().plan("model-1")
        );
        verify(fixture.coverageService(), times(2)).analyze(fixture.model());
        verify(fixture.findingsService(), times(2))
                .analyze(fixture.coverageReport());
        verify(fixture.roadmapService(), times(2))
                .plan(fixture.findingsReport());
    }

    @Test
    void shouldMapEmptyRoadmap() throws Exception {
        Fixture fixture = fixture(new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(0, 0, 0),
                List.of(),
                new FindingsSummary(0, 0, 0, 0)
        ));

        var response = fixture.service().plan("model-1");

        assertEquals(0, response.summary().totalTasks());
        assertEquals(List.of(), response.tasks());
    }

    @Test
    void unknownAndBlankModelShouldUseExistingNotFoundBehavior() {
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        RegisteredModelRoadmapService service = service(
                repository,
                mock(CoverageService.class),
                mock(FindingsService.class),
                mock(RoadmapService.class)
        );

        assertThrows(QaModelNotFoundException.class,
                () -> service.plan("unknown"));
        assertThrows(QaModelNotFoundException.class,
                () -> service.plan(" "));
    }

    @Test
    void nullModelIdShouldFollowRepositoryConvention() {
        RegisteredModelRoadmapService service = service(
                new InMemoryQaModelRepository(),
                mock(CoverageService.class),
                mock(FindingsService.class),
                mock(RoadmapService.class)
        );

        assertThrows(NullPointerException.class, () -> service.plan(null));
    }

    @Test
    void invalidStoredModelShouldReuseValidationAndStopPipeline()
            throws Exception {
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        JsonNode model = objectMapper.readTree("{}");
        when(repository.findById("model-1")).thenReturn(Optional.of(model));
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        RoadmapService roadmapService = mock(RoadmapService.class);
        CoverageReport report = coverageReport(false);
        when(coverageService.analyze(model)).thenReturn(report);
        RegisteredModelRoadmapService service = service(
                repository,
                coverageService,
                findingsService,
                roadmapService
        );

        InvalidQaModelException exception = assertThrows(
                InvalidQaModelException.class,
                () -> service.plan("model-1")
        );

        assertEquals(report.validation(), exception.validationResult());
        verify(findingsService, never()).analyze(report);
        verify(roadmapService, never()).plan(any());
    }

    private Fixture fixture(RoadmapReport roadmapReport) throws Exception {
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        JsonNode before = model.deepCopy();
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        RoadmapService roadmapService = mock(RoadmapService.class);
        CoverageReport coverageReport = coverageReport(true);
        FindingsReport findingsReport = new FindingsReport(
                true,
                "0.1",
                roadmapReport.sourceFindingsSummary(),
                List.of(),
                coverageReport.validation()
        );
        when(repository.findById("model-1")).thenReturn(Optional.of(model));
        when(coverageService.analyze(model)).thenReturn(coverageReport);
        when(findingsService.analyze(coverageReport)).thenReturn(findingsReport);
        when(roadmapService.plan(findingsReport)).thenReturn(roadmapReport);
        return new Fixture(
                model,
                before,
                repository,
                coverageService,
                findingsService,
                roadmapService,
                coverageReport,
                findingsReport,
                service(repository, coverageService, findingsService, roadmapService)
        );
    }

    private RegisteredModelRoadmapService service(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService
    ) {
        return new RegisteredModelRoadmapService(
                repository,
                coverageService,
                findingsService,
                roadmapService,
                new RoadmapResponseMapper()
        );
    }

    private CoverageReport coverageReport(boolean valid) {
        QaModelValidationResult validation = new QaModelValidationResult(
                valid,
                valid ? "0.1" : null,
                new ValidationSummary(valid ? 0 : 1, 0, valid ? 0 : 1),
                List.of()
        );
        return new CoverageReport(
                valid,
                "0.4",
                valid ? "0.1" : null,
                Instant.EPOCH,
                null,
                List.of(),
                List.of(),
                validation
        );
    }

    private RoadmapReport populatedRoadmap() {
        return new RoadmapReport(
                true,
                "0.1",
                new RoadmapSummary(2, 2, 0),
                List.of(
                        task("TASK-CREATE-SCENARIO-BR-001",
                                RemediationTaskType.CREATE_SCENARIO,
                                "BR-001", "BUSINESS_RULE"),
                        task("TASK-CREATE-CHECK-TEST-001",
                                RemediationTaskType.CREATE_CHECK,
                                "TEST-001", "TEST_IMPLEMENTATION")
                ),
                new FindingsSummary(2, 1, 1, 0)
        );
    }

    private RemediationTask task(
            String id,
            RemediationTaskType type,
            String nodeId,
            String nodeType
    ) {
        var code = type == RemediationTaskType.CREATE_SCENARIO
                ? ru.kuznetsov.qaip.findings.model.FindingCode
                        .BUSINESS_RULE_WITHOUT_SCENARIO
                : ru.kuznetsov.qaip.findings.model.FindingCode.TEST_WITHOUT_CHECK;
        return new RemediationTask(
                id, type, RemediationTaskStatus.PLANNED, code,
                nodeId, nodeType, "Description", List.of()
        );
    }

    private record Fixture(
            JsonNode model,
            JsonNode before,
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService,
            CoverageReport coverageReport,
            FindingsReport findingsReport,
            RegisteredModelRoadmapService service
    ) {
    }
}
