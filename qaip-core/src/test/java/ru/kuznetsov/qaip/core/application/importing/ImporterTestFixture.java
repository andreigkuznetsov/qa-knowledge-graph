package ru.kuznetsov.qaip.core.application.importing;

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
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationSuccess;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class ImporterTestFixture {
    final ParsedProjectDocument parsed;
    final SchemaValidProjectDocument schemaValid;
    final BoundProjectDocument bound;
    final ApplicationValidProjectDocument applicationValid;

    ImporterTestFixture() {
        RawProjectJson raw = new RawProjectJson(resource());
        parsed = ((ProjectParseAccepted) new JacksonProjectJsonParser().parse(raw)).document();
        schemaValid = ((SchemaValidationAccepted) new NetworkntProjectSchemaValidator()
                .validate(parsed)).document();
        bound = ((BindingSuccess) new DefaultProjectBinder().bind(schemaValid)).document();
        applicationValid = ((ApplicationValidationSuccess) new DefaultProjectApplicationValidator()
                .validate(bound)).document();
    }

    private static String resource() {
        try (var stream = ImporterTestFixture.class.getResourceAsStream(
                "/schema/valid/representative-project.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
