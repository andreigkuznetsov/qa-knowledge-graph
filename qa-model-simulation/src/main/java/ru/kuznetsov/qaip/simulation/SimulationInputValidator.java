package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.ImpactReport;
import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;
import ru.kuznetsov.qaip.simulation.model.TaskMaterialization;
import ru.kuznetsov.qaip.simulation.model.TaskMaterializationSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SimulationInputValidator {
    private final QaModelFingerprintCalculator fingerprintCalculator =
            new QaModelFingerprintCalculator();

    List<MatchedMaterialization> validateAndMatch(
            JsonNode currentModel,
            ImpactReport impactReport,
            TaskMaterializationSet materializationSet
    ) {
        requireInput(currentModel, "currentModel");
        requireInput(impactReport, "impactReport");
        requireInput(materializationSet, "materializationSet");

        if (!TaskMaterializationSet.SUPPORTED_CONTRACT_VERSION.equals(
                materializationSet.materializationContractVersion())) {
            throw failure(
                    SimulationErrorCode.UNSUPPORTED_MATERIALIZATION_VERSION,
                    "Unsupported materialization contract version: "
                            + materializationSet.materializationContractVersion()
            );
        }

        ModelIndex modelIndex = inspectModel(currentModel);
        String actualFingerprint = fingerprintCalculator.calculate(currentModel);
        if (!actualFingerprint.equals(
                materializationSet.baseModelFingerprint())) {
            throw failure(
                    SimulationErrorCode.BASE_MODEL_FINGERPRINT_MISMATCH,
                    "Materialization base-model fingerprint does not match the current model"
            );
        }

        return match(impactReport, materializationSet, modelIndex);
    }

    private List<MatchedMaterialization> match(
            ImpactReport impactReport,
            TaskMaterializationSet materializationSet,
            ModelIndex modelIndex
    ) {
        Map<String, TaskImpact> impactsByTask = new LinkedHashMap<>();
        for (TaskImpact impact : impactReport.taskImpacts()) {
            if (impact == null || impact.taskId() == null) {
                throw failure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                        "Impact report contains an unusable task impact");
            }
            if (impactsByTask.putIfAbsent(impact.taskId(), impact) != null) {
                throw failure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                        "Impact report contains duplicate task ID "
                                + impact.taskId(), impact.taskId(), null);
            }
        }

        Map<String, TaskMaterialization> byTask = new HashMap<>();
        Set<String> futureNodeIds = new HashSet<>();
        for (TaskMaterialization materialization
                : materializationSet.materializations()) {
            String taskId = materialization.taskId();
            if (byTask.putIfAbsent(taskId, materialization) != null) {
                throw failure(
                        SimulationErrorCode.MATERIALIZATION_DUPLICATE_TASK,
                        "Duplicate Materialization for task " + taskId,
                        taskId, null);
            }
            TaskImpact impact = impactsByTask.get(taskId);
            if (impact == null) {
                throw failure(
                        SimulationErrorCode.MATERIALIZATION_UNKNOWN_TASK,
                        "Materialization references unknown Impact task "
                                + taskId, taskId, null);
            }
            validateImpactShape(impact);
            validateMaterialization(
                    materialization, impact, modelIndex, futureNodeIds);
        }

        List<MatchedMaterialization> matches = new ArrayList<>();
        for (TaskImpact impact : impactReport.taskImpacts()) {
            TaskMaterialization materialization = byTask.get(impact.taskId());
            if (materialization == null) {
                throw failure(
                        SimulationErrorCode.MATERIALIZATION_MISSING,
                        "Materialization is missing for Impact task "
                                + impact.taskId(), impact.taskId(), null);
            }
            matches.add(new MatchedMaterialization(impact, materialization));
        }
        return List.copyOf(matches);
    }

    private void validateImpactShape(TaskImpact impact) {
        var expected = impact.expectedChange();
        var gap = impact.structuralGap();
        boolean supported = expected.changeType()
                        == ImpactChangeType.CREATE_RELATED_NODE
                && expected.nodeTypeToCreate() == gap.requiredNodeType()
                && expected.relationTypeToCreate()
                        == gap.requiredRelationshipType()
                && expected.existingNodeId().equals(impact.targetNodeId())
                && expected.existingNodeRelationRole()
                        == gap.affectedNodeRelationRole();
        if (!supported) {
            throw failure(
                    SimulationErrorCode.UNSUPPORTED_IMPACT_CHANGE,
                    "Unsupported or inconsistent Impact change for task "
                            + impact.taskId(), impact.taskId(), null);
        }
    }

    private void validateMaterialization(
            TaskMaterialization materialization,
            TaskImpact impact,
            ModelIndex modelIndex,
            Set<String> futureNodeIds
    ) {
        JsonNode node = materialization.futureNode();
        String taskId = materialization.taskId();
        if (!node.isObject()) {
            throw failure(SimulationErrorCode.MATERIALIZATION_PAYLOAD_INVALID,
                    "Future node must be a JSON object", taskId, null);
        }

        JsonNode idNode = node.get("id");
        String nodeId = idNode != null && idNode.isTextual()
                ? idNode.textValue() : null;
        if (!QaModelIdentifierValidator.isValid(nodeId)) {
            throw failure(
                    SimulationErrorCode.MATERIALIZATION_INVALID_NODE_ID,
                    "Future node has a missing or invalid id for task " + taskId,
                    taskId, nodeId);
        }
        if (modelIndex.nodeTypesById().containsKey(nodeId)
                || !futureNodeIds.add(nodeId)) {
            throw failure(
                    SimulationErrorCode.MATERIALIZATION_NODE_ID_COLLISION,
                    "Future node ID collides with another node: " + nodeId,
                    taskId, nodeId);
        }

        JsonNode typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual()
                || !typeNode.textValue().equals(
                        impact.expectedChange().nodeTypeToCreate().name())) {
            throw failure(
                    SimulationErrorCode.MATERIALIZATION_NODE_TYPE_MISMATCH,
                    "Future node type does not match Impact expectation for task "
                            + taskId, taskId, nodeId);
        }

        String targetId = impact.expectedChange().existingNodeId();
        String actualTargetType = modelIndex.nodeTypesById().get(targetId);
        if (actualTargetType == null) {
            throw failure(SimulationErrorCode.TARGET_NODE_MISSING,
                    "Affected existing node is missing: " + targetId,
                    taskId, targetId);
        }
        String expectedTargetType = impact.structuralGap()
                .affectedNodeType().name();
        if (!actualTargetType.equals(expectedTargetType)) {
            throw failure(SimulationErrorCode.TARGET_NODE_TYPE_MISMATCH,
                    "Affected existing node type does not match Impact expectation: "
                            + targetId, taskId, targetId);
        }
    }

    private ModelIndex inspectModel(JsonNode model) {
        if (!model.isObject()
                || !model.path("nodes").isArray()
                || !model.path("relationships").isArray()) {
            throw failure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                    "currentModel must be an object with nodes and relationships arrays");
        }
        Map<String, String> nodeTypes = new HashMap<>();
        for (JsonNode node : model.path("nodes")) {
            if (!node.isObject()
                    || !node.path("id").isTextual()
                    || !node.path("type").isTextual()) {
                throw failure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                        "currentModel contains a node without textual id and type");
            }
            nodeTypes.put(node.path("id").textValue(),
                    node.path("type").textValue());
        }
        return new ModelIndex(Map.copyOf(nodeTypes));
    }

    private void requireInput(Object value, String name) {
        if (value == null) {
            throw failure(SimulationErrorCode.INVALID_SIMULATION_INPUT,
                    name + " must not be null");
        }
    }

    private SimulationException failure(
            SimulationErrorCode code, String message) {
        return new SimulationException(code, message);
    }

    private SimulationException failure(
            SimulationErrorCode code,
            String message,
            String taskId,
            String nodeId
    ) {
        return new SimulationException(code, message, taskId, nodeId, null);
    }

    private record ModelIndex(Map<String, String> nodeTypesById) { }
}
