package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidationContractException;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFinding;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFindingCode;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationRejected;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkntProjectSchemaValidatorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ProjectJsonParser PARSER = new JacksonProjectJsonParser();
    private final ProjectSchemaValidator validator = new NetworkntProjectSchemaValidator();

    @Test
    void loadsCanonicalSchemaAndAcceptsStoredValidProjectsWithoutMutation() throws IOException {
        for (String fixture : List.of("minimal-project.json", "representative-project.json",
                "non-canonical-order-project.json", "valid-structurally-semantically-unverified-project.json")) {
            ParsedProjectDocument parsed = parsed(resource("/schema/valid/" + fixture));
            var before = parsed.internalJsonTreeCopy();
            SchemaValidationAccepted accepted = assertInstanceOf(
                    SchemaValidationAccepted.class, validator.validate(parsed), fixture);
            assertEquals(before, parsed.internalJsonTreeCopy());
            assertEquals(before, accepted.document().internalJsonTreeCopy());
            assertNotSame(before, accepted.document().internalJsonTreeCopy());
        }
    }

    @Test
    void rejectsEveryNonObjectRootThroughSchemaTypeKeyword() {
        for (String source : List.of("[]", "\"project\"", "123", "1.25", "true", "null")) {
            ProjectParseAccepted parseAccepted = assertInstanceOf(ProjectParseAccepted.class,
                    PARSER.parse(new RawProjectJson(source)), source);
            SchemaValidationFinding finding = onlyFinding(validator.validate(parseAccepted.document()));
            assertEquals("type", finding.keyword(), source);
            assertEquals("", finding.instanceLocation().value(), source);
        }
    }

    @Test
    void mapsRepresentativeCanonicalSchemaKeywordsAndNestedReferences() throws IOException {
        ObjectNode minimal = objectFixture("minimal-project.json");
        assertHasKeyword(new ParsedProjectDocument(without(minimal, "subject")), "required", "");
        assertHasKeyword(new ParsedProjectDocument(minimal.deepCopy().put("projectContractVersion", "future")),
                "const", "/projectContractVersion");
        assertHasKeyword(new ParsedProjectDocument(minimal.deepCopy().put("subject", "wrong")),
                "type", "/subject");
        assertHasKeyword(new ParsedProjectDocument(minimal.deepCopy().put("unknown", true)),
                "additionalProperties", "");

        ObjectNode emptyChanges = minimal.deepCopy();
        emptyChanges.set("declaredChanges", MAPPER.createArrayNode());
        assertHasKeyword(new ParsedProjectDocument(emptyChanges), "minItems", "/declaredChanges");

        ObjectNode invalidEnum = minimal.deepCopy();
        ((ObjectNode) invalidEnum.at("/declaredChanges/0")).put("artifactCategory", "UNKNOWN");
        assertHasKeyword(new ParsedProjectDocument(invalidEnum), "enum", "/declaredChanges/0/artifactCategory");

        ObjectNode invalidPattern = minimal.deepCopy();
        ((ObjectNode) invalidPattern.get("subject")).put("localArtifactId", " ");
        assertHasKeyword(new ParsedProjectDocument(invalidPattern), "pattern", "/subject/localArtifactId");

        ObjectNode tooLong = minimal.deepCopy();
        ((ObjectNode) tooLong.at("/declaredChanges/0")).put("canonicalIdentity", "A".repeat(121));
        assertHasKeyword(new ParsedProjectDocument(tooLong), "maxLength", "/declaredChanges/0/canonicalIdentity");

        ObjectNode duplicateTags = minimal.deepCopy();
        ((ObjectNode) duplicateTags.at("/declaredChanges/0/afterState"))
                .set("tags", MAPPER.createArrayNode().add("duplicate").add("duplicate"));
        assertHasKeyword(new ParsedProjectDocument(duplicateTags), "uniqueItems",
                "/declaredChanges/0/afterState/tags");

        ObjectNode belowMinimum = withConfidence(minimal, -0.1);
        assertHasKeyword(new ParsedProjectDocument(belowMinimum), "minimum",
                "/declaredChanges/0/afterState/sourceReferences/0/confidence");
        ObjectNode aboveMaximum = withConfidence(minimal, 1.1);
        assertHasKeyword(new ParsedProjectDocument(aboveMaximum), "maximum",
                "/declaredChanges/0/afterState/sourceReferences/0/confidence");

        ObjectNode forbiddenBeforeState = minimal.deepCopy();
        ((ObjectNode) forbiddenBeforeState.at("/declaredChanges/0")).set("beforeState",
                forbiddenBeforeState.at("/declaredChanges/0/afterState").deepCopy());
        assertHasKeyword(new ParsedProjectDocument(forbiddenBeforeState), "not", "/declaredChanges/0");

        ObjectNode invalidRef = minimal.deepCopy();
        ((ObjectNode) invalidRef.get("baseModel")).remove("project");
        assertHasKeyword(new ParsedProjectDocument(invalidRef), "required", "/baseModel");

        ObjectNode invalidArrayItem = minimal.deepCopy();
        ((ArrayNode) invalidArrayItem.get("declaredChanges")).addNull();
        assertHasKeyword(new ParsedProjectDocument(invalidArrayItem), "type", "/declaredChanges/1");

        ObjectNode invalidOneOf = minimal.deepCopy();
        ObjectNode resolution = MAPPER.createObjectNode().put("status", "RESOLVED");
        ObjectNode assertion = MAPPER.createObjectNode()
                .put("assertionId", "a").set("snapshot", minimal.at("/evidenceManifest/snapshot").deepCopy());
        assertion.put("localArtifactId", "local").put("nodeType", "BUSINESS_RULE")
                .set("resolution", resolution);
        assertion.put("contentFingerprint", "sha256:a").put("provenanceId", "p");
        ((ArrayNode) invalidOneOf.at("/evidenceManifest/identityAssertions")).add(assertion);
        assertHasKeyword(new ParsedProjectDocument(invalidOneOf), "oneOf",
                "/evidenceManifest/identityAssertions/0/resolution");
    }

    @Test
    void preservesAllDistinctFindingsInCanonicalDeterministicOrder() throws IOException {
        ObjectNode invalid = objectFixture("minimal-project.json");
        invalid.remove("subject");
        invalid.put("unknown", true);
        invalid.put("projectContractVersion", "future");
        ((ObjectNode) invalid.get("analysisContext")).remove("algorithmVersion");

        SchemaValidationRejected first = rejected(validator.validate(new ParsedProjectDocument(invalid)));
        assertTrue(first.findings().size() >= 4);
        assertTrue(first.findings().stream().anyMatch(f -> f.keyword().equals("required")));
        assertTrue(first.findings().stream().anyMatch(f -> f.keyword().equals("const")));
        assertTrue(first.findings().stream().anyMatch(f -> f.keyword().equals("additionalProperties")));
        assertEquals(first.findings().stream().distinct().count(), first.findings().size());
        for (int repeat = 0; repeat < 5; repeat++) {
            assertEquals(first, validator.validate(new ParsedProjectDocument(invalid)));
        }
    }

    @Test
    void mapsEscapedStructuredInstancePathsWithoutMessageParsing() {
        NetworkntProjectSchemaValidator escapedValidator = new NetworkntProjectSchemaValidator(Map.of(
                NetworkntProjectSchemaValidator.PROJECT_SCHEMA, "/schema/escaped-path-schema.json"));
        SchemaValidationFinding finding = onlyFinding(escapedValidator.validate(parsed("{\"a/b\":{\"m~n\":\"bad\"}}")));
        assertEquals("/a~1b/m~0n", finding.instanceLocation().value());
        assertEquals("type", finding.keyword());
    }

    @Test
    void mapsFindingsWithoutLeakingLibraryMessagesOrObjects() {
        SchemaValidationFinding finding = onlyFinding(validator.validate(parsed("{}")));
        assertEquals(SchemaValidationFindingCode.SCHEMA_VIOLATION, finding.code());
        assertFalse(finding.schemaLocation().value().isBlank());
        assertFalse(finding.keyword().isBlank());
        assertFalse(finding.message().isBlank());
        assertFalse(finding.message().contains("$"));
    }

    @Test
    void validationIsDeterministicForAcceptedAndRepresentativeRejectedDocuments() throws IOException {
        ParsedProjectDocument valid = parsed(resource("/schema/valid/minimal-project.json"));
        for (int repeat = 0; repeat < 5; repeat++) {
            assertEquals(validator.validate(valid), validator.validate(valid));
        }
        for (String invalid : List.of("{}", "[]")) {
            SchemaValidationResult first = validator.validate(parsed(invalid));
            for (int repeat = 0; repeat < 5; repeat++) assertEquals(first, validator.validate(parsed(invalid)));
        }
    }

    @Test
    void sharedValidatorSupportsConcurrentMixedDocuments() throws Exception {
        String minimal = resource("/schema/valid/minimal-project.json");
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<SchemaValidationResult>> futures = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                int kind = index % 5;
                String source = switch (kind) {
                    case 0 -> minimal;
                    case 1 -> "[]";
                    case 2 -> "{}";
                    case 3 -> minimal.replace("\"qaip-project-v1\"", "\"future\"");
                    default -> minimal.substring(0, minimal.lastIndexOf('}')) + ",\"unknown\":true}";
                };
                futures.add(executor.submit(() -> validator.validate(parsed(source))));
            }
            for (int index = 0; index < futures.size(); index++) {
                SchemaValidationResult result = futures.get(index).get();
                if (index % 5 == 0) assertInstanceOf(SchemaValidationAccepted.class, result);
                else assertInstanceOf(SchemaValidationRejected.class, result);
            }
        }
    }

    @Test
    void schemaInitializationFailuresRemainContractExceptions() {
        assertThrows(ProjectSchemaValidationContractException.class,
                () -> new NetworkntProjectSchemaValidator(Map.of(
                        NetworkntProjectSchemaValidator.PROJECT_SCHEMA, "/schema/missing.json")));
        assertThrows(ProjectSchemaValidationContractException.class,
                () -> new NetworkntProjectSchemaValidator(Map.of(
                        NetworkntProjectSchemaValidator.PROJECT_SCHEMA, "/schema/invalid-schema.json")));
        NetworkntProjectSchemaValidator unresolved = new NetworkntProjectSchemaValidator(Map.of(
                NetworkntProjectSchemaValidator.PROJECT_SCHEMA, "/schema/unresolved-schema.json"));
        assertThrows(ProjectSchemaValidationContractException.class, () -> unresolved.validate(parsed("{}")));
    }

    @Test
    void nullDocumentIsAProgrammingError() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    private void assertHasKeyword(ParsedProjectDocument document, String keyword, String instanceLocation) {
        List<SchemaValidationFinding> findings = rejected(validator.validate(document)).findings().stream()
                .filter(finding -> finding.keyword().equals(keyword)
                        && finding.instanceLocation().value().equals(instanceLocation)).toList();
        assertFalse(findings.isEmpty(), () -> "Missing " + keyword + " at " + instanceLocation);
        findings.forEach(finding -> {
            assertEquals(SchemaValidationFindingCode.SCHEMA_VIOLATION, finding.code());
            assertTrue(finding.schemaLocation().value().startsWith("https://example.local/schemas/"));
            assertFalse(finding.message().isBlank());
        });
    }

    private static SchemaValidationFinding onlyFinding(SchemaValidationResult result) {
        SchemaValidationRejected rejected = rejected(result);
        assertEquals(1, rejected.findings().size(), rejected.findings().toString());
        return rejected.findings().getFirst();
    }

    private static SchemaValidationRejected rejected(SchemaValidationResult result) {
        return assertInstanceOf(SchemaValidationRejected.class, result);
    }

    private static ParsedProjectDocument parsed(String json) {
        return assertInstanceOf(ProjectParseAccepted.class,
                PARSER.parse(new RawProjectJson(json))).document();
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = NetworkntProjectSchemaValidatorTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing test resource " + path);
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static ObjectNode objectFixture(String name) throws IOException {
        return (ObjectNode) MAPPER.readTree(resource("/schema/valid/" + name));
    }

    private static ObjectNode without(ObjectNode source, String member) {
        ObjectNode copy = source.deepCopy(); copy.remove(member); return copy;
    }

    private static ObjectNode withConfidence(ObjectNode source, double confidence) {
        ObjectNode copy = source.deepCopy();
        ObjectNode reference = MAPPER.createObjectNode().put("sourceId", "source-1").put("confidence", confidence);
        ((ObjectNode) copy.at("/declaredChanges/0/afterState"))
                .set("sourceReferences", MAPPER.createArrayNode().add(reference));
        return copy;
    }
}
