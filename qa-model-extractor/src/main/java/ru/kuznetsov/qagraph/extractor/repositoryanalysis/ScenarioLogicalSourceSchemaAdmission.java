package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Applies the frozen Scenario Manifest v1 schema independently to attributed members only. */
public final class ScenarioLogicalSourceSchemaAdmission {
    public static final String SCHEMA_CONTRACT_IDENTIFIER =
            ScenarioManifestSchemaValidator.SCHEMA_CONTRACT_IDENTIFIER;

    private final SchemaValidationStep schemaValidation;

    public ScenarioLogicalSourceSchemaAdmission() {
        ScenarioManifestSchemaValidator validator = new ScenarioManifestSchemaValidator();
        this.schemaValidation = validator::validateDocument;
    }

    ScenarioLogicalSourceSchemaAdmission(SchemaValidationStep schemaValidation) {
        this.schemaValidation = Objects.requireNonNull(schemaValidation, "schemaValidation");
    }

    public ScenarioSchemaAdmissionResult admit(ScenarioLogicalSourceProcessingResult processingResult) {
        Objects.requireNonNull(processingResult, "processingResult");
        List<ScenarioSchemaAdmissionOutcome> admitted = new ArrayList<>();
        for (ScenarioMemberProcessingOutcome outcome : processingResult.memberOutcomes()) {
            if (outcome instanceof UnattributableMemberProcessingOutcome unattributable) {
                admitted.add(unattributable);
            } else {
                admitted.add(admitAttributed((AttributedMemberProcessingOutcome) outcome));
            }
        }
        return new ScenarioSchemaAdmissionResult(processingResult, admitted);
    }

    private AttributedMemberSchemaAdmissionOutcome admitAttributed(
            AttributedMemberProcessingOutcome attributed
    ) {
        List<ScenarioSchemaDiagnostic> diagnostics = schemaValidation.validate(attributed.parsedSource()).stream()
                .map(value -> new ScenarioSchemaDiagnostic(
                        ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION,
                        value.instanceLocation(),
                        value.keyword(),
                        value.machineStableDetail(),
                        value.humanMessage()))
                .toList();
        AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState state = diagnostics.isEmpty()
                ? AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED
                : AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED;
        return new AttributedMemberSchemaAdmissionOutcome(
                attributed.parentMemberRef(),
                attributed.claimedAuthority(),
                attributed.parserContractIdentifier(),
                attributed.attributionContractIdentifier(),
                attributed.parseOutcome(),
                attributed.attributionOutcome(),
                attributed.structuralLocation(),
                SCHEMA_CONTRACT_IDENTIFIER,
                state,
                diagnostics,
                attributed.parsedSource());
    }

    @FunctionalInterface
    interface SchemaValidationStep {
        List<ScenarioManifestSchemaValidator.SchemaDiagnosticData> validate(JsonNode document);
    }
}
