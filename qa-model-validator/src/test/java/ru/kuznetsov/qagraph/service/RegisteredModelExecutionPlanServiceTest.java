package ru.kuznetsov.qagraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.api.ExecutionPlanResponseMapper;
import ru.kuznetsov.qagraph.api.RegisteredModelExecutionPlanResponse;
import ru.kuznetsov.qagraph.repository.InMemoryQaModelRepository;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.execution.model.ExecutionPlan;
import ru.kuznetsov.qaip.execution.model.ExecutionPlanSummary;
import ru.kuznetsov.qaip.execution.model.ExecutionWave;
import ru.kuznetsov.qaip.execution.service.ExecutionPlanner;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;
import ru.kuznetsov.qaip.findings.service.FindingsService;
import ru.kuznetsov.qaip.roadmap.model.RoadmapReport;
import ru.kuznetsov.qaip.roadmap.model.RoadmapSummary;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegisteredModelExecutionPlanServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRunEveryStageOnceWithExactReportInstances() throws Exception {
        Fixture fixture = fixture(populatedPlan());

        RegisteredModelExecutionPlanResponse response =
                fixture.service().plan("model-1");

        assertEquals("model-1", response.modelId());
        assertEquals(List.of(2, 1), response.waves().stream()
                .map(wave -> wave.number()).toList());
        assertEquals(List.of("TASK-Z", "TASK-A"),
                response.waves().getFirst().taskIds());
        assertEquals(fixture.coverageReport().validation(),
                response.validation());
        assertEquals(fixture.modelBefore(), fixture.model());
        assertEquals(fixture.roadmapBefore(), fixture.roadmapReport());

        verify(fixture.repository(), times(1)).findById("model-1");
        verify(fixture.coverageService(), times(1))
                .analyze(same(fixture.model()));
        verify(fixture.findingsService(), times(1))
                .analyze(same(fixture.coverageReport()));
        verify(fixture.findingsService(), never()).analyze(any(JsonNode.class));
        verify(fixture.roadmapService(), times(1))
                .plan(same(fixture.findingsReport()));
        verify(fixture.executionPlanner(), times(1))
                .plan(same(fixture.roadmapReport()));
        verify(fixture.responseMapper(), times(1)).map(
                "model-1",
                fixture.executionPlan(),
                fixture.coverageReport().validation()
        );
        verify(fixture.repository(), never()).save(any(), anyInt());
    }

    @Test
    void shouldMapEmptyPlanAndReturnEqualResponsesForRepeatedCalls()
            throws Exception {
        Fixture fixture = fixture(emptyPlan());

        var first = fixture.service().plan("model-1");
        var second = fixture.service().plan("model-1");

        assertEquals(first, second);
        assertEquals(0, first.summary().totalTasks());
        assertEquals(0, first.summary().totalWaves());
        assertEquals(List.of(), first.waves());
        assertEquals(0, first.sourceRoadmapSummary().totalTasks());
        verify(fixture.repository(), times(2)).findById("model-1");
        verify(fixture.coverageService(), times(2))
                .analyze(same(fixture.model()));
        verify(fixture.findingsService(), times(2))
                .analyze(same(fixture.coverageReport()));
        verify(fixture.roadmapService(), times(2))
                .plan(same(fixture.findingsReport()));
        verify(fixture.executionPlanner(), times(2))
                .plan(same(fixture.roadmapReport()));
        verify(fixture.responseMapper(), times(2)).map(
                "model-1",
                fixture.executionPlan(),
                fixture.coverageReport().validation()
        );
    }

    @Test
    void unknownAndBlankModelShouldUseExistingNotFoundBehavior() {
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        RegisteredModelExecutionPlanService service = service(
                repository,
                mock(CoverageService.class),
                mock(FindingsService.class),
                mock(RoadmapService.class),
                mock(ExecutionPlanner.class),
                mock(ExecutionPlanResponseMapper.class)
        );

        assertThrows(QaModelNotFoundException.class,
                () -> service.plan("unknown"));
        assertThrows(QaModelNotFoundException.class,
                () -> service.plan(" "));
    }

    @Test
    void nullModelIdShouldFollowRepositoryConvention() {
        RegisteredModelExecutionPlanService service = service(
                new InMemoryQaModelRepository(),
                mock(CoverageService.class),
                mock(FindingsService.class),
                mock(RoadmapService.class),
                mock(ExecutionPlanner.class),
                mock(ExecutionPlanResponseMapper.class)
        );

        assertThrows(NullPointerException.class, () -> service.plan(null));
    }

    @Test
    void invalidStoredModelShouldReuseValidationAndStopPipeline()
            throws Exception {
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        JsonNode model = objectMapper.readTree("{}");
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        RoadmapService roadmapService = mock(RoadmapService.class);
        ExecutionPlanner executionPlanner = mock(ExecutionPlanner.class);
        ExecutionPlanResponseMapper responseMapper =
                mock(ExecutionPlanResponseMapper.class);
        CoverageReport report = coverageReport(false);
        when(repository.findById("model-1")).thenReturn(Optional.of(model));
        when(coverageService.analyze(model)).thenReturn(report);
        RegisteredModelExecutionPlanService service = service(
                repository,
                coverageService,
                findingsService,
                roadmapService,
                executionPlanner,
                responseMapper
        );

        InvalidQaModelException exception = assertThrows(
                InvalidQaModelException.class,
                () -> service.plan("model-1")
        );

        assertEquals(report.validation(), exception.validationResult());
        verify(coverageService, times(1)).analyze(model);
        verifyNoInteractions(findingsService, roadmapService,
                executionPlanner, responseMapper);
    }

    private Fixture fixture(ExecutionPlan executionPlan) throws Exception {
        JsonNode model = objectMapper.readTree(
                "{\"nodes\":[],\"relationships\":[]}"
        );
        CoverageReport coverageReport = coverageReport(true);
        FindingsReport findingsReport = new FindingsReport(
                true,
                "0.1",
                new FindingsSummary(2, 1, 1, 0),
                List.of(),
                coverageReport.validation()
        );
        RoadmapReport roadmapReport = new RoadmapReport(
                true,
                "0.1",
                executionPlan.sourceRoadmapSummary(),
                List.of(),
                findingsReport.summary()
        );
        InMemoryQaModelRepository repository = mock(InMemoryQaModelRepository.class);
        CoverageService coverageService = mock(CoverageService.class);
        FindingsService findingsService = mock(FindingsService.class);
        RoadmapService roadmapService = mock(RoadmapService.class);
        ExecutionPlanner executionPlanner = mock(ExecutionPlanner.class);
        ExecutionPlanResponseMapper responseMapper =
                mock(ExecutionPlanResponseMapper.class);
        ExecutionPlanResponseMapper realMapper = new ExecutionPlanResponseMapper();

        when(repository.findById("model-1")).thenReturn(Optional.of(model));
        when(coverageService.analyze(model)).thenReturn(coverageReport);
        when(findingsService.analyze(coverageReport)).thenReturn(findingsReport);
        when(roadmapService.plan(findingsReport)).thenReturn(roadmapReport);
        when(executionPlanner.plan(roadmapReport)).thenReturn(executionPlan);
        when(responseMapper.map("model-1", executionPlan,
                coverageReport.validation())).thenReturn(
                        realMapper.map("model-1", executionPlan,
                                coverageReport.validation())
                );

        return new Fixture(
                model,
                model.deepCopy(),
                repository,
                coverageService,
                findingsService,
                roadmapService,
                executionPlanner,
                responseMapper,
                coverageReport,
                findingsReport,
                roadmapReport,
                new RoadmapReport(
                        roadmapReport.planned(),
                        roadmapReport.schemaVersion(),
                        roadmapReport.summary(),
                        roadmapReport.tasks(),
                        roadmapReport.sourceFindingsSummary()
                ),
                executionPlan,
                service(repository, coverageService, findingsService,
                        roadmapService, executionPlanner, responseMapper)
        );
    }

    private RegisteredModelExecutionPlanService service(
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService,
            ExecutionPlanner executionPlanner,
            ExecutionPlanResponseMapper responseMapper
    ) {
        return new RegisteredModelExecutionPlanService(
                repository,
                coverageService,
                findingsService,
                roadmapService,
                executionPlanner,
                responseMapper
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

    private ExecutionPlan populatedPlan() {
        return new ExecutionPlan(
                true,
                "0.1",
                new ExecutionPlanSummary(3, 2, 2, 1, 2),
                List.of(
                        new ExecutionWave(2, List.of("TASK-Z", "TASK-A")),
                        new ExecutionWave(1, List.of("TASK-B"))
                ),
                new RoadmapSummary(3, 3, 1)
        );
    }

    private ExecutionPlan emptyPlan() {
        return new ExecutionPlan(
                true,
                "0.1",
                new ExecutionPlanSummary(0, 0, 0, 0, 0),
                List.of(),
                new RoadmapSummary(0, 0, 0)
        );
    }

    private record Fixture(
            JsonNode model,
            JsonNode modelBefore,
            InMemoryQaModelRepository repository,
            CoverageService coverageService,
            FindingsService findingsService,
            RoadmapService roadmapService,
            ExecutionPlanner executionPlanner,
            ExecutionPlanResponseMapper responseMapper,
            CoverageReport coverageReport,
            FindingsReport findingsReport,
            RoadmapReport roadmapReport,
            RoadmapReport roadmapBefore,
            ExecutionPlan executionPlan,
            RegisteredModelExecutionPlanService service
    ) {
    }
}
