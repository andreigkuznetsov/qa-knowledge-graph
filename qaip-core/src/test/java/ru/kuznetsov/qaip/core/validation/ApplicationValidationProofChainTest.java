package ru.kuznetsov.qaip.core.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.importing.binding.BindingSuccess;
import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocument;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.ParsedProjectDocument;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseAccepted;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationValidationProofChainTest {
    @Test
    void complete_proof_chain_produces_application_valid_document_without_data_loss() throws IOException {
        RawProjectJson raw = new RawProjectJson(representativeProjectWithNumericMetadata());
        ProjectParseAccepted parsedResult = assertInstanceOf(ProjectParseAccepted.class,
                new JacksonProjectJsonParser().parse(raw));
        ParsedProjectDocument parsed = assertInstanceOf(ParsedProjectDocument.class, parsedResult.document());

        SchemaValidationAccepted schemaResult = assertInstanceOf(SchemaValidationAccepted.class,
                new NetworkntProjectSchemaValidator().validate(parsed));
        SchemaValidProjectDocument schemaValid = assertInstanceOf(
                SchemaValidProjectDocument.class, schemaResult.document());

        BindingSuccess bindingResult = assertInstanceOf(BindingSuccess.class,
                new DefaultProjectBinder().bind(schemaValid));
        BoundProjectDocument bound = assertInstanceOf(BoundProjectDocument.class, bindingResult.document());

        ApplicationValidationSuccess validationResult = assertInstanceOf(ApplicationValidationSuccess.class,
                new DefaultProjectApplicationValidator().validate(bound));
        ApplicationValidProjectDocument applicationValid = assertInstanceOf(
                ApplicationValidProjectDocument.class, validationResult.document());
        Project project = applicationValid.project();

        assertSame(bound.project(), project);
        assertTrue(validationResult.warnings().isEmpty());
        assertEquals("P-1", project.metadata().id());
        assertEquals("local-1", project.subject().localArtifactId());
        assertEquals(2, project.nodes().size());
        assertEquals(1, project.relationships().size());
        assertEquals("jira", project.evidenceManifest().sourceId());
        assertEquals(2, project.declaredChanges().size());
        assertEquals(new BigInteger("9223372036854775808"),
                project.metadata().attributes().get("largeInteger"));
        assertEquals(0, new BigDecimal("1234567890.123456789012345678900").compareTo(
                (BigDecimal) project.metadata().attributes().get("preciseDecimal")));
    }

    private static String representativeProjectWithNumericMetadata() throws IOException {
        try (var stream = ApplicationValidationProofChainTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return json.replace("\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}",
                    "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{"
                            + "\"largeInteger\":9223372036854775808,"
                            + "\"preciseDecimal\":1234567890.123456789012345678900}}" );
        }
    }
}
