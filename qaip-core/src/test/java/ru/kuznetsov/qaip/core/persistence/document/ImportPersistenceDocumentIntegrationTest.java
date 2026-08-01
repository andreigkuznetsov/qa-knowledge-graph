package ru.kuznetsov.qaip.core.persistence.document;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportPersistenceDocumentIntegrationTest {
    @Test
    void exact_proof_owned_project_round_trips_with_numeric_and_null_fidelity() throws IOException {
        ProjectImporter importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(), new DefaultProjectApplicationValidator());
        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(projectJson())));
        Project original = success.document().project();

        ProjectPersistenceDocumentCodec codec = new JacksonProjectPersistenceDocumentCodec();
        Project decoded = codec.decode(codec.encode(original));

        assertEquals(original, decoded);
        Map<String, Object> attributes = decoded.metadata().attributes();
        assertEquals(Integer.class, attributes.get("ordinary").getClass());
        assertEquals(Long.class, attributes.get("longMax").getClass());
        assertEquals(new BigInteger("9223372036854775808"), attributes.get("big"));
        assertEquals(new BigDecimal("1234567890.1234567890123456789"), attributes.get("precise"));
        assertTrue(attributes.containsKey("explicitNull"));
        assertEquals(null, attributes.get("explicitNull"));
    }

    private static String projectJson() throws IOException {
        try (var stream = ImportPersistenceDocumentIntegrationTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return json.replace("\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}",
                    "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{" +
                            "\"ordinary\":42,\"longMax\":9223372036854775807," +
                            "\"big\":9223372036854775808," +
                            "\"precise\":1234567890.123456789012345678900,\"explicitNull\":null}}");
        }
    }
}
