package ru.kuznetsov.qaip.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import ru.kuznetsov.qaip.findings.model.FindingCode;
import ru.kuznetsov.qaip.impact.model.ExpectedStructuralChange;
import ru.kuznetsov.qaip.impact.model.ImpactChangeType;
import ru.kuznetsov.qaip.impact.model.RelationEndpointRole;
import ru.kuznetsov.qaip.impact.model.ResolutionExpectation;
import ru.kuznetsov.qaip.impact.model.StructuralGap;
import ru.kuznetsov.qaip.impact.model.TaskImpact;
import ru.kuznetsov.qaip.roadmap.model.RemediationTaskType;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;
import ru.kuznetsov.qaip.simulation.model.TaskMaterialization;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CandidateModelMaterializerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CandidateModelMaterializer materializer =
            new CandidateModelMaterializer();

    @Test
    void shouldAppendCompleteNodesAndRelationshipsInMatchOrder() {
        ObjectNode current = model(
                node("BR-1", "BUSINESS_RULE", "Existing rule"),
                node("BR-2", "BUSINESS_RULE", "Second rule"));
        current.withArray("relationships").add(relationship(
                "REL-EXISTING", "BR-1", "RELATED_TO", "BR-2"));
        ObjectNode secondPayload = node(
                "SC-2", "SCENARIO", "Second scenario");
        secondPayload.putObject("scenario")
                .put("code", "SC-2")
                .putArray("given").add("given");
        ObjectNode firstPayload = node(
                "SC-1", "SCENARIO", "First scenario");
        List<MatchedMaterialization> matches = List.of(
                match(scenarioImpact("TASK-2", "BR-2"), secondPayload),
                match(scenarioImpact("TASK-1", "BR-1"), firstPayload));

        JsonNode candidate = materializer.materialize(current, matches)
                .candidateModel();

        assertEquals(List.of("BR-1", "BR-2", "SC-2", "SC-1"),
                ids(candidate.path("nodes")));
        assertEquals(secondPayload, candidate.path("nodes").get(2));
        assertEquals(List.of("REL-EXISTING",
                        new RelationshipIdentityPolicy().idFor(
                                RelationshipType.COVERS, "SC-2", "BR-2"),
                        new RelationshipIdentityPolicy().idFor(
                                RelationshipType.COVERS, "SC-1", "BR-1")),
                ids(candidate.path("relationships")));
        assertEquals(relationship(
                        candidate.path("relationships").get(1).path("id").asText(),
                        "SC-2", "COVERS", "BR-2"),
                candidate.path("relationships").get(1));
    }

    @Test
    void shouldMaterializeEverySupportedRelationshipDirection() {
        ObjectNode current = model(
                node("BR-1", "BUSINESS_RULE", "Rule"),
                node("SC-OLD", "SCENARIO", "Scenario"),
                node("TEST-OLD", "TEST_IMPLEMENTATION", "Test"));
        List<MatchedMaterialization> matches = List.of(
                match(scenarioImpact("TASK-SC", "BR-1"),
                        node("SC-NEW", "SCENARIO", "New scenario")),
                match(testImpact("TASK-TEST", "SC-OLD"),
                        node("TEST-NEW", "TEST_IMPLEMENTATION", "New test")),
                match(checkImpact("TASK-CHECK", "TEST-OLD"),
                        node("CHECK-NEW", "CHECK", "New check")));

        JsonNode relationships = materializer.materialize(current, matches)
                .candidateModel().path("relationships");

        assertRelationship(relationships.get(0),
                "SC-NEW", "COVERS", "BR-1");
        assertRelationship(relationships.get(1),
                "TEST-NEW", "VALIDATES", "SC-OLD");
        assertRelationship(relationships.get(2),
                "TEST-OLD", "HAS_CHECK", "CHECK-NEW");
    }

    @Test
    void shouldBeDeterministicAndReturnIndependentTrees() {
        ObjectNode current = model(node("BR-1", "BUSINESS_RULE", "Rule"));
        List<MatchedMaterialization> matches = List.of(match(
                scenarioImpact("TASK-1", "BR-1"),
                node("SC-1", "SCENARIO", "Scenario")));

        JsonNode first = materializer.materialize(current, matches)
                .candidateModel();
        JsonNode second = materializer.materialize(current, matches)
                .candidateModel();

        assertEquals(first, second);
        assertNotSame(first, second);
        ((ObjectNode) first).put("changed", true);
        assertEquals(false, second.has("changed"));
    }

    @Test
    void shouldIsolateCandidateFromEveryMutableInputAndAccessor() {
        ObjectNode current = model(node("BR-1", "BUSINESS_RULE", "Rule"));
        JsonNode currentBefore = current.deepCopy();
        ObjectNode sourceFuture = node("SC-1", "SCENARIO", "Scenario");
        TaskMaterialization materialization =
                new TaskMaterialization("TASK-1", sourceFuture);
        var match = new MatchedMaterialization(
                scenarioImpact("TASK-1", "BR-1"), materialization);

        JsonNode candidate = this.materializer.materialize(
                current, List.of(match)).candidateModel();
        sourceFuture.put("name", "Changed source");
        ObjectNode exposed = (ObjectNode) materialization.futureNode();
        exposed.put("name", "Changed accessor");
        current.withArray("nodes").get(0).withObject("/metadata")
                .put("changed", true);

        assertEquals("Scenario",
                candidate.path("nodes").get(1).path("name").asText());
        assertEquals(currentBefore.path("nodes").get(0),
                candidate.path("nodes").get(0));
        ((ObjectNode) candidate.path("nodes").get(0)).put("name", "Candidate");
        assertEquals("Rule", current.path("nodes").get(0)
                .path("name").asText());
    }

    @Test
    void shouldRejectInvalidInternalModelShape() {
        ObjectNode missingNodes = mapper.createObjectNode();
        missingNodes.putArray("relationships");
        assertFailure(SimulationErrorCode.INVALID_CANDIDATE_MODEL_SHAPE,
                () -> materializer.materialize(missingNodes, List.of()));

        ObjectNode invalidRelationship = model(
                node("BR-1", "BUSINESS_RULE", "Rule"));
        invalidRelationship.withArray("relationships").addObject()
                .put("id", "REL-1");
        assertFailure(SimulationErrorCode.INVALID_CANDIDATE_MODEL_SHAPE,
                () -> materializer.materialize(
                        invalidRelationship, List.of()));
    }

    @Test
    void shouldRejectExistingRelationshipIdCollisionWithoutInputMutation() {
        ObjectNode current = model(node("BR-1", "BUSINESS_RULE", "Rule"));
        String generatedId = new RelationshipIdentityPolicy().idFor(
                RelationshipType.COVERS, "SC-1", "BR-1");
        current.withArray("relationships").add(relationship(
                generatedId, "BR-1", "RELATED_TO", "BR-1"));
        JsonNode before = current.deepCopy();

        assertFailure(SimulationErrorCode.RELATIONSHIP_ID_COLLISION,
                () -> materializer.materialize(current, List.of(match(
                        scenarioImpact("TASK-1", "BR-1"),
                        node("SC-1", "SCENARIO", "Scenario")))));
        assertEquals(before, current);
    }

    @Test
    void shouldRejectGeneratedRelationshipIdCollision() {
        RelationshipIdentityPolicy collidingPolicy =
                new RelationshipIdentityPolicy() {
                    @Override
                    String idFor(
                            RelationshipType type, String from, String to
                    ) {
                        return "SIMREL-v1-collision";
                    }
                };
        var collidingMaterializer =
                new CandidateModelMaterializer(collidingPolicy);
        ObjectNode current = model(
                node("BR-1", "BUSINESS_RULE", "Rule 1"),
                node("BR-2", "BUSINESS_RULE", "Rule 2"));

        assertFailure(SimulationErrorCode.RELATIONSHIP_ID_COLLISION,
                () -> collidingMaterializer.materialize(current, List.of(
                        match(scenarioImpact("TASK-1", "BR-1"),
                                node("SC-1", "SCENARIO", "Scenario 1")),
                        match(scenarioImpact("TASK-2", "BR-2"),
                                node("SC-2", "SCENARIO", "Scenario 2")))));
    }

    @Test
    void shouldRejectDuplicateLogicalRelationshipBeforeIdCollision() {
        ObjectNode current = model(node("BR-1", "BUSINESS_RULE", "Rule"));
        var impact1 = scenarioImpact("TASK-1", "BR-1");
        var impact2 = scenarioImpact("TASK-2", "BR-1");

        assertFailure(SimulationErrorCode.DUPLICATE_LOGICAL_RELATIONSHIP,
                () -> materializer.materialize(current, List.of(
                        match(impact1, node("SC-1", "SCENARIO", "First")),
                        match(impact2, node("SC-1", "SCENARIO", "Second")))));
    }

    @Test
    void relationshipIdsShouldBeStableValidQaModelIdentifiers() {
        var policy = new RelationshipIdentityPolicy();
        String first = policy.idFor(
                RelationshipType.VALIDATES, "TEST-1", "SC-1");

        assertEquals(first, policy.idFor(
                RelationshipType.VALIDATES, "TEST-1", "SC-1"));
        assertEquals(true, QaModelIdentifierValidator.isValid(first));
        assertEquals(false, first.equals(policy.idFor(
                RelationshipType.VALIDATES, "TEST-2", "SC-1")));
    }

    private TaskImpact scenarioImpact(String taskId, String targetId) {
        return impact(taskId, RemediationTaskType.CREATE_SCENARIO,
                FindingCode.BUSINESS_RULE_WITHOUT_SCENARIO, targetId,
                NodeType.BUSINESS_RULE, NodeType.SCENARIO,
                RelationshipType.COVERS, RelationEndpointRole.TARGET);
    }

    private TaskImpact testImpact(String taskId, String targetId) {
        return impact(taskId, RemediationTaskType.CREATE_TEST_IMPLEMENTATION,
                FindingCode.SCENARIO_WITHOUT_TEST, targetId,
                NodeType.SCENARIO, NodeType.TEST_IMPLEMENTATION,
                RelationshipType.VALIDATES, RelationEndpointRole.TARGET);
    }

    private TaskImpact checkImpact(String taskId, String targetId) {
        return impact(taskId, RemediationTaskType.CREATE_CHECK,
                FindingCode.TEST_WITHOUT_CHECK, targetId,
                NodeType.TEST_IMPLEMENTATION, NodeType.CHECK,
                RelationshipType.HAS_CHECK, RelationEndpointRole.SOURCE);
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
                new StructuralGap(targetType, futureType, relationshipType,
                        endpointRole),
                new ExpectedStructuralChange(
                        ImpactChangeType.CREATE_RELATED_NODE, futureType,
                        relationshipType, targetId, endpointRole,
                        ResolutionExpectation
                                .FINDING_EXPECTED_TO_BE_RESOLVED_AFTER_VALID_COMPLETION),
                1, List.of());
    }

    private MatchedMaterialization match(
            TaskImpact impact, ObjectNode futureNode
    ) {
        return new MatchedMaterialization(impact,
                new TaskMaterialization(impact.taskId(), futureNode));
    }

    private ObjectNode model(ObjectNode... nodes) {
        ObjectNode model = mapper.createObjectNode();
        model.put("schemaVersion", "0.1");
        for (ObjectNode node : nodes) {
            model.withArray("nodes").add(node);
        }
        model.putArray("relationships");
        return model;
    }

    private ObjectNode node(String id, String type, String name) {
        return mapper.createObjectNode()
                .put("id", id).put("type", type).put("name", name);
    }

    private ObjectNode relationship(
            String id, String from, String type, String to
    ) {
        return mapper.createObjectNode()
                .put("id", id).put("from", from)
                .put("type", type).put("to", to);
    }

    private List<String> ids(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(node -> node.path("id").asText())
                .toList();
    }

    private void assertRelationship(
            JsonNode relationship, String from, String type, String to
    ) {
        List<String> fieldNames = new java.util.ArrayList<>();
        relationship.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(List.of("id", "from", "type", "to"), fieldNames);
        assertEquals(from, relationship.path("from").asText());
        assertEquals(type, relationship.path("type").asText());
        assertEquals(to, relationship.path("to").asText());
    }

    private void assertFailure(SimulationErrorCode code, Runnable action) {
        SimulationException exception = assertThrows(
                SimulationException.class, action::run);
        assertEquals(code, exception.code());
    }
}
