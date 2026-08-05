package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalImplementationRoleSchemaTest {
    private static final List<String> ROLES = List.of(
            "REST_CONTROLLER",
            "APPLICATION_SERVICE",
            "REPOSITORY",
            "MESSAGE_PRODUCER",
            "MESSAGE_DESTINATION",
            "MESSAGE_CONSUMER");
    private static final JsonSchema SCHEMA = new LocalProjectSchemaResolver()
            .resolve(LocalProjectSchemaResolver.QA_MODEL);
    private static final Path SYNCHRONOUS_PROJECT = Path.of(System.getProperty("qaip.repositoryRoot"))
            .resolve("qa-model-validator/src/test/resources/valid-qa-model.json");

    @Test
    void acceptsEveryCanonicalImplementationRole() throws IOException {
        ObjectNode project = synchronousProject();
        ObjectNode implementation = firstTechnicalImplementation(project);

        for (String role : ROLES) {
            implementation.put("implementationRole", role);
            assertTrue(SCHEMA.validate(project).isEmpty(), role);
        }
    }

    @Test
    void acceptsExistingSynchronousProjectWithoutRoleAndPreservesFlowStage() throws IOException {
        ObjectNode project = synchronousProject();
        ObjectNode implementation = firstTechnicalImplementation(project);
        ObjectNode details = (ObjectNode) implementation.get("details");
        details.put("flowStage", "SERVICE");

        assertFalse(implementation.has("implementationRole"));
        assertTrue(SCHEMA.validate(project).isEmpty());
        assertTrue(details.has("flowStage"));
    }

    @Test
    void rejectsInvalidImplementationRole() throws IOException {
        ObjectNode project = synchronousProject();
        firstTechnicalImplementation(project).put("implementationRole", "KAFKA_PRODUCER");

        Set<ValidationMessage> errors = SCHEMA.validate(project);
        assertTrue(errors.stream().anyMatch(error ->
                error.getInstanceLocation().toString().endsWith(".implementationRole")
                        && "enum".equals(error.getType())), errors::toString);
    }

    private static ObjectNode synchronousProject() throws IOException {
        return (ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(SYNCHRONOUS_PROJECT.toFile());
    }

    private static ObjectNode firstTechnicalImplementation(ObjectNode project) {
        for (var node : project.withArray("nodes")) {
            if ("TECHNICAL_IMPLEMENTATION".equals(node.path("type").asText())) {
                return (ObjectNode) node.get("technicalImplementation");
            }
        }
        throw new AssertionError("Fixture contains no technical implementation");
    }
}
