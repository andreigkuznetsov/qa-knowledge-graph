package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentSchemaContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static Path schemaRoot;
    private static JsonSchema definitionSchema;
    private static JsonSchema resultSchema;

    @BeforeAll
    static void loadSchemas() throws IOException {
        schemaRoot = Path.of(System.getProperty("qaip.repositoryRoot"))
                .resolve("experiments/schema/v0.1");
        definitionSchema = loadSchema("experiment-definition.schema.json");
        resultSchema = loadSchema("experiment-result.schema.json");
    }

    @Test
    void shouldLoadBothSchemas() {
        assertNotNull(definitionSchema);
        assertNotNull(resultSchema);
    }

    static Stream<String> validDefinitionExamples() {
        return Stream.of("minimal-draft-experiment.json", "ready-experiment.json");
    }

    @ParameterizedTest
    @MethodSource("validDefinitionExamples")
    void shouldAcceptValidDefinitions(String fileName) throws IOException {
        Set<ValidationMessage> messages = validate(definitionSchema, "valid", fileName);
        assertTrue(messages.isEmpty(), () -> fileName + " produced " + messages);
    }

    static Stream<String> validResultExamples() {
        return Stream.of("completed-result-supported.json", "completed-result-inconclusive.json");
    }

    @ParameterizedTest
    @MethodSource("validResultExamples")
    void shouldAcceptValidResults(String fileName) throws IOException {
        assertTrue(validate(resultSchema, "valid", fileName).isEmpty());
    }

    static Stream<InvalidCase> invalidDefinitionExamples() {
        return Stream.of(
                new InvalidCase("invalid-experiment-id.json", "pattern", "$.experimentId"),
                new InvalidCase("invalid-status.json", "enum", "$.status"),
                new InvalidCase("ready-without-frozen-contract.json", "const", "$.hypothesis.frozen"),
                new InvalidCase("ready-without-protocol-checksum.json", "required", "$.protocol"),
                new InvalidCase("ready-without-dataset-integrity-reference.json", "required", "$.dataset"),
                new InvalidCase("automated-criterion-without-comparison.json", "required", "$.criteria.supported[0]"),
                new InvalidCase("invalid-additional-property.json", "additionalProperties", "")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDefinitionExamples")
    void shouldRejectFocusedInvalidDefinitions(InvalidCase invalidCase) throws IOException {
        assertIntendedFailure(definitionSchema, invalidCase);
    }

    static Stream<InvalidCase> invalidResultExamples() {
        return Stream.of(
                new InvalidCase("result-with-invalid-outcome.json", "enum", "$.outcome"),
                new InvalidCase("result-without-evidence.json", "minItems", "$.rawArtifacts"),
                new InvalidCase("result-with-malformed-criterion-id.json", "pattern", "$.criteriaEvaluation[0].criterionId"),
                new InvalidCase("invalid-git-commit-sha.json", "pattern", "$.implementation.repositoryCommit"),
                new InvalidCase("invalid-timestamp-without-timezone.json", "pattern", "$.startedAt")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidResultExamples")
    void shouldRejectFocusedInvalidResults(InvalidCase invalidCase) throws IOException {
        assertIntendedFailure(resultSchema, invalidCase);
    }

    @Test
    void shouldRequireInvalidationMetadata() throws IOException {
        JsonNode definition = readExample("valid", "minimal-draft-experiment.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) definition).put("status", "INVALIDATED");
        assertHasType(definitionSchema.validate(definition), "required");
    }

    @Test
    void shouldRequireSupersessionMetadata() throws IOException {
        JsonNode definition = readExample("valid", "minimal-draft-experiment.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) definition).put("status", "SUPERSEDED");
        assertHasType(definitionSchema.validate(definition), "required");
    }

    private static JsonSchema loadSchema(String fileName) throws IOException {
        try (InputStream input = Files.newInputStream(schemaRoot.resolve(fileName))) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(input);
        }
    }

    private static Set<ValidationMessage> validate(JsonSchema schema, String kind, String fileName)
            throws IOException {
        return schema.validate(readExample(kind, fileName));
    }

    private static JsonNode readExample(String kind, String fileName) throws IOException {
        return MAPPER.readTree(schemaRoot.resolve("examples").resolve(kind).resolve(fileName).toFile());
    }

    private static void assertIntendedFailure(JsonSchema schema, InvalidCase invalidCase)
            throws IOException {
        Set<ValidationMessage> messages = validate(schema, "invalid", invalidCase.fileName());
        assertFalse(messages.isEmpty(), invalidCase.fileName());
        assertTrue(messages.stream().anyMatch(message ->
                        invalidCase.keyword().equals(message.getType())
                                && String.valueOf(message.getInstanceLocation())
                                .contains(invalidCase.path())),
                () -> invalidCase.fileName() + " produced " + messages);
    }

    private static void assertHasType(Set<ValidationMessage> messages, String type) {
        assertTrue(messages.stream().anyMatch(message -> type.equals(message.getType())),
                () -> "Expected " + type + " in " + messages);
    }

    private record InvalidCase(String fileName, String keyword, String path) {
    }
}
