package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectImportContractsTest {
    private final ImporterTestFixture fixture = new ImporterTestFixture();

    @Test
    void finding_rejects_null_and_blank_fields() {
        assertThrows(NullPointerException.class, () -> finding(null, "CODE", ProjectImportSeverity.ERROR, "message", "project"));
        assertThrows(NullPointerException.class, () -> finding(ProjectImportStage.PARSING, null, ProjectImportSeverity.ERROR, "message", "project"));
        assertThrows(NullPointerException.class, () -> finding(ProjectImportStage.PARSING, "CODE", null, "message", "project"));
        assertThrows(NullPointerException.class, () -> finding(ProjectImportStage.PARSING, "CODE", ProjectImportSeverity.ERROR, null, "project"));
        assertThrows(NullPointerException.class, () -> finding(ProjectImportStage.PARSING, "CODE", ProjectImportSeverity.ERROR, "message", null));
        for (String blank : List.of("", " ", "\t")) {
            assertThrows(IllegalArgumentException.class,
                    () -> finding(ProjectImportStage.PARSING, blank, ProjectImportSeverity.ERROR, "message", "project"));
            assertThrows(IllegalArgumentException.class,
                    () -> finding(ProjectImportStage.PARSING, "CODE", ProjectImportSeverity.ERROR, blank, "project"));
            assertThrows(IllegalArgumentException.class,
                    () -> finding(ProjectImportStage.PARSING, "CODE", ProjectImportSeverity.ERROR, "message", blank));
        }
    }

    @Test
    void success_enforces_document_warning_and_immutability_contracts() {
        ProjectImportFinding warning = finding(ProjectImportStage.APPLICATION_VALIDATION,
                "WARNING", ProjectImportSeverity.WARNING, "warning", "project.nodes[0]");
        List<ProjectImportFinding> caller = new ArrayList<>(List.of(warning));
        ProjectImportSuccess success = new ProjectImportSuccess(fixture.applicationValid, caller);
        caller.clear();
        assertEquals(List.of(warning), success.warnings());
        assertThrows(UnsupportedOperationException.class, () -> success.warnings().clear());
        assertThrows(NullPointerException.class, () -> new ProjectImportSuccess(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProjectImportSuccess(fixture.applicationValid, List.of(
                finding(ProjectImportStage.APPLICATION_VALIDATION, "ERROR", ProjectImportSeverity.ERROR,
                        "error", "project.nodes[0]"))));
    }

    @Test
    void failure_enforces_stage_error_and_immutability_contracts() {
        ProjectImportFinding error = finding(ProjectImportStage.BINDING,
                "ERROR", ProjectImportSeverity.ERROR, "error", "project");
        List<ProjectImportFinding> caller = new ArrayList<>(List.of(error));
        ProjectImportFailure failure = new ProjectImportFailure(ProjectImportStage.BINDING, caller);
        caller.clear();
        assertEquals(List.of(error), failure.findings());
        assertThrows(UnsupportedOperationException.class, () -> failure.findings().clear());
        assertThrows(NullPointerException.class, () -> new ProjectImportFailure(null, List.of(error)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectImportFailure(ProjectImportStage.BINDING, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProjectImportFailure(
                ProjectImportStage.PARSING, List.of(error)));
        assertThrows(IllegalArgumentException.class, () -> new ProjectImportFailure(
                ProjectImportStage.BINDING, List.of(finding(ProjectImportStage.BINDING,
                        "WARNING", ProjectImportSeverity.WARNING, "warning", "project"))));
    }

    @Test
    void importer_rejects_null_dependencies_and_source() {
        var parser = (ru.kuznetsov.qaip.core.importing.parsing.ProjectJsonParser) ignored -> null;
        var schema = (ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidator) ignored -> null;
        var binder = (ru.kuznetsov.qaip.core.importing.binding.ProjectBinder) ignored -> null;
        var application = (ru.kuznetsov.qaip.core.validation.ProjectApplicationValidator) ignored -> null;
        assertThrows(NullPointerException.class, () -> new DefaultProjectImporter(null, schema, binder, application));
        assertThrows(NullPointerException.class, () -> new DefaultProjectImporter(parser, null, binder, application));
        assertThrows(NullPointerException.class, () -> new DefaultProjectImporter(parser, schema, null, application));
        assertThrows(NullPointerException.class, () -> new DefaultProjectImporter(parser, schema, binder, null));
        assertThrows(NullPointerException.class,
                () -> new DefaultProjectImporter(parser, schema, binder, application).importProject(null));
    }

    private static ProjectImportFinding finding(ProjectImportStage stage, String code,
                                                ProjectImportSeverity severity, String message, String location) {
        return new ProjectImportFinding(stage, code, severity, message, location);
    }
}
