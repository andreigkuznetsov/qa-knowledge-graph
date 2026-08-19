package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Frozen real-NetworkNT compatibility corpus for all V1 schema rules. */
class ScenarioSchemaDiagnosticNetworkntConformanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREFIX = ScenarioSchemaDiagnostic.RULE_PREFIX;
    private final ScenarioSchemaDiagnosticAdapterV1 adapter = new ScenarioSchemaDiagnosticAdapterV1();

    @Test
    void corpusRemainsBoundToExactFrozenSchemaContent() {
        assertEquals(
                "scenario-authority-schema-diagnostic-mapping-v1|sha256:"
                        + "7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1",
                ScenarioSchemaDiagnosticAdapterV1.V1_SCHEMA_MAPPING_BINDING);
    }

    @TestFactory
    Stream<DynamicTest> mapsCompleteExpectedRecordForEveryRealNetworkntV1Rule() {
        List<NetworkCase> cases = cases();
        assertEquals(36, cases.size());
        assertEquals(36, cases.stream().map(value -> value.expected().schemaRuleIdentifier())
                .distinct().count());
        assertEquals(36, ScenarioSchemaDiagnosticAdapterV1.ruleCount());

        return cases.stream().map(value -> DynamicTest.dynamicTest(
                value.expected().schemaRuleIdentifier(), () -> {
                    List<ScenarioSchemaDiagnostic> actual = adapter.validate(value.document());
                    List<ScenarioSchemaDiagnostic> matchingRule = actual.stream()
                            .filter(diagnostic -> diagnostic.schemaRuleIdentifier().equals(
                                    value.expected().schemaRuleIdentifier()))
                            .toList();

                    assertEquals(List.of(value.expected()), matchingRule, actual::toString);
                    ScenarioSchemaDiagnostic diagnostic = matchingRule.getFirst();
                    assertEquals("scenario-authority-schema-diagnostic-v1",
                            diagnostic.diagnosticContractVersion());
                    assertEquals(ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION,
                            diagnostic.stableCode());
                    assertEquals(value.expected().instanceLocation(), diagnostic.instanceLocation());
                    assertEquals(value.expected().normativeSchemaKeyword(),
                            diagnostic.normativeSchemaKeyword());
                    assertEquals(value.expected().typedParameters(), diagnostic.typedParameters());
                }));
    }

    private static List<NetworkCase> cases() {
        return List.of(
                direct(BooleanNode.TRUE, expected("", "type", "/type", text("expectedType", "object"))),
                mutate(root -> root.put("extra", true),
                        expected("", "additionalProperties", "/additionalProperties",
                                text("unexpectedProperty", "extra"))),
                mutate(root -> root.remove("authority"),
                        expected("", "required", "/required", text("missingProperty", "authority"))),
                mutate(root -> root.put("format", "wrong"),
                        expected("/format", "const", "/properties/format/const",
                                text("expectedText", "qaip-scenario-authority-manifest-v1"))),
                mutate(root -> root.put("schemaVersion", "wrong"),
                        expected("/schemaVersion", "const", "/properties/schemaVersion/const",
                                text("expectedText", "1.0"))),
                mutate(root -> root.put("scenarioIdentityScheme", "wrong"),
                        expected("/scenarioIdentityScheme", "const",
                                "/properties/scenarioIdentityScheme/const",
                                text("expectedText", "qaip-scenario-identity-v1"))),
                mutate(root -> root.put("scenarios", true),
                        expected("/scenarios", "type", "/properties/scenarios/type",
                                text("expectedType", "array"))),

                mutate(root -> root.put("authority", true), authority("type", text("expectedType", "string"))),
                mutate(root -> root.put("authority", ""), authority("minLength", unsigned("minimumCodePointLength", 1))),
                mutate(root -> root.put("authority", "x".repeat(201)), authority("maxLength", unsigned("maximumCodePointLength", 200))),
                mutate(root -> root.put("authority", "bad value"), authority("pattern", text("requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:/-]*$"))),

                mutate(root -> scenario(root).put("scenarioKey", true), stable("type", text("expectedType", "string"))),
                mutate(root -> scenario(root).put("scenarioKey", ""), stable("minLength", unsigned("minimumCodePointLength", 1))),
                mutate(root -> scenario(root).put("scenarioKey", "x".repeat(161)), stable("maxLength", unsigned("maximumCodePointLength", 160))),
                mutate(root -> scenario(root).put("scenarioKey", "bad/value"), stable("pattern", text("requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$"))),

                mutate(root -> ruleRef(root).put("identityScheme", true), identity("type", text("expectedType", "string"))),
                mutate(root -> ruleRef(root).put("identityScheme", ""), identity("minLength", unsigned("minimumCodePointLength", 1))),
                mutate(root -> ruleRef(root).put("identityScheme", "x".repeat(161)), identity("maxLength", unsigned("maximumCodePointLength", 160))),
                mutate(root -> ruleRef(root).put("identityScheme", "bad/value"), identity("pattern", text("requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$"))),

                mutate(root -> scenario(root).put("title", true), nonBlank("/scenarios/0/title", "type", text("expectedType", "string"))),
                mutate(root -> scenario(root).put("title", ""), nonBlank("/scenarios/0/title", "minLength", unsigned("minimumCodePointLength", 1))),
                mutate(root -> scenario(root).put("title", "   "), nonBlank("/scenarios/0/title", "pattern", text("requiredPattern", ".*\\S.*"))),

                mutate(root -> scenario(root).put("given", true), steps("type", text("expectedType", "array"))),
                mutate(root -> scenario(root).set("given", JSON.createArrayNode()), steps("minItems", unsigned("minimumItemCount", 1))),

                mutate(root -> ((ArrayNode) root.path("scenarios")).set(0, BooleanNode.TRUE),
                        expected("/scenarios/0", "type", "/$defs/scenario/type", text("expectedType", "object"))),
                mutate(root -> scenario(root).put("extra", true),
                        expected("/scenarios/0", "additionalProperties", "/$defs/scenario/additionalProperties",
                                text("unexpectedProperty", "extra"))),
                mutate(root -> scenario(root).remove("title"),
                        expected("/scenarios/0", "required", "/$defs/scenario/required",
                                text("missingProperty", "title"))),
                mutate(root -> scenario(root).put("ruleRefs", true),
                        expected("/scenarios/0/ruleRefs", "type",
                                "/$defs/scenario/properties/ruleRefs/type", text("expectedType", "array"))),
                mutate(root -> ((ArrayNode) scenario(root).path("ruleRefs")).add(ruleRef(root).deepCopy()),
                        expected("/scenarios/0/ruleRefs", "uniqueItems",
                                "/$defs/scenario/properties/ruleRefs/uniqueItems",
                                List.of(unsigned("duplicateIndex", 1), unsigned("firstIndex", 0)))),

                mutate(root -> scenario(root).put("operationRef", true),
                        expected("/scenarios/0/operationRef", "type",
                                "/$defs/httpOperationReference/type", text("expectedType", "object"))),
                mutate(root -> operation(root).put("extra", true),
                        expected("/scenarios/0/operationRef", "additionalProperties",
                                "/$defs/httpOperationReference/additionalProperties",
                                text("unexpectedProperty", "extra"))),
                mutate(root -> operation(root).remove("method"),
                        expected("/scenarios/0/operationRef", "required",
                                "/$defs/httpOperationReference/required", text("missingProperty", "method"))),
                mutate(root -> operation(root).put("identityScheme", "wrong"),
                        expected("/scenarios/0/operationRef/identityScheme", "const",
                                "/$defs/httpOperationReference/properties/identityScheme/const",
                                text("expectedText", "qaip-http-operation-reference-v1"))),

                mutate(root -> ((ArrayNode) scenario(root).path("ruleRefs")).set(0, BooleanNode.TRUE),
                        expected("/scenarios/0/ruleRefs/0", "type",
                                "/$defs/businessRuleReference/type", text("expectedType", "object"))),
                mutate(root -> ruleRef(root).put("extra", true),
                        expected("/scenarios/0/ruleRefs/0", "additionalProperties",
                                "/$defs/businessRuleReference/additionalProperties",
                                text("unexpectedProperty", "extra"))),
                mutate(root -> ruleRef(root).remove("authority"),
                        expected("/scenarios/0/ruleRefs/0", "required",
                                "/$defs/businessRuleReference/required", text("missingProperty", "authority")))
        );
    }

    private static ScenarioSchemaDiagnostic authority(
            String keyword, ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected("/authority", keyword, "/$defs/authority/" + keyword, parameter);
    }

    private static ScenarioSchemaDiagnostic stable(
            String keyword, ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected("/scenarios/0/scenarioKey", keyword, "/$defs/stableKey/" + keyword, parameter);
    }

    private static ScenarioSchemaDiagnostic identity(
            String keyword, ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected("/scenarios/0/ruleRefs/0/identityScheme", keyword,
                "/$defs/identityScheme/" + keyword, parameter);
    }

    private static ScenarioSchemaDiagnostic nonBlank(
            String location, String keyword, ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected(location, keyword, "/$defs/nonBlankString/" + keyword, parameter);
    }

    private static ScenarioSchemaDiagnostic steps(
            String keyword, ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected("/scenarios/0/given", keyword, "/$defs/steps/" + keyword, parameter);
    }

    private static ScenarioSchemaDiagnostic expected(
            String location, String keyword, String pointer,
            ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter) {
        return expected(location, keyword, pointer, List.of(parameter));
    }

    private static ScenarioSchemaDiagnostic expected(
            String location, String keyword, String pointer,
            List<ScenarioSchemaDiagnostic.CanonicalTypedParameter> parameters) {
        return ScenarioSchemaDiagnostic.v1(location, keyword, PREFIX + pointer, parameters);
    }

    private static ScenarioSchemaDiagnostic.TextParameter text(String name, String value) {
        return new ScenarioSchemaDiagnostic.TextParameter(name, value);
    }

    private static ScenarioSchemaDiagnostic.Unsigned64Parameter unsigned(String name, long value) {
        return new ScenarioSchemaDiagnostic.Unsigned64Parameter(name, value);
    }

    private static NetworkCase direct(JsonNode document, ScenarioSchemaDiagnostic expected) {
        return new NetworkCase(document.deepCopy(), expected);
    }

    private static NetworkCase mutate(
            Consumer<ObjectNode> mutation,
            ScenarioSchemaDiagnostic expected
    ) {
        ObjectNode document = validManifest();
        mutation.accept(document);
        return new NetworkCase(document, expected);
    }

    private static ObjectNode scenario(ObjectNode root) {
        return (ObjectNode) root.path("scenarios").get(0);
    }

    private static ObjectNode operation(ObjectNode root) {
        return (ObjectNode) scenario(root).path("operationRef");
    }

    private static ObjectNode ruleRef(ObjectNode root) {
        return (ObjectNode) scenario(root).path("ruleRefs").get(0);
    }

    private static ObjectNode validManifest() {
        try {
            return (ObjectNode) JSON.readTree("""
                    {"format":"qaip-scenario-authority-manifest-v1","schemaVersion":"1.0",
                     "authority":"orders","scenarioIdentityScheme":"qaip-scenario-identity-v1",
                     "scenarios":[{"scenarioKey":"CREATE","title":"Title","given":["A"],
                     "when":["B"],"then":["C"],"operationRef":{
                     "identityScheme":"qaip-http-operation-reference-v1","method":"POST","path":"/orders"},
                     "ruleRefs":[{"authority":"rules","stableRuleKey":"R1",
                     "identityScheme":"qaip-rule-identity-v1"}]}]}
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record NetworkCase(JsonNode document, ScenarioSchemaDiagnostic expected) {}
}
