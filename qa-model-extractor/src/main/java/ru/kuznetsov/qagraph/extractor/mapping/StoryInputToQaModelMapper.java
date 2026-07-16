package ru.kuznetsov.qagraph.extractor.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.kuznetsov.qagraph.extractor.model.ExtractionIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StoryInputToQaModelMapper {

    private final ObjectMapper objectMapper;

    public StoryInputToQaModelMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MappingResult map(JsonNode input) {
        List<ExtractionIssue> issues = new ArrayList<>();
        ObjectNode output = objectMapper.createObjectNode();

        output.put("schemaVersion", "0.1");
        output.set("project", input.path("project").deepCopy());

        ArrayNode sources = output.putArray("sources");
        sources.add(mapSource(input.path("source")));

        ArrayNode nodes = output.putArray("nodes");
        ArrayNode relationships = output.putArray("relationships");

        Sequence operationIds = new Sequence("BO");
        Sequence ruleIds = new Sequence("BR");
        Sequence scenarioIds = new Sequence("SC");
        Sequence stepIds = new Sequence("STEP");
        Sequence relationshipIds = new Sequence("REL");

        String sourceId = text(input.path("source"), "id");
        String storyId = normalizeId(text(input.path("story"), "externalId"), "US");

        nodes.add(mapStory(input.path("story"), storyId, sourceId));

        Map<String, String> technicalIds = mapTechnicalImplementations(
                input.path("technicalImplementations"),
                sourceId,
                nodes
        );

        JsonNode operations = input.path("operations");

        for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
            JsonNode operation = operations.get(operationIndex);
            String operationId = operationIds.next();

            nodes.add(mapOperation(operation, operationId, sourceId));
            addRelationship(
                    relationships,
                    relationshipIds.next(),
                    storyId,
                    "DESCRIBES",
                    operationId
            );

            Map<String, String> ruleCodeToNodeId = new HashMap<>();
            JsonNode rules = operation.path("rules");

            for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
                JsonNode rule = rules.get(ruleIndex);
                String ruleId = ruleIds.next();
                String ruleCode = text(rule, "code");

                if (ruleCodeToNodeId.putIfAbsent(ruleCode, ruleId) != null) {
                    issues.add(ExtractionIssue.mappingWarning(
                            "DUPLICATE_RULE_CODE",
                            "В операции повторяется rule.code: " + ruleCode,
                            "/operations/" + operationIndex
                                    + "/rules/" + ruleIndex + "/code"
                    ));
                }

                nodes.add(mapRule(rule, ruleId, sourceId));
                addRelationship(
                        relationships,
                        relationshipIds.next(),
                        operationId,
                        "GOVERNED_BY",
                        ruleId
                );
            }

            JsonNode scenarios = operation.path("scenarios");

            for (int scenarioIndex = 0; scenarioIndex < scenarios.size(); scenarioIndex++) {
                JsonNode scenario = scenarios.get(scenarioIndex);
                String scenarioId = scenarioIds.next();

                nodes.add(mapScenario(
                        scenario,
                        scenarioId,
                        sourceId,
                        stepIds
                ));

                addRelationship(
                        relationships,
                        relationshipIds.next(),
                        operationId,
                        "SPECIFIED_BY",
                        scenarioId
                );

                JsonNode coveredRules = scenario.path("coversRuleCodes");

                for (int coverIndex = 0; coverIndex < coveredRules.size(); coverIndex++) {
                    String ruleCode = coveredRules.get(coverIndex).asText();
                    String ruleId = ruleCodeToNodeId.get(ruleCode);

                    if (ruleId == null) {
                        issues.add(ExtractionIssue.mappingWarning(
                                "UNKNOWN_RULE_CODE",
                                "Сценарий ссылается на отсутствующее правило: "
                                        + ruleCode,
                                "/operations/" + operationIndex
                                        + "/scenarios/" + scenarioIndex
                                        + "/coversRuleCodes/" + coverIndex
                        ));
                        continue;
                    }

                    addRelationship(
                            relationships,
                            relationshipIds.next(),
                            scenarioId,
                            "COVERS",
                            ruleId
                    );
                }
            }

            JsonNode implementationRefs =
                    operation.path("technicalImplementationRefs");

            for (int refIndex = 0; refIndex < implementationRefs.size(); refIndex++) {
                String inputTechnicalId = implementationRefs.get(refIndex).asText();
                String technicalNodeId = technicalIds.get(inputTechnicalId);

                if (technicalNodeId == null) {
                    issues.add(ExtractionIssue.mappingWarning(
                            "UNKNOWN_TECHNICAL_IMPLEMENTATION",
                            "Операция ссылается на отсутствующую technical implementation: "
                                    + inputTechnicalId,
                            "/operations/" + operationIndex
                                    + "/technicalImplementationRefs/" + refIndex
                    ));
                    continue;
                }

                addRelationship(
                        relationships,
                        relationshipIds.next(),
                        operationId,
                        "IMPLEMENTED_BY",
                        technicalNodeId
                );
            }
        }

        return new MappingResult(output, List.copyOf(issues));
    }

    private ObjectNode mapSource(JsonNode source) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", text(source, "id"));
        result.put("type", mapSourceType(text(source, "type")));
        result.put("name", text(source, "name"));
        putNullable(result, "version", source.get("version"));
        putNullable(result, "externalRef", source.get("externalRef"));
        putNullable(result, "uri", source.get("uri"));
        result.putNull("checksum");

        ObjectNode metadata = source.path("metadata").isObject()
                ? source.path("metadata").deepCopy()
                : objectMapper.createObjectNode();

        metadata.put("storyInputSourceType", text(source, "type"));
        result.set("metadata", metadata);

        return result;
    }

    private ObjectNode mapStory(JsonNode story, String id, String sourceId) {
        ObjectNode node = baseNode(
                id,
                "USER_STORY",
                text(story, "title"),
                nullableText(story, "description"),
                "CONFIRMED"
        );

        node.set(
                "sourceReferences",
                sourceReferences(
                        sourceId,
                        story.path("sourceRef"),
                        "DOCUMENTED",
                        1.0
                )
        );

        ObjectNode payload = node.putObject("story");
        payload.put("externalId", text(story, "externalId"));
        putNullableText(payload, "actor", story, "actor");
        putNullableText(payload, "goal", story, "goal");
        putNullableText(payload, "businessValue", story, "businessValue");

        return node;
    }

    private ObjectNode mapOperation(JsonNode operation, String id, String sourceId) {
        ObjectNode node = baseNode(
                id,
                "BUSINESS_OPERATION",
                text(operation, "name"),
                nullableText(operation, "description"),
                "CONFIRMED"
        );

        ObjectNode metadata = (ObjectNode) node.get("metadata");

        metadata.set(
                "preconditions",
                operation.path("preconditions").deepCopy()
        );

        node.set(
                "sourceReferences",
                sourceReferences(
                        sourceId,
                        operation.path("sourceRef"),
                        "DOCUMENTED",
                        1.0
                )
        );

        ObjectNode payload = node.putObject("operation");
        payload.put("code", text(operation, "code"));
        putNullableText(payload, "domain", operation.path("metadata"), "domain");
        putNullableText(
                payload,
                "businessOutcome",
                operation,
                "businessOutcome"
        );

        return node;
    }

    private ObjectNode mapRule(JsonNode rule, String id, String sourceId) {
        ObjectNode node = baseNode(
                id,
                "BUSINESS_RULE",
                text(rule, "name"),
                null,
                "CONFIRMED"
        );

        node.set(
                "sourceReferences",
                sourceReferences(
                        sourceId,
                        rule.path("sourceRef"),
                        "DOCUMENTED",
                        1.0
                )
        );

        ObjectNode payload = node.putObject("rule");
        payload.put("code", text(rule, "code"));
        payload.put("ruleType", text(rule, "ruleType"));
        payload.put("text", text(rule, "text"));
        putNullable(payload, "expression", rule.get("expression"));

        return node;
    }

    private ObjectNode mapScenario(
            JsonNode scenario,
            String id,
            String sourceId,
            Sequence stepIds
    ) {
        ObjectNode node = baseNode(
                id,
                "SCENARIO",
                text(scenario, "title"),
                null,
                "CONFIRMED"
        );

        node.set("tags", scenario.path("tags").deepCopy());
        node.set(
                "sourceReferences",
                sourceReferences(
                        sourceId,
                        scenario.path("sourceRef"),
                        "DOCUMENTED",
                        1.0
                )
        );

        ObjectNode payload = node.putObject("scenario");
        payload.put("code", text(scenario, "code"));
        payload.set("given", mapSteps(scenario.path("given"), stepIds));
        payload.set("when", mapSteps(scenario.path("when"), stepIds));
        payload.set("then", mapSteps(scenario.path("then"), stepIds));

        return node;
    }

    private Map<String, String> mapTechnicalImplementations(
            JsonNode input,
            String sourceId,
            ArrayNode nodes
    ) {
        Map<String, String> result = new HashMap<>();

        for (JsonNode technical : input) {
            String id = text(technical, "id");
            result.put(id, id);

            ObjectNode node = baseNode(
                    id,
                    "TECHNICAL_IMPLEMENTATION",
                    text(technical, "name"),
                    null,
                    text(technical, "status")
            );

            node.set(
                    "sourceReferences",
                    sourceReferences(
                            sourceId,
                            technical.path("sourceRef"),
                            text(technical, "evidenceType"),
                            "INFERRED".equals(text(technical, "evidenceType"))
                                    ? 0.8
                                    : 1.0
                    )
            );

            ObjectNode payload = node.putObject("technicalImplementation");
            payload.put(
                    "implementationType",
                    text(technical, "implementationType")
            );
            payload.put("system", text(technical, "system"));
            payload.set(
                    "details",
                    technical.path("details").isObject()
                            ? technical.path("details").deepCopy()
                            : objectMapper.createObjectNode()
            );

            nodes.add(node);
        }

        return result;
    }

    private ArrayNode mapSteps(JsonNode input, Sequence stepIds) {
        ArrayNode result = objectMapper.createArrayNode();

        for (JsonNode step : input) {
            ObjectNode mapped = result.addObject();
            mapped.put("id", stepIds.next());
            mapped.put("text", text(step, "text"));
            mapped.set(
                    "parameters",
                    step.path("parameters").isObject()
                            ? step.path("parameters").deepCopy()
                            : objectMapper.createObjectNode()
            );
        }

        return result;
    }

    private ObjectNode baseNode(
            String id,
            String type,
            String name,
            String description,
            String status
    ) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", id);
        node.put("type", type);
        node.put("name", name);

        if (description == null) {
            node.putNull("description");
        } else {
            node.put("description", description);
        }

        node.put("status", status);
        node.putArray("tags");
        node.putArray("sourceReferences");
        node.putObject("metadata");

        return node;
    }

    private ArrayNode sourceReferences(
            String sourceId,
            JsonNode sourceRef,
            String evidenceType,
            double confidence
    ) {
        ArrayNode result = objectMapper.createArrayNode();

        if (!sourceRef.isObject()) {
            return result;
        }

        ObjectNode reference = result.addObject();
        reference.put("sourceId", sourceId);

        JsonNode inputLocation = sourceRef.path("location");

        if (inputLocation.isObject()) {
            ObjectNode outputLocation = reference.putObject("location");

            outputLocation.put(
                    "type",
                    mapSourceLocationType(
                            text(inputLocation, "type")
                    )
            );

            outputLocation.put(
                    "value",
                    text(inputLocation, "value")
            );
        } else {
            reference.putNull("location");
        }

        putNullable(
                reference,
                "text",
                sourceRef.get("text")
        );

        reference.put("confidence", confidence);
        reference.put("evidenceType", evidenceType);

        return result;
    }

    private void addRelationship(
            ArrayNode relationships,
            String id,
            String from,
            String type,
            String to
    ) {
        ObjectNode relationship = relationships.addObject();
        relationship.put("id", id);
        relationship.put("from", from);
        relationship.put("type", type);
        relationship.put("to", to);
        relationship.putObject("properties");
        relationship.putArray("sourceReferences");
    }

    private String mapSourceType(String type) {
        return switch (type) {
            case "BRD", "SRS", "BDD_FEATURE",
                 "USER_STORY", "MANUAL_INPUT", "OTHER" -> type;
            case "USE_CASE" -> "USER_STORY";
            default -> "OTHER";
        };
    }

    private String mapSourceLocationType(String inputType) {
        if (inputType == null) {
            return "OTHER";
        }

        return switch (inputType) {
            case "USER_STORY" ->
                    "USER_STORY";

            case "ACCEPTANCE_CRITERION" ->
                    "ACCEPTANCE_CRITERION";

            case "PRECONDITION",
                 "MAIN_SCENARIO",
                 "ALTERNATIVE_SCENARIO",
                 "ERROR_SCENARIO",
                 "REQUIREMENT",
                 "NOTE",
                 "SECTION" ->
                    "SECTION";

            case "OTHER" ->
                    "OTHER";

            default ->
                    "OTHER";
        };
    }

    private String normalizeId(String source, String prefix) {
        if (source.matches("^[A-Za-z0-9][A-Za-z0-9._:-]*$")) {
            return source;
        }

        return prefix + "-" + source.replaceAll("[^A-Za-z0-9._:-]", "-");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String nullableText(JsonNode node, String field) {
        return text(node, field);
    }

    private void putNullableText(
            ObjectNode target,
            String targetField,
            JsonNode source,
            String sourceField
    ) {
        String value = text(source, sourceField);

        if (value == null) {
            target.putNull(targetField);
        } else {
            target.put(targetField, value);
        }
    }

    private void putNullable(ObjectNode target, String field, JsonNode value) {
        if (value == null || value.isNull()) {
            target.putNull(field);
        } else {
            target.set(field, value.deepCopy());
        }
    }

    public record MappingResult(
            ObjectNode qaModel,
            List<ExtractionIssue> issues
    ) {
    }

    private static final class Sequence {
        private final String prefix;
        private int value = 1;

        private Sequence(String prefix) {
            this.prefix = prefix;
        }

        private String next() {
            return "%s-%03d".formatted(prefix, value++);
        }
    }
}
