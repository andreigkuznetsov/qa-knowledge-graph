package ru.kuznetsov.qaip.core.application.persistence;

import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class PersistenceProofFixture {
    final ApplicationValidProjectDocument document;

    PersistenceProofFixture() {
        ProjectImporter importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(), new DefaultProjectApplicationValidator());
        document = ((ProjectImportSuccess) importer.importProject(new RawProjectJson(resource()))).document();
    }

    private static String resource() {
        try (var stream = PersistenceProofFixture.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
