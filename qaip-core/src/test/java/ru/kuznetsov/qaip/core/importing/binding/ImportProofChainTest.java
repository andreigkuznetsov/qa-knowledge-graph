package ru.kuznetsov.qaip.core.importing.binding;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ImportProofChainTest {
    @Test
    void complete_proof_chain_preserves_domain_and_numeric_values() throws IOException {
        String json = representativeProjectWithNumericMetadata();

        ProjectParseAccepted parseAccepted = assertInstanceOf(ProjectParseAccepted.class,
                new JacksonProjectJsonParser().parse(new RawProjectJson(json)));
        ParsedProjectDocument parsed = assertInstanceOf(ParsedProjectDocument.class, parseAccepted.document());

        SchemaValidationAccepted schemaAccepted = assertInstanceOf(SchemaValidationAccepted.class,
                new NetworkntProjectSchemaValidator().validate(parsed));
        SchemaValidProjectDocument schemaValid = assertInstanceOf(
                SchemaValidProjectDocument.class, schemaAccepted.document());

        BindingSuccess bindingSuccess = assertInstanceOf(BindingSuccess.class,
                new DefaultProjectBinder().bind(schemaValid));
        BoundProjectDocument bound = assertInstanceOf(BoundProjectDocument.class, bindingSuccess.document());
        Project project = bound.project();

        assertEquals("P-1", project.metadata().id());
        assertEquals("local-1", project.subject().localArtifactId());
        assertEquals(2, project.nodes().size());
        assertEquals(1, project.relationships().size());
        assertEquals("jira", project.evidenceManifest().sourceId());
        assertEquals(2, project.declaredChanges().size());

        Map<String, Object> numbers = project.metadata().attributes();
        assertEquals(42, numbers.get("ordinary"));
        assertSame(Integer.class, numbers.get("ordinary").getClass());
        assertEquals(Long.MAX_VALUE, numbers.get("longMax"));
        assertEquals(Long.MIN_VALUE, numbers.get("longMin"));
        assertEquals(new BigInteger("9223372036854775808"), numbers.get("aboveLong"));
        assertEquals(new BigInteger("-9223372036854775809"), numbers.get("belowLong"));
        assertEquals(0, new BigDecimal("1234567890.123456789012345678900")
                .compareTo((BigDecimal) numbers.get("preciseDecimal")));
        assertEquals(0, new BigDecimal("1.234567890123456789e+100")
                .compareTo((BigDecimal) numbers.get("largeExponent")));
    }

    private static String representativeProjectWithNumericMetadata() throws IOException {
        try (var stream = ImportProofChainTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String original = "\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}";
            String replacement = "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{"
                    + "\"ordinary\":42,"
                    + "\"longMax\":9223372036854775807,"
                    + "\"longMin\":-9223372036854775808,"
                    + "\"aboveLong\":9223372036854775808,"
                    + "\"belowLong\":-9223372036854775809,"
                    + "\"preciseDecimal\":1234567890.123456789012345678900,"
                    + "\"largeExponent\":1.234567890123456789e+100}}";
            return json.replace(original, replacement);
        }
    }
}
