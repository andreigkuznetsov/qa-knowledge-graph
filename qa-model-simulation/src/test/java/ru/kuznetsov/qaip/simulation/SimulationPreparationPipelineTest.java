package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.impact.model.ExpectedStructuralChange;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.impact.model.ImpactSummary;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.impact.model.ResolutionExpectation;
import ru.kuznetsov.qaip.impact.model.StructuralGap;
import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;
import ru.kuznetsov.qaip.simulation.model.TaskMaterialization;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationPreparationPipelineTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldValidateMatchAndMaterializeWithoutPublicSimulationResult() {
        var model = mapper.createObjectNode();
        model.putArray("nodes").addObject()
                .put("id", "BR-1").put("type", "BUSINESS_RULE")
                .put("name", "Rule");
        model.putArray("relationships");
        TaskImpact impact = impact();
        ImpactReport report = new ImpactReport(
                true, "impact-0.1",
                new ImpactSummary(1, 1, 1, 0, 0, 1, 0, 0),
                List.of(impact));
        var futureNode = mapper.createObjectNode()
                .put("id", "SC-1").put("type", "SCENARIO")
                .put("name", "Scenario");
        var set = new TaskMaterializationSet(
                "0.1", new QaModelFingerprintCalculator().calculate(model),
                List.of(new TaskMaterialization("TASK-1", futureNode)));

        var preparation = new SimulationPreparationPipeline()
                .prepareCandidate(model, report, set);
        var candidate = preparation.candidateModel();

        assertEquals(2, candidate.path("nodes").size());
        assertEquals("SC-1", candidate.path("nodes").get(1)
                .path("id").asText());
        assertEquals("SC-1", candidate.path("relationships").get(0)
                .path("from").asText());
        assertEquals("BR-1", candidate.path("relationships").get(0)
                .path("to").asText());

        var wrongFingerprint = new TaskMaterializationSet(
                "0.1", "wrong", set.materializations());
        SimulationException exception = assertThrows(
                SimulationException.class,
                () -> new SimulationPreparationPipeline().prepareCandidate(
                        model, report, wrongFingerprint));
        assertEquals(SimulationErrorCode.BASE_MODEL_FINGERPRINT_MISMATCH,
                exception.code());
    }

    private TaskImpact impact() {
        return new TaskImpact(
                "TASK-1", RemediationTaskType.CREATE_SCENARIO, "BR-1",
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                new StructuralGap(
                        NodeType.BUSINESS_RULE, NodeType.SCENARIO,
                        RelationshipType.COVERS, RelationEndpointRole.TARGET),
                new ExpectedStructuralChange(
                        ImpactChangeType.CREATE_RELATED_NODE,
                        NodeType.SCENARIO, RelationshipType.COVERS, "BR-1",
                        RelationEndpointRole.TARGET,
                        ResolutionExpectation
                                .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION),
                1, List.of());
    }
}
