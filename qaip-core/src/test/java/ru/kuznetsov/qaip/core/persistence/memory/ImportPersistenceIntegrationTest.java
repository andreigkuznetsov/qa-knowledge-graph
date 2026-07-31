package ru.kuznetsov.qaip.core.persistence.memory;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.application.persistence.DefaultPersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFindingCode;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ImportPersistenceIntegrationTest {
    @Test
    void real_import_proof_is_created_once_and_persisted_without_data_loss_or_replacement() throws IOException {
        ProjectImporter importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(), new DefaultProjectApplicationValidator());
        ProjectImportSuccess imported = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(representativeProjectWithNumericMetadata())));

        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        PersistProject persistence = new DefaultPersistProject(repository);
        PersistProjectAccepted accepted = assertInstanceOf(
                PersistProjectAccepted.class, persistence.execute(imported.document()));
        PersistProjectRejected duplicate = assertInstanceOf(
                PersistProjectRejected.class, persistence.execute(imported.document()));

        assertEquals("P-1", accepted.projectId());
        assertEquals(PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, duplicate.finding().code());
        assertSame(imported.document().project(), repository.storedProject("P-1"));
        assertEquals(new BigInteger("9223372036854775808"),
                repository.storedProject("P-1").metadata().attributes().get("largeInteger"));
        assertEquals(0, new BigDecimal("1234567890.123456789012345678900").compareTo(
                (BigDecimal) repository.storedProject("P-1").metadata().attributes().get("preciseDecimal")));
    }

    private static String representativeProjectWithNumericMetadata() throws IOException {
        try (var stream = ImportPersistenceIntegrationTest.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return json.replace("\"project\":{\"id\":\"P-1\",\"name\":\"Project\"}",
                    "\"project\":{\"id\":\"P-1\",\"name\":\"Project\",\"metadata\":{"
                            + "\"largeInteger\":9223372036854775808,"
                            + "\"preciseDecimal\":1234567890.123456789012345678900}}" );
        }
    }
}
