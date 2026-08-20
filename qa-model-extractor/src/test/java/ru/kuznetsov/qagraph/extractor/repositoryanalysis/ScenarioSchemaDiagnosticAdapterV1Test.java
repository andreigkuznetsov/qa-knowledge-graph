package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.JsonNodePath;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScenarioSchemaDiagnosticAdapterV1Test {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREFIX = ScenarioSchemaDiagnosticAdapterV1.RULE_PREFIX;
    private final ScenarioSchemaDiagnosticAdapterV1 adapter = new ScenarioSchemaDiagnosticAdapterV1();

    @Test
    void freezesContractsAndRuleCount() {
        assertEquals("scenario-authority-schema-diagnostic-mapping-v1",
                ScenarioSchemaDiagnosticAdapterV1.CONTRACT_IDENTIFIER);
        assertEquals("scenario-authority-schema-diagnostic-v1",
                ScenarioSchemaDiagnostic.DIAGNOSTIC_CONTRACT_VERSION);
        assertEquals(36, ScenarioSchemaDiagnosticAdapterV1.ruleCount());
    }

    @TestFactory
    Stream<DynamicTest> mapsEveryOneOfTheThirtySixRules() {
        List<RuleCase> cases = ruleCases();
        assertEquals(36, cases.size());
        assertEquals(36, cases.stream().map(RuleCase::pointer).distinct().count());
        return cases.stream().map(rule -> DynamicTest.dynamicTest(rule.pointer(), () -> {
            List<ScenarioSchemaDiagnostic> diagnostics = adapter.map(rule.instance(),
                    List.of(signal(rule.keyword(), rule.pointer(), "")));
            assertFalse(diagnostics.isEmpty());
            assertTrue(diagnostics.stream().allMatch(value ->
                    value.diagnosticContractVersion().equals(ScenarioSchemaDiagnostic.DIAGNOSTIC_CONTRACT_VERSION)
                            && value.stableCode() == ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION
                            && value.instanceLocation().isEmpty()
                            && value.normativeSchemaKeyword().equals(rule.keyword())
                            && value.schemaRuleIdentifier().equals(PREFIX + rule.pointer())));
        }));
    }

    @Test
    void coversAllNineKeywordParameterShapesWithGoldenRecords() {
        Set<String> shapes = ruleCases().stream().map(RuleCase::keyword)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("type", "required", "additionalProperties", "const", "minLength",
                "maxLength", "pattern", "minItems", "uniqueItems"), shapes);

        assertEquals(List.of(new ScenarioSchemaDiagnostic.TextParameter("expectedType", "object")),
                mapOne("type", "/type", BooleanNode.TRUE).typedParameters());
        assertTrue(diagnostics("required", "/required", JSON.createObjectNode()).stream()
                .anyMatch(value -> value.typedParameters().equals(List.of(
                        new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")))));
        assertEquals(List.of(new ScenarioSchemaDiagnostic.TextParameter("unexpectedProperty", "unexpected")),
                mapOne("additionalProperties", "/additionalProperties",
                        JSON.createObjectNode().put("unexpected", true)).typedParameters());
        assertEquals(List.of(new ScenarioSchemaDiagnostic.TextParameter(
                        "expectedText", "qaip-scenario-authority-manifest-v1")),
                mapOne("const", "/properties/format/const", TextNode.valueOf("wrong")).typedParameters());
        assertEquals(List.of(new ScenarioSchemaDiagnostic.Unsigned64Parameter(
                        "minimumCodePointLength", 1)),
                mapOne("minLength", "/$defs/authority/minLength", TextNode.valueOf("")).typedParameters());
        assertEquals(List.of(new ScenarioSchemaDiagnostic.Unsigned64Parameter(
                        "maximumCodePointLength", 200)),
                mapOne("maxLength", "/$defs/authority/maxLength",
                        TextNode.valueOf("😀".repeat(201))).typedParameters());
        ScenarioSchemaDiagnostic pattern = mapOne("pattern", "/$defs/nonBlankString/pattern",
                TextNode.valueOf("   "));
        assertEquals(List.of(new ScenarioSchemaDiagnostic.TextParameter("requiredPattern", ".*\\S.*")),
                pattern.typedParameters());
        String patternValue = ((ScenarioSchemaDiagnostic.TextParameter)
                pattern.typedParameters().getFirst()).value();
        assertEquals(1, patternValue.codePoints().filter(value -> value == '\\').count());
        assertEquals(List.of(new ScenarioSchemaDiagnostic.Unsigned64Parameter("minimumItemCount", 1)),
                mapOne("minItems", "/$defs/steps/minItems", JSON.createArrayNode()).typedParameters());
        assertEquals(List.of(
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("duplicateIndex", 1),
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("firstIndex", 0)),
                mapOne("uniqueItems", "/$defs/scenario/properties/ruleRefs/uniqueItems",
                        array("1", "1.0")).typedParameters());
    }

    @Test
    void networkntBoundaryMapsRootNestedTypesAndEveryConst() throws Exception {
        ScenarioSchemaDiagnostic root = adapter.validate(JSON.readTree("[]")).getFirst();
        assertEquals(PREFIX + "/type", root.schemaRuleIdentifier());
        assertEquals("", root.instanceLocation());

        ObjectNode scenariosType = validManifest();
        scenariosType.put("scenarios", true);
        assertContains(adapter.validate(scenariosType), "/properties/scenarios/type", "/scenarios");

        ObjectNode wrongConsts = validManifest();
        wrongConsts.put("format", "wrong");
        wrongConsts.put("schemaVersion", "wrong");
        wrongConsts.put("scenarioIdentityScheme", "wrong");
        ((ObjectNode) wrongConsts.path("scenarios").get(0).path("operationRef"))
                .put("identityScheme", "wrong");
        List<ScenarioSchemaDiagnostic> diagnostics = adapter.validate(wrongConsts);
        assertContains(diagnostics, "/properties/format/const", "/format");
        assertContains(diagnostics, "/properties/schemaVersion/const", "/schemaVersion");
        assertContains(diagnostics, "/properties/scenarioIdentityScheme/const", "/scenarioIdentityScheme");
        assertContains(diagnostics,
                "/$defs/httpOperationReference/properties/identityScheme/const",
                "/scenarios/0/operationRef/identityScheme");
    }

    @Test
    void expandsMultipleRequiredAndAdditionalProperties() {
        List<ScenarioSchemaDiagnostic> required = diagnostics("required", "/required", JSON.createObjectNode());
        assertEquals(List.of("format", "authority", "scenarios", "schemaVersion", "scenarioIdentityScheme"),
                required.stream().map(ScenarioSchemaDiagnosticAdapterV1Test::onlyTextValue).toList());

        ObjectNode extras = JSON.createObjectNode().put("z", true).put("a", true);
        List<ScenarioSchemaDiagnostic> additional = diagnostics(
                "additionalProperties", "/additionalProperties", extras);
        assertEquals(List.of("a", "z"),
                additional.stream().map(ScenarioSchemaDiagnosticAdapterV1Test::onlyTextValue).toList());
    }

    @Test
    void uniqueItemsUsesJsonSchemaEqualityAndLowestIndexExpansion() throws Exception {
        ArrayNode values = (ArrayNode) JSON.readTree("""
                [{"a":1,"b":[true,null]},{"b":[true,null],"a":1.0},{"a":1,"b":[true,null]},{"a":2}]
                """);
        List<ScenarioSchemaDiagnostic> diagnostics = diagnostics(
                "uniqueItems", "/$defs/scenario/properties/ruleRefs/uniqueItems", values);
        assertEquals(2, diagnostics.size());
        assertEquals(List.of(BigInteger.ONE, BigInteger.TWO), diagnostics.stream()
                .map(value -> ((ScenarioSchemaDiagnostic.Unsigned64Parameter)
                        value.typedParameters().getFirst()).value()).toList());
        assertTrue(diagnostics.stream().allMatch(value ->
                ((ScenarioSchemaDiagnostic.Unsigned64Parameter)
                        value.typedParameters().get(1)).value().equals(BigInteger.ZERO)));
    }

    @Test
    void maxLengthCountsUnicodeCodePointsRatherThanUtf16Units() {
        TextNode exactlyTwoHundredSupplementaryPoints = TextNode.valueOf("😀".repeat(200));
        assertFailure(() -> adapter.map(exactlyTwoHundredSupplementaryPoints, List.of(signal(
                "maxLength", "/$defs/authority/maxLength", ""))));

        ScenarioSchemaDiagnostic violation = mapOne(
                "maxLength", "/$defs/authority/maxLength", TextNode.valueOf("😀".repeat(201)));
        assertEquals(List.of(new ScenarioSchemaDiagnostic.Unsigned64Parameter(
                "maximumCodePointLength", 200)), violation.typedParameters());
    }

    @Test
    void rfc6901EscapingIsConstructedAndResolvedExactly() throws Exception {
        JsonNode document = JSON.readTree("{\"a/b\":{\"~x\":\"\"}}");
        ScenarioSchemaDiagnostic diagnostic = adapter.map(document, List.of(signal(
                "minLength", "/$defs/nonBlankString/minLength", "/a~1b/~0x"))).getFirst();
        assertEquals("/a~1b/~0x", diagnostic.instanceLocation());
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "/bad~2escape", "type", PREFIX + "/type",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("expectedType", "object"))));
    }

    @Test
    void rfc6901AcceptsCanonicalArrayIndexesAndRejectsNoncanonicalForms() throws Exception {
        JsonNode document = JSON.readTree("[{\"value\":\"\"}]");
        ScenarioSchemaDiagnostic diagnostic = adapter.map(document, List.of(signal(
                "minLength", "/$defs/nonBlankString/minLength", "/0/value"))).getFirst();
        assertEquals("/0/value", diagnostic.instanceLocation());

        assertFailure(() -> adapter.map(document, List.of(signal(
                "minLength", "/$defs/nonBlankString/minLength", "/00/value"))));
        assertFailure(() -> adapter.map(document, List.of(signal(
                "minLength", "/$defs/nonBlankString/minLength", "/+0/value"))));
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "not/a/pointer", "type", PREFIX + "/type",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("expectedType", "object"))));
    }

    @Test
    void uniqueItemsHandlesIndependentGroupsAndOrdersTheirLowestIndexPairs() throws Exception {
        ArrayNode values = (ArrayNode) JSON.readTree("""
                [{"a":1,"b":2},{"b":2,"a":1.0},2,2.0,{"a":1.00,"b":2}]
                """);

        List<ScenarioSchemaDiagnostic> diagnostics = diagnostics(
                "uniqueItems", "/$defs/scenario/properties/ruleRefs/uniqueItems", values);

        assertEquals(List.of("0:1", "2:3", "0:4"), diagnostics.stream()
                .map(ScenarioSchemaDiagnosticAdapterV1Test::indexPair).toList());
    }

    @Test
    void validatorOwnedMetadataAndEmissionOrderDoNotAffectCanonicalDiagnostics() {
        JsonNodePath rootLocation = mock(JsonNodePath.class);
        when(rootLocation.getNameCount()).thenReturn(0);
        ValidationMessage first = mock(ValidationMessage.class);
        when(first.getType()).thenReturn("type");
        when(first.getSchemaLocation()).thenReturn(SchemaLocation.of("https://one.example/schema#/type"));
        when(first.getInstanceLocation()).thenReturn(rootLocation);
        when(first.getMessage()).thenReturn("localized message one");
        when(first.getMessageKey()).thenReturn("message.key.one");
        when(first.getCode()).thenReturn("library-code-one");
        when(first.getEvaluationPath()).thenReturn(rootLocation);
        when(first.getArguments()).thenReturn(new Object[]{"one"});
        when(first.getDetails()).thenReturn(Map.of("library", "one"));
        when(first.getInstanceNode()).thenReturn(BooleanNode.TRUE);
        when(first.getSchemaNode()).thenReturn(JSON.createObjectNode().put("rendering", "one"));

        ValidationMessage second = mock(ValidationMessage.class);
        when(second.getType()).thenReturn("type");
        when(second.getSchemaLocation()).thenReturn(SchemaLocation.of("https://two.example/other#/type"));
        when(second.getInstanceLocation()).thenReturn(rootLocation);
        when(second.getMessage()).thenReturn("completely different prose");
        when(second.getMessageKey()).thenReturn("another.key");
        when(second.getCode()).thenReturn("different-code");
        JsonNodePath differentEvaluationPath = mock(JsonNodePath.class);
        when(second.getEvaluationPath()).thenReturn(differentEvaluationPath);
        when(second.getArguments()).thenReturn(new Object[]{"two", 2});
        when(second.getDetails()).thenReturn(Map.of("library", "two"));
        when(second.getInstanceNode()).thenReturn(BooleanNode.FALSE);
        when(second.getSchemaNode()).thenReturn(JSON.createObjectNode().put("rendering", "two"));

        List<ScenarioSchemaDiagnostic> firstResult = adapter.mapValidationMessages(
                BooleanNode.TRUE, List.of(first, second));
        List<ScenarioSchemaDiagnostic> reversedResult = adapter.mapValidationMessages(
                BooleanNode.TRUE, List.of(second, first));

        assertEquals(firstResult, reversedResult);
        assertEquals(1, firstResult.size());
        assertEquals(PREFIX + "/type", firstResult.getFirst().schemaRuleIdentifier());
    }

    @Test
    void consolidatesDuplicatesAndSortsIndependentlyOfEmissionOrder() throws Exception {
        JsonNode document = JSON.readTree("{\"b\":false,\"a\":false}");
        List<ScenarioSchemaDiagnosticAdapterV1.ValidationSignal> signals = new ArrayList<>(List.of(
                signal("type", "/type", "/b"), signal("type", "/type", "/a"),
                signal("type", "/type", "/b")));
        List<ScenarioSchemaDiagnostic> first = adapter.map(document, signals);
        Collections.reverse(signals);
        assertEquals(first, adapter.map(document, signals));
        assertEquals(List.of("/a", "/b"), first.stream()
                .map(ScenarioSchemaDiagnostic::instanceLocation).toList());
    }

    @Test
    void diagnosticModelHasOnlyQaipCanonicalFields() {
        assertEquals("ru.kuznetsov.qaip.evidencegovernance.diagnostic",
                ScenarioSchemaDiagnostic.class.getPackageName());
    }

    @Test
    void adapterUsesEvidenceGovernanceOrderingWithoutCanonicalSerializationCopy() throws Exception {
        String source = Files.readString(Path.of("src/main/java/ru/kuznetsov/qagraph/extractor/"
                + "repositoryanalysis/ScenarioSchemaDiagnosticAdapterV1.java"));
        assertTrue(source.contains("ScenarioSchemaDiagnostic.canonicalOrder()"));
        assertFalse(source.contains("CanonicalBinaryWriter"));
        assertFalse(source.contains("writeOrderedCollection"));
    }

    @Test
    void unknownMalformedAmbiguousIncompleteAndInconsistentSignalsFailClosed() {
        JsonNode object = JSON.createObjectNode();
        assertFailure(() -> adapter.map(object, List.of(signal("type", "/unknown/type", ""))));
        assertFailure(() -> adapter.map(object, List.of(signal("unknown", "/type", ""))));
        assertFailure(() -> adapter.map(object, List.of(signal("required", "/type", ""))));
        assertFailure(() -> adapter.map(object, List.of(signal("type", "/type", "/missing"))));
        assertFailure(() -> new ScenarioSchemaDiagnosticAdapterV1.ValidationSignal(
                "type", "not-a-pointer", ""));
        assertFailure(() -> adapter.map(object, java.util.Arrays.asList((
                ScenarioSchemaDiagnosticAdapterV1.ValidationSignal) null)));
        assertFailure(() -> adapter.map(object, List.of(signal("type", "/type", ""))));
    }

    @Test
    void nullAndIncompleteNetworkntMessagesUseStableMappingFailureWithoutPartialOutput() {
        ValidationMessage missingKeyword = mock(ValidationMessage.class);
        when(missingKeyword.getType()).thenReturn(null);
        ValidationMessage missingSchemaLocation = mock(ValidationMessage.class);
        when(missingSchemaLocation.getType()).thenReturn("type");
        when(missingSchemaLocation.getSchemaLocation()).thenReturn(null);
        ValidationMessage brokenAccessor = mock(ValidationMessage.class);
        when(brokenAccessor.getType()).thenThrow(new IllegalStateException("library failure"));

        assertFailure(() -> adapter.mapValidationMessages(
                BooleanNode.TRUE, Arrays.asList((ValidationMessage) null)));
        assertFailure(() -> adapter.mapValidationMessages(
                BooleanNode.TRUE, List.of(missingKeyword)));
        assertFailure(() -> adapter.mapValidationMessages(
                BooleanNode.TRUE, List.of(missingSchemaLocation)));
        assertFailure(() -> adapter.mapValidationMessages(
                BooleanNode.TRUE, List.of(brokenAccessor)));
        assertFailure(() -> adapter.mapValidationMessages(BooleanNode.TRUE, null));
        assertFailure(() -> adapter.mapValidationMessages(null, List.of()));
        assertFailure(() -> adapter.map(null, List.of()));

        ValidationMessage valid = mock(ValidationMessage.class);
        when(valid.getType()).thenReturn("type");
        when(valid.getSchemaLocation()).thenReturn(SchemaLocation.of("https://example/schema#/type"));
        when(valid.getInstanceLocation()).thenReturn(mock(JsonNodePath.class));
        List<ValidationMessage> partialThenMalformed = Arrays.asList(valid, null);
        assertFailure(() -> adapter.mapValidationMessages(BooleanNode.TRUE, partialThenMalformed));
    }

    @Test
    void repeatedNetworkntCalculationIsDeterministic() {
        ObjectNode invalid = validManifest();
        invalid.remove(List.of("format", "schemaVersion"));
        invalid.put("unexpected", true);
        assertEquals(adapter.validate(invalid), adapter.validate(invalid));
    }

    private ScenarioSchemaDiagnostic mapOne(String keyword, String pointer, JsonNode instance) {
        List<ScenarioSchemaDiagnostic> values = diagnostics(keyword, pointer, instance);
        assertEquals(1, values.size());
        return values.getFirst();
    }

    private List<ScenarioSchemaDiagnostic> diagnostics(String keyword, String pointer, JsonNode instance) {
        return adapter.map(instance, List.of(signal(keyword, pointer, "")));
    }

    private static ScenarioSchemaDiagnosticAdapterV1.ValidationSignal signal(
            String keyword, String pointer, String location) {
        return new ScenarioSchemaDiagnosticAdapterV1.ValidationSignal(keyword, pointer, location);
    }

    private static void assertContains(
            List<ScenarioSchemaDiagnostic> diagnostics, String pointer, String location) {
        assertTrue(diagnostics.stream().anyMatch(value ->
                value.schemaRuleIdentifier().equals(PREFIX + pointer)
                        && value.instanceLocation().equals(location)), diagnostics::toString);
    }

    private static String onlyTextValue(ScenarioSchemaDiagnostic diagnostic) {
        return ((ScenarioSchemaDiagnostic.TextParameter) diagnostic.typedParameters().getFirst()).value();
    }

    private static String indexPair(ScenarioSchemaDiagnostic diagnostic) {
        BigInteger duplicate = ((ScenarioSchemaDiagnostic.Unsigned64Parameter)
                diagnostic.typedParameters().get(0)).value();
        BigInteger first = ((ScenarioSchemaDiagnostic.Unsigned64Parameter)
                diagnostic.typedParameters().get(1)).value();
        return first + ":" + duplicate;
    }

    private static void assertFailure(org.junit.jupiter.api.function.Executable executable) {
        ScenarioSchemaDiagnosticMappingException failure = assertThrows(
                ScenarioSchemaDiagnosticMappingException.class, executable);
        assertEquals("UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING", failure.failureCode());
    }

    private static ArrayNode array(String... jsonValues) {
        ArrayNode result = JSON.createArrayNode();
        for (String value : jsonValues) {
            try {
                result.add(JSON.readTree(value));
            } catch (Exception exception) {
                throw new IllegalArgumentException(exception);
            }
        }
        return result;
    }

    private static List<RuleCase> ruleCases() {
        ObjectNode unexpected = JSON.createObjectNode().put("unexpected", true);
        ObjectNode empty = JSON.createObjectNode();
        ArrayNode duplicate = array("1", "1.0");
        return List.of(
                rule("/type", "type", BooleanNode.TRUE),
                rule("/additionalProperties", "additionalProperties", unexpected),
                rule("/required", "required", empty),
                rule("/properties/format/const", "const", TextNode.valueOf("wrong")),
                rule("/properties/schemaVersion/const", "const", TextNode.valueOf("wrong")),
                rule("/properties/scenarioIdentityScheme/const", "const", TextNode.valueOf("wrong")),
                rule("/properties/scenarios/type", "type", BooleanNode.TRUE),
                rule("/$defs/authority/type", "type", BooleanNode.TRUE),
                rule("/$defs/authority/minLength", "minLength", TextNode.valueOf("")),
                rule("/$defs/authority/maxLength", "maxLength", TextNode.valueOf("😀".repeat(201))),
                rule("/$defs/authority/pattern", "pattern", TextNode.valueOf("bad value")),
                rule("/$defs/stableKey/type", "type", BooleanNode.TRUE),
                rule("/$defs/stableKey/minLength", "minLength", TextNode.valueOf("")),
                rule("/$defs/stableKey/maxLength", "maxLength", TextNode.valueOf("x".repeat(161))),
                rule("/$defs/stableKey/pattern", "pattern", TextNode.valueOf("bad/value")),
                rule("/$defs/identityScheme/type", "type", BooleanNode.TRUE),
                rule("/$defs/identityScheme/minLength", "minLength", TextNode.valueOf("")),
                rule("/$defs/identityScheme/maxLength", "maxLength", TextNode.valueOf("x".repeat(161))),
                rule("/$defs/identityScheme/pattern", "pattern", TextNode.valueOf("bad/value")),
                rule("/$defs/nonBlankString/type", "type", BooleanNode.TRUE),
                rule("/$defs/nonBlankString/minLength", "minLength", TextNode.valueOf("")),
                rule("/$defs/nonBlankString/pattern", "pattern", TextNode.valueOf("   ")),
                rule("/$defs/steps/type", "type", BooleanNode.TRUE),
                rule("/$defs/steps/minItems", "minItems", JSON.createArrayNode()),
                rule("/$defs/scenario/type", "type", BooleanNode.TRUE),
                rule("/$defs/scenario/additionalProperties", "additionalProperties", unexpected),
                rule("/$defs/scenario/required", "required", empty),
                rule("/$defs/scenario/properties/ruleRefs/type", "type", BooleanNode.TRUE),
                rule("/$defs/scenario/properties/ruleRefs/uniqueItems", "uniqueItems", duplicate),
                rule("/$defs/httpOperationReference/type", "type", BooleanNode.TRUE),
                rule("/$defs/httpOperationReference/additionalProperties", "additionalProperties", unexpected),
                rule("/$defs/httpOperationReference/required", "required", empty),
                rule("/$defs/httpOperationReference/properties/identityScheme/const", "const",
                        TextNode.valueOf("wrong")),
                rule("/$defs/businessRuleReference/type", "type", BooleanNode.TRUE),
                rule("/$defs/businessRuleReference/additionalProperties", "additionalProperties", unexpected),
                rule("/$defs/businessRuleReference/required", "required", empty));
    }

    private static RuleCase rule(String pointer, String keyword, JsonNode instance) {
        return new RuleCase(pointer, keyword, instance.deepCopy());
    }

    private static ObjectNode validManifest() {
        try {
            return (ObjectNode) JSON.readTree("""
                    {"format":"qaip-scenario-authority-manifest-v1","schemaVersion":"1.0",
                     "authority":"orders","scenarioIdentityScheme":"qaip-scenario-identity-v1",
                     "scenarios":[{"scenarioKey":"CREATE","title":"Title","given":["A"],
                     "when":["B"],"then":["C"],"operationRef":{
                     "identityScheme":"qaip-http-operation-reference-v1","method":"POST","path":"/orders"},
                     "ruleRefs":[]}]}
                    """);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record RuleCase(String pointer, String keyword, JsonNode instance) {
    }
}
