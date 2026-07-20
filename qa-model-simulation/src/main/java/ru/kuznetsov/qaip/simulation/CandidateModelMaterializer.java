package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;
import ru.kuznetsov.qaip.simulation.model.AppliedMaterialization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CandidateModelMaterializer {
    private final RelationshipIdentityPolicy identityPolicy;

    CandidateModelMaterializer() {
        this(new RelationshipIdentityPolicy());
    }

    CandidateModelMaterializer(RelationshipIdentityPolicy identityPolicy) {
        this.identityPolicy = identityPolicy;
    }

    CandidateMaterialization materialize(
            JsonNode currentModel,
            List<MatchedMaterialization> matchedMaterializations
    ) {
        ObjectNode candidate = candidateCopy(currentModel);
        ArrayNode nodes = requiredArray(candidate, "nodes");
        ArrayNode relationships = requiredArray(candidate, "relationships");
        RelationshipIndex relationshipIndex = index(relationships);
        List<AppliedMaterialization> appliedMaterializations =
                new ArrayList<>();

        for (MatchedMaterialization match : matchedMaterializations) {
            nodes.add(match.taskMaterialization().futureNode().deepCopy());
        }
        for (MatchedMaterialization match : matchedMaterializations) {
            ObjectNode relationship = relationshipFor(match);
            relationshipIndex.reserve(relationship, match.taskImpact().taskId());
            relationships.add(relationship);
            appliedMaterializations.add(new AppliedMaterialization(
                    match.taskImpact().taskId(),
                    match.taskMaterialization().futureNode()
                            .path("id").textValue(),
                    relationship.path("id").textValue()
            ));
        }
        return new CandidateMaterialization(
                candidate, appliedMaterializations);
    }

    private ObjectNode candidateCopy(JsonNode currentModel) {
        if (currentModel == null || !currentModel.isObject()) {
            throw failure("currentModel must be a JSON object");
        }
        return currentModel.deepCopy();
    }

    private ArrayNode requiredArray(ObjectNode candidate, String fieldName) {
        JsonNode value = candidate.get(fieldName);
        if (!(value instanceof ArrayNode array)) {
            throw failure("Candidate model requires a " + fieldName + " array");
        }
        return array;
    }

    private RelationshipIndex index(ArrayNode relationships) {
        Set<String> ids = new HashSet<>();
        Set<LogicalRelationship> logicalRelationships = new HashSet<>();
        for (JsonNode relationship : relationships) {
            if (!relationship.isObject()
                    || !relationship.path("id").isTextual()
                    || !relationship.path("from").isTextual()
                    || !relationship.path("type").isTextual()
                    || !relationship.path("to").isTextual()) {
                throw failure("Candidate model contains an unusable relationship");
            }
            ids.add(relationship.path("id").textValue());
            logicalRelationships.add(new LogicalRelationship(
                    relationship.path("type").textValue(),
                    relationship.path("from").textValue(),
                    relationship.path("to").textValue()));
        }
        return new RelationshipIndex(ids, logicalRelationships);
    }

    private ObjectNode relationshipFor(MatchedMaterialization match) {
        var impact = match.taskImpact();
        var expected = impact.expectedChange();
        if (expected.changeType() != ImpactChangeType.CREATE_RELATED_NODE) {
            throw new SimulationException(
                    SimulationErrorCode.UNSUPPORTED_IMPACT_CHANGE,
                    "Unsupported Impact change for task " + impact.taskId(),
                    impact.taskId(), null, null);
        }

        String newNodeId = match.taskMaterialization()
                .futureNode().path("id").textValue();
        String existingNodeId = expected.existingNodeId();
        String fromNodeId;
        String toNodeId;
        if (expected.existingNodeRelationRole()
                == RelationEndpointRole.SOURCE) {
            fromNodeId = existingNodeId;
            toNodeId = newNodeId;
        } else if (expected.existingNodeRelationRole()
                == RelationEndpointRole.TARGET) {
            fromNodeId = newNodeId;
            toNodeId = existingNodeId;
        } else {
            throw new SimulationException(
                    SimulationErrorCode.UNSUPPORTED_IMPACT_CHANGE,
                    "Unsupported relationship endpoint role for task "
                            + impact.taskId(), impact.taskId(), null, null);
        }

        RelationshipType type = expected.relationTypeToCreate();
        ObjectNode relationship = com.fasterxml.jackson.databind.node
                .JsonNodeFactory.instance.objectNode();
        relationship.put("id", identityPolicy.idFor(
                type, fromNodeId, toNodeId));
        relationship.put("from", fromNodeId);
        relationship.put("type", type.name());
        relationship.put("to", toNodeId);
        return relationship;
    }

    private SimulationException failure(String message) {
        return new SimulationException(
                SimulationErrorCode.INVALID_CANDIDATE_MODEL_SHAPE, message);
    }

    private record LogicalRelationship(String type, String from, String to) {
    }

    private static final class RelationshipIndex {
        private final Set<String> ids;
        private final Set<LogicalRelationship> logicalRelationships;

        private RelationshipIndex(
                Set<String> ids,
                Set<LogicalRelationship> logicalRelationships
        ) {
            this.ids = ids;
            this.logicalRelationships = logicalRelationships;
        }

        private void reserve(ObjectNode relationship, String taskId) {
            String id = relationship.path("id").textValue();
            var logical = new LogicalRelationship(
                    relationship.path("type").textValue(),
                    relationship.path("from").textValue(),
                    relationship.path("to").textValue());
            if (!logicalRelationships.add(logical)) {
                throw new SimulationException(
                        SimulationErrorCode.DUPLICATE_LOGICAL_RELATIONSHIP,
                        "Logical relationship already exists for task " + taskId,
                        taskId, null, null);
            }
            if (!ids.add(id)) {
                throw new SimulationException(
                        SimulationErrorCode.RELATIONSHIP_ID_COLLISION,
                        "Relationship ID collision for task " + taskId,
                        taskId, id, null);
            }
        }
    }
}
