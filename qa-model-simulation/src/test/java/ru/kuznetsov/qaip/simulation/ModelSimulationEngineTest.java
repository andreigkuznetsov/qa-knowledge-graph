package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
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
import ru.kuznetsov.qaip.simulation.model.SimulationResult;
import ru.kuznetsov.qaip.simulation.model.TaskMaterialization;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSimulationEngineTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCompletePublicSimulationWithOrderedAppliedMaterializations()
            throws Exception {
        JsonNode currentModel = fixture();
        JsonNode currentBefore = currentModel.deepCopy();
        ImpactReport report = impactReport(
                testImpact(), checkImpact());
        ObjectNode testNode = testNode();
        ObjectNode checkNode = checkNode();
        TaskMaterializationSet set = set(currentModel,
                new TaskMaterialization("TASK-CHECK", checkNode),
                new TaskMaterialization("TASK-TEST", testNode));
        CountingValidationEngine validationEngine =
                new CountingValidationEngine();
        ModelSimulationEngine engine = new ModelSimulationEngine(
                new SimulationPreparationPipeline(), validationEngine,
                new QaModelFingerprintCalculator());

        SimulationResult result = engine.simulate(currentModel, report, set);

        assertTrue(result.validation().valid());
        assertTrue(result.validation().summary().warnings() > 0);
        assertEquals(1, validationEngine.invocations());
        assertEquals(SimulationResult.CONTRACT_VERSION,
                result.simulationContractVersion());
        assertEquals(set.baseModelFingerprint(), result.baseModelFingerprint());
        assertEquals(new QaModelFingerprintCalculator().calculate(
                result.futureModel()), result.futureModelFingerprint());
        assertNotEquals(result.baseModelFingerprint(),
                result.futureModelFingerprint());
        assertEquals(List.of("TASK-TEST", "TASK-CHECK"),
                result.appliedMaterializations().stream()
                        .map(applied -> applied.taskId()).toList());
        assertEquals(List.of("TEST-CANCEL-NEW", "CHECK-CANCEL-STATUS"),
                result.appliedMaterializations().stream()
                        .map(applied -> applied.createdNodeId()).toList());
        assertTrue(result.appliedMaterializations().stream()
                .allMatch(applied -> relationshipIds(result.futureModel())
                        .contains(applied.createdRelationshipId())));
        assertEquals(6, result.futureModel().path("nodes").size());
        assertEquals(6, result.futureModel().path("relationships").size());
        assertEquals(currentBefore, currentModel);

        SimulationResult originalTestResult = new ModelSimulationEngine()
                .simulate(currentModel, impactReport(testImpact()),
                        set(currentModel, new TaskMaterialization(
                                "TASK-TEST", testNode())));
        ObjectNode changedTestNode = testNode();
        changedTestNode.put("name", "Changed cancellation API test");
        SimulationResult changedResult = new ModelSimulationEngine().simulate(
                currentModel, impactReport(testImpact()),
                set(currentModel, new TaskMaterialization(
                        "TASK-TEST", changedTestNode)));
        assertNotEquals(originalTestResult.futureModelFingerprint(),
                changedResult.futureModelFingerprint());
    }

    @Test
    void shouldRejectInvalidCandidateWithImmutableDeterministicValidation()
            throws Exception {
        JsonNode currentModel = fixture();
        JsonNode before = currentModel.deepCopy();
        ObjectNode incompleteTest = mapper.createObjectNode()
                .put("id", "TEST-CANCEL-INVALID")
                .put("type", "TEST_IMPLEMENTATION")
                .put("name", "Incomplete cancellation test");
        TaskMaterializationSet set = set(currentModel,
                new TaskMaterialization("TASK-TEST", incompleteTest));
        ImpactReport report = impactReport(testImpact());
        CountingValidationEngine validationEngine =
                new CountingValidationEngine();
        ModelSimulationEngine engine = new ModelSimulationEngine(
                new SimulationPreparationPipeline(), validationEngine,
                new QaModelFingerprintCalculator());

        SimulationException first = assertThrows(
                SimulationException.class,
                () -> engine.simulate(currentModel, report, set));
        SimulationException second = assertThrows(
                SimulationException.class,
                () -> engine.simulate(currentModel, report, set));

        assertEquals(SimulationErrorCode.CANDIDATE_MODEL_VALIDATION_FAILED,
                first.code());
        assertFalse(first.validation().valid());
        assertFalse(first.validation().issues().isEmpty());
        assertEquals(first.getMessage(), second.getMessage());
        assertEquals(first.validation(), second.validation());
        assertEquals(2, validationEngine.invocations());
        assertThrows(UnsupportedOperationException.class,
                () -> first.validation().issues().clear());
        assertEquals(before, currentModel);
        assertEquals("Incomplete cancellation test",
                incompleteTest.path("name").asText());
    }

    @Test
    void shouldKeepSuccessfulResultsAndInputsMutablyIsolated()
            throws Exception {
        ObjectNode currentModel = (ObjectNode) fixture();
        ObjectNode sourceFutureNode = testNode();
        TaskMaterializationSet set = set(currentModel,
                new TaskMaterialization("TASK-TEST", sourceFutureNode));
        ModelSimulationEngine engine = new ModelSimulationEngine();
        SimulationResult first = engine.simulate(
                currentModel, impactReport(testImpact()), set);
        SimulationResult second = engine.simulate(
                currentModel, impactReport(testImpact()), set);

        assertEquals(first, second);
        assertNotSame(first.futureModel(), second.futureModel());
        currentModel.put("schemaVersion", "changed");
        sourceFutureNode.put("name", "Changed source");
        ObjectNode exposed = (ObjectNode) first.futureModel();
        exposed.put("schemaVersion", "changed result");

        assertEquals("0.1", first.futureModel()
                .path("schemaVersion").asText());
        assertEquals("Cancellation API test",
                first.futureModel().path("nodes").get(4)
                        .path("name").asText());
        assertEquals("0.1", second.futureModel()
                .path("schemaVersion").asText());
    }

    @Test
    void shouldExposeRepresentativeBoundaryFailuresThroughSimulationException()
            throws Exception {
        JsonNode model = fixture();
        ImpactReport report = impactReport(testImpact());
        TaskMaterialization materialization =
                new TaskMaterialization("TASK-TEST", testNode());
        String fingerprint = new QaModelFingerprintCalculator().calculate(model);
        ModelSimulationEngine engine = new ModelSimulationEngine();

        assertFailure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                () -> engine.simulate(null, report,
                        set(model, materialization)));
        assertFailure(SimulationErrorCode.UNSUPPORTED_MATERIALIZATION_VERSION,
                () -> engine.simulate(model, report,
                        new TaskMaterializationSet(
                                "9.0", fingerprint, List.of(materialization))));
        assertFailure(SimulationErrorCode.BASE_MODEL_FINGERPRINT_MISMATCH,
                () -> engine.simulate(model, report,
                        new TaskMaterializationSet(
                                "0.1", "wrong", List.of(materialization))));
        assertFailure(SimulationErrorCode.MATERIALIZATION_MISSING,
                () -> engine.simulate(model, report,
                        new TaskMaterializationSet(
                                "0.1", fingerprint, List.of())));
        assertFailure(SimulationErrorCode.MATERIALIZATION_UNKNOWN_TASK,
                () -> engine.simulate(model, report,
                        new TaskMaterializationSet("0.1", fingerprint,
                                List.of(new TaskMaterialization(
                                        "TASK-UNKNOWN", testNode())))));
        assertFailure(SimulationErrorCode.MATERIALIZATION_DUPLICATE_TASK,
                () -> engine.simulate(model, report,
                        new TaskMaterializationSet("0.1", fingerprint,
                                List.of(materialization, materialization))));
    }

    @Test
    void sharedEngineShouldProduceIdenticalResultsConcurrently()
            throws Exception {
        JsonNode model = fixture();
        ImpactReport report = impactReport(testImpact());
        TaskMaterializationSet set = set(model,
                new TaskMaterialization("TASK-TEST", testNode()));
        ModelSimulationEngine engine = new ModelSimulationEngine();
        SimulationResult expected = engine.simulate(model, report, set);
        List<Callable<SimulationResult>> calls = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            calls.add(() -> engine.simulate(model, report, set));
        }

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var future : executor.invokeAll(calls)) {
                SimulationResult actual = future.get();
                assertEquals(expected, actual);
                assertNotSame(expected.futureModel(), actual.futureModel());
            }
        }
    }

    private JsonNode fixture() throws IOException {
        try (var input = getClass().getResourceAsStream(
                "/simulation-base-model.json")) {
            assertNotNull(input);
            return mapper.readTree(input);
        }
    }

    private ObjectNode testNode() {
        ObjectNode node = mapper.createObjectNode()
                .put("id", "TEST-CANCEL-NEW")
                .put("type", "TEST_IMPLEMENTATION")
                .put("name", "Cancellation API test");
        ObjectNode details = node.putObject("testImplementation");
        details.put("code", "CANCEL_ORDER_API_NEW");
        details.put("executionType", "AUTOMATED");
        details.putArray("preconditions");
        details.putArray("steps").addObject()
                .put("order", 1)
                .put("action", "Call cancellation API")
                .put("expectedResult", "Order is cancelled");
        return node;
    }

    private ObjectNode checkNode() {
        ObjectNode node = mapper.createObjectNode()
                .put("id", "CHECK-CANCEL-STATUS")
                .put("type", "CHECK")
                .put("name", "Cancellation status check");
        node.putObject("check")
                .put("checkType", "API")
                .put("assertion", "Response status is CANCELLED");
        return node;
    }

    private ImpactReport impactReport(TaskImpact... impacts) {
        return new ImpactReport(
                true, "impact-0.1",
                new ImpactSummary(
                        impacts.length, impacts.length, 0, 1, 1, 1, 0, 0),
                List.of(impacts));
    }

    private TaskImpact testImpact() {
        return impact(
                "TASK-TEST", RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                FindingCode.SCENARIO_WITHOUT_TEST,
                "SC-CANCEL-PENDING", NodeType.SCENARIO,
                NodeType.TEST_IMPLEMENTATION, RelationshipType.VALIDATES,
                RelationEndpointRole.TARGET);
    }

    private TaskImpact checkImpact() {
        return impact(
                "TASK-CHECK", RemediationTaskType.CREATE_CHECK,
                FindingCode.TEST_WITHOUT_CHECK,
                "TEST-CANCEL-EXISTING", NodeType.TEST_IMPLEMENTATION,
                NodeType.CHECK, RelationshipType.HAS_CHECK,
                RelationEndpointRole.SOURCE);
    }

    private TaskImpact impact(
            String taskId,
            RemediationTaskType taskType,
            FindingCode findingCode,
            String targetId,
            NodeType targetType,
            NodeType futureType,
            RelationshipType relationshipType,
            RelationEndpointRole endpointRole
    ) {
        return new TaskImpact(
                taskId, taskType, targetId, findingCode,
                new StructuralGap(targetType, futureType,
                        relationshipType, endpointRole),
                new ExpectedStructuralChange(
                        ImpactChangeType.CREATE_RELATED_NODE,
                        futureType, relationshipType, targetId, endpointRole,
                        ResolutionExpectation
                                .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION),
                1, List.of());
    }

    private TaskMaterializationSet set(
            JsonNode model, TaskMaterialization... materializations
    ) {
        return new TaskMaterializationSet(
                "0.1", new QaModelFingerprintCalculator().calculate(model),
                List.of(materializations));
    }

    private List<String> relationshipIds(JsonNode model) {
        List<String> ids = new ArrayList<>();
        model.path("relationships").forEach(
                relationship -> ids.add(relationship.path("id").asText()));
        return ids;
    }

    private void assertFailure(SimulationErrorCode code, Runnable action) {
        SimulationException exception = assertThrows(
                SimulationException.class, action::run);
        assertEquals(code, exception.code());
    }

    private static final class CountingValidationEngine
            extends QaModelValidationEngine {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public QaModelValidationResult validate(JsonNode document) {
            invocations.incrementAndGet();
            return super.validate(document);
        }

        private int invocations() {
            return invocations.get();
        }
    }
}
