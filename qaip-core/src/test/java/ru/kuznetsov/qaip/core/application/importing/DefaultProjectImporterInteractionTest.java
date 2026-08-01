package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.binding.BindingFailure;
import ru.kuznetsov.qaip.core.importing.binding.BindingFinding;
import ru.kuznetsov.qaip.core.importing.binding.BindingSuccess;
import ru.kuznetsov.qaip.core.importing.binding.ProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JsonInstanceLocation;
import ru.kuznetsov.qaip.core.importing.parsing.JsonSourcePosition;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseAccepted;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseFinding;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseFindingCode;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseRejected;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.importing.schema.JsonSchemaLocation;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFinding;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFindingCode;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationRejected;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationFailure;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationFinding;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationSeverity;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationSuccess;
import ru.kuznetsov.qaip.core.validation.ProjectApplicationValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProjectImporterInteractionTest {
    private final ImporterTestFixture fixture = new ImporterTestFixture();
    private final RawProjectJson source = new RawProjectJson("{}");

    @Test
    void successful_pipeline_invokes_every_stage_once_in_order_and_preserves_warnings() {
        List<String> calls = new ArrayList<>();
        List<ApplicationValidationFinding> warnings = List.of(
                applicationFinding("W-1", "first", "project.nodes[0]"),
                applicationFinding("W-2", "second", "project.nodes[1]"));
        ProjectImporter importer = importer(calls,
                new ProjectParseAccepted(fixture.parsed),
                new SchemaValidationAccepted(fixture.schemaValid),
                new BindingSuccess(fixture.bound),
                new ApplicationValidationSuccess(fixture.applicationValid, warnings));

        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(source));
        assertEquals(List.of("parse", "schema", "bind", "application"), calls);
        assertSame(fixture.applicationValid, success.document());
        assertEquals(List.of("W-1", "W-2"),
                success.warnings().stream().map(ProjectImportFinding::code).toList());
        assertTrue(success.warnings().stream().allMatch(finding ->
                finding.stage() == ProjectImportStage.APPLICATION_VALIDATION
                        && finding.severity() == ProjectImportSeverity.WARNING));
    }

    @Test
    void parsing_failure_adapts_position_and_stops_pipeline() {
        List<String> calls = new ArrayList<>();
        ProjectParseFinding sourceFinding = new ProjectParseFinding(
                ProjectParseFindingCode.MALFORMED_JSON, JsonInstanceLocation.ROOT,
                Optional.of(new JsonSourcePosition(3, 14, 85)), "Malformed input");
        ProjectImporter importer = importer(calls, new ProjectParseRejected(List.of(sourceFinding)),
                new SchemaValidationAccepted(fixture.schemaValid), new BindingSuccess(fixture.bound),
                new ApplicationValidationSuccess(fixture.applicationValid, List.of()));

        ProjectImportFailure failure = assertInstanceOf(ProjectImportFailure.class,
                importer.importProject(source));
        assertEquals(List.of("parse"), calls);
        assertEquals(ProjectImportStage.PARSING, failure.failedStage());
        assertFinding(failure.findings().getFirst(), "MALFORMED_JSON", ProjectImportSeverity.ERROR,
                "Malformed input", "line 3, column 14, offset 85");
    }

    @Test
    void schema_failure_adapts_normalized_finding_and_stops_pipeline() {
        List<String> calls = new ArrayList<>();
        SchemaValidationFinding sourceFinding = new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, JsonInstanceLocation.ROOT,
                new JsonSchemaLocation("https://example/schema#/required"), "required", "Required property is missing.");
        ProjectImporter importer = importer(calls, new ProjectParseAccepted(fixture.parsed),
                new SchemaValidationRejected(List.of(sourceFinding)), new BindingSuccess(fixture.bound),
                new ApplicationValidationSuccess(fixture.applicationValid, List.of()));

        ProjectImportFailure failure = assertInstanceOf(ProjectImportFailure.class,
                importer.importProject(source));
        assertEquals(List.of("parse", "schema"), calls);
        assertEquals(ProjectImportStage.SCHEMA_VALIDATION, failure.failedStage());
        assertFinding(failure.findings().getFirst(), "SCHEMA_VIOLATION", ProjectImportSeverity.ERROR,
                "Required property is missing.", "project");
    }

    @Test
    void binding_failure_preserves_finding_and_stops_pipeline() {
        List<String> calls = new ArrayList<>();
        BindingFinding sourceFinding = new BindingFinding("BINDING_PROBLEM", "Cannot bind value", "project.nodes[0]");
        ProjectImporter importer = importer(calls, new ProjectParseAccepted(fixture.parsed),
                new SchemaValidationAccepted(fixture.schemaValid), new BindingFailure(List.of(sourceFinding)),
                new ApplicationValidationSuccess(fixture.applicationValid, List.of()));

        ProjectImportFailure failure = assertInstanceOf(ProjectImportFailure.class,
                importer.importProject(source));
        assertEquals(List.of("parse", "schema", "bind"), calls);
        assertEquals(ProjectImportStage.BINDING, failure.failedStage());
        assertFinding(failure.findings().getFirst(), "BINDING_PROBLEM", ProjectImportSeverity.ERROR,
                "Cannot bind value", "project.nodes[0]");
    }

    @Test
    void application_failure_preserves_errors_warnings_and_order() {
        List<String> calls = new ArrayList<>();
        List<ApplicationValidationFinding> sourceFindings = List.of(
                new ApplicationValidationFinding("ERROR", ApplicationValidationSeverity.ERROR,
                        "Invalid relationship", "project.relationships[0]"),
                applicationFinding("WARNING", "Uncovered scenario", "project.nodes[0]"));
        ProjectImporter importer = importer(calls, new ProjectParseAccepted(fixture.parsed),
                new SchemaValidationAccepted(fixture.schemaValid), new BindingSuccess(fixture.bound),
                new ApplicationValidationFailure(sourceFindings));

        ProjectImportFailure failure = assertInstanceOf(ProjectImportFailure.class,
                importer.importProject(source));
        assertEquals(List.of("parse", "schema", "bind", "application"), calls);
        assertEquals(ProjectImportStage.APPLICATION_VALIDATION, failure.failedStage());
        assertEquals(List.of("ERROR", "WARNING"),
                failure.findings().stream().map(ProjectImportFinding::code).toList());
        assertEquals(List.of(ProjectImportSeverity.ERROR, ProjectImportSeverity.WARNING),
                failure.findings().stream().map(ProjectImportFinding::severity).toList());
    }

    @Test
    void repeated_equivalent_execution_is_deterministic() {
        ProjectImporter importer = importer(new ArrayList<>(), new ProjectParseAccepted(fixture.parsed),
                new SchemaValidationAccepted(fixture.schemaValid), new BindingSuccess(fixture.bound),
                new ApplicationValidationSuccess(fixture.applicationValid, List.of()));
        assertEquals(importer.importProject(source), importer.importProject(new RawProjectJson("{}")));
    }

    private ProjectImporter importer(List<String> calls, Object parseResult, Object schemaResult,
                                     Object bindingResult, Object applicationResult) {
        ProjectJsonParser parser = ignored -> {
            calls.add("parse");
            return (ru.kuznetsov.qaip.core.importing.parsing.ProjectParseResult) parseResult;
        };
        ProjectSchemaValidator schema = ignored -> {
            calls.add("schema");
            return (ru.kuznetsov.qaip.core.importing.schema.SchemaValidationResult) schemaResult;
        };
        ProjectBinder binder = ignored -> {
            calls.add("bind");
            return (ru.kuznetsov.qaip.core.importing.binding.BindingResult) bindingResult;
        };
        ProjectApplicationValidator application = ignored -> {
            calls.add("application");
            return (ru.kuznetsov.qaip.core.validation.ApplicationValidationResult) applicationResult;
        };
        return new DefaultProjectImporter(parser, schema, binder, application);
    }

    private static ApplicationValidationFinding applicationFinding(String code, String message, String location) {
        return new ApplicationValidationFinding(code, ApplicationValidationSeverity.WARNING, message, location);
    }

    private static void assertFinding(ProjectImportFinding finding, String code,
                                      ProjectImportSeverity severity, String message, String location) {
        assertEquals(code, finding.code());
        assertEquals(severity, finding.severity());
        assertEquals(message, finding.message());
        assertEquals(location, finding.location());
    }
}
