package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
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

class SimulationInputValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final QaModelFingerprintCalculator fingerprints =
            new QaModelFingerprintCalculator();
    private final SimulationInputValidator validator =
            new SimulationInputValidator();

    @Test
    void shouldRejectNullInputsInRequiredOrder() {
        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> validator.validateAndMatch(null, null, null));
        JsonNode model = model("BUSINESS_RULE");
        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> validator.validateAndMatch(model, null, null));
        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> validator.validateAndMatch(model, report(), null));
    }

    @Test
    void shouldValidateVersionAndTopLevelShapeBeforeFingerprint() {
        JsonNode model = model("BUSINESS_RULE");
        assertFailure(SimulationErrorCode.UNSUPPORTED_MATERIALIZATION_VERSION,
                () -> validator.validateAndMatch(model, report(),
                        new TaskMaterializationSet("9", "bad", List.of())));
        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> validator.validateAndMatch(mapper.createObjectNode(),
                        report(), new TaskMaterializationSet(
                                "0.1", "bad", List.of())));
    }

    @Test
    void matchingFingerprintShouldAllowOrderedMatching() {
        JsonNode model = model("BUSINESS_RULE");
        TaskImpact second = impact("TASK-2", "BR-1", NodeType.BUSINESS_RULE,
                NodeType.SCENARIO);
        TaskImpact first = impact("TASK-1", "BR-1", NodeType.BUSINESS_RULE,
                NodeType.SCENARIO);
        ImpactReport report = report(second, first);
        var set = set(model,
                materialization("TASK-1", "SC-1", "SCENARIO"),
                materialization("TASK-2", "SC-2", "SCENARIO"));

        var matches = validator.validateAndMatch(model, report, set);

        assertEquals(List.of("TASK-2", "TASK-1"), matches.stream()
                .map(match -> match.taskImpact().taskId()).toList());
    }

    @Test
    void shouldRejectFingerprintMismatch() {
        JsonNode model = model("BUSINESS_RULE");
        assertFailure(SimulationErrorCode.BASE_MODEL_FINGERPRINT_MISMATCH,
                () -> validator.validateAndMatch(model, report(),
                        new TaskMaterializationSet("0.1", "wrong", List.of())));
    }

    @Test
    void shouldRejectDuplicateMissingAndUnknownTaskMaterializations() {
        JsonNode model = model("BUSINESS_RULE");
        TaskMaterialization item = materialization(
                "TASK-1", "SC-1", "SCENARIO");
        assertFailure(SimulationErrorCode.MATERIALIZATION_DUPLICATE_TASK,
                () -> validator.validateAndMatch(model, report(),
                        set(model, item, item)));
        assertFailure(SimulationErrorCode.MATERIALIZATION_MISSING,
                () -> validator.validateAndMatch(model, report(),
                        set(model)));
        assertFailure(SimulationErrorCode.MATERIALIZATION_UNKNOWN_TASK,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materialization("UNKNOWN", "SC-1", "SCENARIO"))));
    }

    @Test
    void shouldRejectMissingAndInvalidFutureNodeId() {
        JsonNode model = model("BUSINESS_RULE");
        assertFailure(SimulationErrorCode.MATERIALIZATION_INVALID_NODE_ID,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materializationWithoutId("TASK-1", "SCENARIO"))));
        assertFailure(SimulationErrorCode.MATERIALIZATION_INVALID_NODE_ID,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materialization("TASK-1", " invalid", "SCENARIO"))));
        assertFailure(SimulationErrorCode.MATERIALIZATION_INVALID_NODE_ID,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materialization("TASK-1", "A".repeat(121),
                                "SCENARIO"))));
    }

    @Test
    void shouldRejectCurrentAndFutureNodeIdCollisions() {
        JsonNode model = model("BUSINESS_RULE");
        assertFailure(SimulationErrorCode.MATERIALIZATION_NODE_ID_COLLISION,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materialization("TASK-1", "BR-1", "SCENARIO"))));

        ImpactReport twoTasks = report(
                impact("TASK-1", "BR-1", NodeType.BUSINESS_RULE,
                        NodeType.SCENARIO),
                impact("TASK-2", "BR-1", NodeType.BUSINESS_RULE,
                        NodeType.SCENARIO));
        assertFailure(SimulationErrorCode.MATERIALIZATION_NODE_ID_COLLISION,
                () -> validator.validateAndMatch(model, twoTasks, set(model,
                        materialization("TASK-1", "SC-X", "SCENARIO"),
                        materialization("TASK-2", "SC-X", "SCENARIO"))));
    }

    @Test
    void shouldRejectMissingOrMismatchedFutureNodeType() {
        JsonNode model = model("BUSINESS_RULE");
        var withoutType = new TaskMaterialization("TASK-1",
                mapper.createObjectNode().put("id", "SC-1"));
        assertFailure(SimulationErrorCode.MATERIALIZATION_NODE_TYPE_MISMATCH,
                () -> validator.validateAndMatch(model, report(),
                        set(model, withoutType)));
        assertFailure(SimulationErrorCode.MATERIALIZATION_NODE_TYPE_MISMATCH,
                () -> validator.validateAndMatch(model, report(), set(model,
                        materialization("TASK-1", "SC-1", "CHECK"))));
    }

    @Test
    void shouldRejectMissingAndMismatchedAffectedTarget() {
        JsonNode model = model("BUSINESS_RULE");
        ImpactReport missing = report(impact("TASK-1", "ABSENT",
                NodeType.BUSINESS_RULE, NodeType.SCENARIO));
        assertFailure(SimulationErrorCode.TARGET_NODE_MISSING,
                () -> validator.validateAndMatch(model, missing, set(model,
                        materialization("TASK-1", "SC-1", "SCENARIO"))));

        JsonNode wrongType = model("SCENARIO");
        assertFailure(SimulationErrorCode.TARGET_NODE_TYPE_MISMATCH,
                () -> validator.validateAndMatch(wrongType, report(),
                        set(wrongType, materialization(
                                "TASK-1", "SC-1", "SCENARIO"))));
    }

    @Test
    void shouldRejectUnsupportedImpactShape() {
        JsonNode model = model("BUSINESS_RULE");
        TaskImpact inconsistent = new TaskImpact(
                "TASK-1", RemediationTaskType.CREATE_SCENARIO, "BR-1",
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                new StructuralGap(NodeType.BUSINESS_RULE, NodeType.SCENARIO,
                        RelationshipType.COVERS, RelationEndpointRole.TARGET),
                new ExpectedStructuralChange(
                        ImpactChangeType.CREATE_RELATED_NODE, NodeType.CHECK,
                        RelationshipType.COVERS, "BR-1",
                        RelationEndpointRole.TARGET,
                        ResolutionExpectation
                                .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION),
                1, List.of());
        assertFailure(SimulationErrorCode.UNSUPPORTED_IMPACT_CHANGE,
                () -> validator.validateAndMatch(model, report(inconsistent),
                        set(model, materialization(
                                "TASK-1", "SC-1", "CHECK"))));
    }

    @Test
    void malformedCurrentNodeShouldBeStableInputFailure() {
        var model = mapper.createObjectNode();
        model.putArray("nodes").addObject().put("id", "BR-1");
        model.putArray("relationships");
        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> validator.validateAndMatch(model, report(),
                        new TaskMaterializationSet("0.1", "ignored", List.of())));
    }

    private JsonNode model(String targetType) {
        var model = mapper.createObjectNode();
        model.put("schemaVersion", "0.1");
        model.putArray("nodes").addObject()
                .put("id", "BR-1").put("type", targetType);
        model.putArray("relationships");
        return model;
    }

    private ImpactReport report(TaskImpact... impacts) {
        List<TaskImpact> values = impacts.length == 0
                ? List.of(impact("TASK-1", "BR-1", NodeType.BUSINESS_RULE,
                NodeType.SCENARIO)) : List.of(impacts);
        return new ImpactReport(true, "impact-0.1",
                new ImpactSummary(values.size(), values.size(), 1, 0, 0,
                        1, 0, 0), values);
    }

    private TaskImpact impact(
            String taskId, String targetId, NodeType targetType,
            NodeType futureType
    ) {
        return new TaskImpact(
                taskId, RemediationTaskType.CREATE_SCENARIO, targetId,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO,
                new StructuralGap(targetType, futureType,
                        RelationshipType.COVERS, RelationEndpointRole.TARGET),
                new ExpectedStructuralChange(
                        ImpactChangeType.CREATE_RELATED_NODE, futureType,
                        RelationshipType.COVERS, targetId,
                        RelationEndpointRole.TARGET,
                        ResolutionExpectation
                                .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION),
                1, List.of());
    }

    private TaskMaterialization materialization(
            String taskId, String nodeId, String nodeType
    ) {
        return new TaskMaterialization(taskId, mapper.createObjectNode()
                .put("id", nodeId).put("type", nodeType).put("name", "New"));
    }

    private TaskMaterialization materializationWithoutId(
            String taskId, String nodeType
    ) {
        return new TaskMaterialization(taskId, mapper.createObjectNode()
                .put("type", nodeType).put("name", "New"));
    }

    private TaskMaterializationSet set(
            JsonNode model, TaskMaterialization... materializations
    ) {
        return new TaskMaterializationSet("0.1",
                fingerprints.calculate(model), List.of(materializations));
    }

    private void assertFailure(SimulationErrorCode code, Runnable action) {
        SimulationException exception = assertThrows(
                SimulationException.class, action::run);
        assertEquals(code, exception.code());
    }
}
