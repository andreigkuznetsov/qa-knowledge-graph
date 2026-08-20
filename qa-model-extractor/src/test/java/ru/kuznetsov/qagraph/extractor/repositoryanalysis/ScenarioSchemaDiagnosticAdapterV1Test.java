package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Public-path conformance; no test-only production mapping seam is used. */
class ScenarioSchemaDiagnosticAdapterV1Test {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ScenarioSchemaDiagnosticAdapterV1 adapter = new ScenarioSchemaDiagnosticAdapterV1();

    @Test void publicValidatedPathIsDeterministicCanonicalAndImmutable() {
        ObjectNode invalid = validManifest();
        invalid.remove("format");
        invalid.put("z-extra", true);
        invalid.put("a-extra", true);
        List<ScenarioSchemaDiagnostic> first = adapter.validate(invalid);
        assertEquals(first, adapter.validate(invalid));
        assertThrows(UnsupportedOperationException.class, first::clear);
        assertTrue(first.stream().anyMatch(x -> x.normativeSchemaKeyword().equals("required")));
        assertEquals(List.of("a-extra", "z-extra"), first.stream()
                .filter(x -> x.normativeSchemaKeyword().equals("additionalProperties"))
                .map(x -> ((ScenarioSchemaDiagnostic.TextParameter) x.typedParameters().getFirst()).value())
                .toList());
        assertEquals(first.stream().sorted(ScenarioSchemaDiagnostic.canonicalOrder()).toList(), first);
    }

    @Test void publicPathPreservesUnicodeCodePointLengthSemantics() {
        ObjectNode accepted = validManifest();
        accepted.put("authority", "x" + "😀".repeat(199));
        assertTrue(adapter.validate(accepted).stream().noneMatch(x ->
                x.normativeSchemaKeyword().equals("maxLength")));
        ObjectNode rejected = validManifest();
        rejected.put("authority", "x" + "😀".repeat(200));
        assertTrue(adapter.validate(rejected).stream().anyMatch(x ->
                x.normativeSchemaKeyword().equals("maxLength")
                        && x.instanceLocation().equals("/authority")));
    }

    @Test void publicPathProducesCanonicalUniqueItemPositions() {
        ObjectNode invalid = validManifest();
        ArrayNode refs = (ArrayNode) invalid.path("scenarios").get(0).path("ruleRefs");
        ObjectNode first = rule("rules", "R-1");
        refs.add(first); refs.add(rule("rules", "R-2")); refs.add(first.deepCopy());
        ScenarioSchemaDiagnostic unique = adapter.validate(invalid).stream()
                .filter(x -> x.normativeSchemaKeyword().equals("uniqueItems")).findFirst().orElseThrow();
        assertEquals("/scenarios/0/ruleRefs", unique.instanceLocation());
        assertEquals(List.of("duplicateIndex", "firstIndex"),
                unique.typedParameters().stream().map(ScenarioSchemaDiagnostic.CanonicalTypedParameter::name).toList());
    }

    @Test void productionAdapterExposesOnlyCompletePinnedValidationAuthority() throws Exception {
        assertTrue(Arrays.stream(ScenarioSchemaDiagnosticAdapterV1.class.getDeclaredMethods())
                .filter(method -> method.getReturnType().equals(List.class))
                .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .allMatch(method -> Modifier.isPublic(method.getModifiers())
                        && method.getName().equals("validate")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{com.fasterxml.jackson.databind.JsonNode.class})));
        Path sourcePath = repositoryRoot().resolve(
                "qa-model-extractor/src/main/java/ru/kuznetsov/qagraph/extractor/repositoryanalysis/ScenarioSchemaDiagnosticAdapterV1.java");
        String source = Files.readString(sourcePath);
        assertFalse(source.contains("com.networknt"));
        assertFalse(source.contains("ValidationMessage"));
        assertFalse(source.contains("signal.message()"));
        assertTrue(source.contains("validator.validationSignals(document)"));
        assertTrue(source.contains("private List<ScenarioSchemaDiagnostic> mapValidationSignals"));
        assertTrue(source.contains("private List<ScenarioSchemaDiagnostic> map("));
    }

    private static ObjectNode validManifest() {
        ObjectNode root = JSON.createObjectNode();
        root.put("format", "qaip-scenario-authority-manifest-v1");
        root.put("schemaVersion", "1.0"); root.put("authority", "orders");
        root.put("scenarioIdentityScheme", "qaip-scenario-identity-v1");
        ObjectNode scenario = root.putArray("scenarios").addObject();
        scenario.put("scenarioKey", "CREATE"); scenario.put("title", "Create order");
        scenario.putArray("given").add("ready"); scenario.putArray("when").add("create");
        scenario.putArray("then").add("created");
        ObjectNode operation = scenario.putObject("operationRef");
        operation.put("identityScheme", "qaip-http-operation-reference-v1");
        operation.put("method", "POST"); operation.put("path", "/orders");
        scenario.putArray("ruleRefs"); return root;
    }

    private static ObjectNode rule(String authority, String key) {
        ObjectNode rule = JSON.createObjectNode(); rule.put("authority", authority);
        rule.put("stableRuleKey", key); rule.put("identityScheme", "qaip-business-rule-identity-v1");
        return rule;
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
        return java.util.Objects.requireNonNull(current, "repository root");
    }
}
