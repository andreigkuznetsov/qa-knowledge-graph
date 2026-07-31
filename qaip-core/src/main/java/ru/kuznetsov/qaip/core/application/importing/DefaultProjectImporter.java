package ru.kuznetsov.qaip.core.application.importing;

import ru.kuznetsov.qaip.core.importing.binding.BindingFailure;
import ru.kuznetsov.qaip.core.importing.binding.BindingFinding;
import ru.kuznetsov.qaip.core.importing.binding.BindingSuccess;
import ru.kuznetsov.qaip.core.importing.binding.ProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JsonInstanceLocation;
import ru.kuznetsov.qaip.core.importing.parsing.JsonSourcePosition;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseFinding;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseAccepted;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseRejected;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.importing.schema.ProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFinding;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationRejected;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationFailure;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationFinding;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationSeverity;
import ru.kuznetsov.qaip.core.validation.ApplicationValidationSuccess;
import ru.kuznetsov.qaip.core.validation.ProjectApplicationValidator;

import java.util.List;
import java.util.Objects;

public final class DefaultProjectImporter implements ProjectImporter {
    private static final String PROJECT_ROOT = "project";

    private final ProjectJsonParser parser;
    private final ProjectSchemaValidator schemaValidator;
    private final ProjectBinder binder;
    private final ProjectApplicationValidator applicationValidator;

    public DefaultProjectImporter(ProjectJsonParser parser, ProjectSchemaValidator schemaValidator,
                                  ProjectBinder binder, ProjectApplicationValidator applicationValidator) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator");
        this.binder = Objects.requireNonNull(binder, "binder");
        this.applicationValidator = Objects.requireNonNull(applicationValidator, "applicationValidator");
    }

    @Override
    public ProjectImportResult importProject(RawProjectJson source) {
        Objects.requireNonNull(source, "source");

        var parseResult = parser.parse(source);
        if (parseResult instanceof ProjectParseRejected rejected) {
            return failure(ProjectImportStage.PARSING,
                    rejected.findings().stream().map(DefaultProjectImporter::adapt).toList());
        }
        ProjectParseAccepted parsed = (ProjectParseAccepted) parseResult;

        var schemaResult = schemaValidator.validate(parsed.document());
        if (schemaResult instanceof SchemaValidationRejected rejected) {
            return failure(ProjectImportStage.SCHEMA_VALIDATION,
                    rejected.findings().stream().map(DefaultProjectImporter::adapt).toList());
        }
        SchemaValidationAccepted schemaValid = (SchemaValidationAccepted) schemaResult;

        var bindingResult = binder.bind(schemaValid.document());
        if (bindingResult instanceof BindingFailure failed) {
            return failure(ProjectImportStage.BINDING,
                    failed.findings().stream().map(DefaultProjectImporter::adapt).toList());
        }
        BindingSuccess bound = (BindingSuccess) bindingResult;

        var applicationResult = applicationValidator.validate(bound.document());
        if (applicationResult instanceof ApplicationValidationFailure failed) {
            return failure(ProjectImportStage.APPLICATION_VALIDATION,
                    failed.findings().stream().map(DefaultProjectImporter::adapt).toList());
        }
        ApplicationValidationSuccess valid = (ApplicationValidationSuccess) applicationResult;
        return new ProjectImportSuccess(valid.document(),
                valid.warnings().stream().map(DefaultProjectImporter::adapt).toList());
    }

    private static ProjectImportFailure failure(ProjectImportStage stage, List<ProjectImportFinding> findings) {
        return new ProjectImportFailure(stage, findings);
    }

    private static ProjectImportFinding adapt(ProjectParseFinding finding) {
        return new ProjectImportFinding(ProjectImportStage.PARSING, finding.code().name(),
                ProjectImportSeverity.ERROR, finding.message(), parsingLocation(finding));
    }

    private static String parsingLocation(ProjectParseFinding finding) {
        if (finding.sourcePosition().isPresent()) {
            JsonSourcePosition position = finding.sourcePosition().orElseThrow();
            return "line " + position.line() + ", column " + position.column()
                    + ", offset " + position.characterOffset();
        }
        return pointerLocation(finding.location());
    }

    private static ProjectImportFinding adapt(SchemaValidationFinding finding) {
        return new ProjectImportFinding(ProjectImportStage.SCHEMA_VALIDATION, finding.code().name(),
                ProjectImportSeverity.ERROR, finding.message(), pointerLocation(finding.instanceLocation()));
    }

    private static ProjectImportFinding adapt(BindingFinding finding) {
        return new ProjectImportFinding(ProjectImportStage.BINDING, finding.code(),
                ProjectImportSeverity.ERROR, finding.message(), finding.location());
    }

    private static ProjectImportFinding adapt(ApplicationValidationFinding finding) {
        return new ProjectImportFinding(ProjectImportStage.APPLICATION_VALIDATION, finding.code(),
                finding.severity() == ApplicationValidationSeverity.ERROR
                        ? ProjectImportSeverity.ERROR : ProjectImportSeverity.WARNING,
                finding.message(), finding.location());
    }

    private static String pointerLocation(JsonInstanceLocation location) {
        return location.value().isEmpty() ? PROJECT_ROOT : location.value();
    }
}
