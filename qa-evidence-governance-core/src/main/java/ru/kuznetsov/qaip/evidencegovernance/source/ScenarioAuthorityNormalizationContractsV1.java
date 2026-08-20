package ru.kuznetsov.qaip.evidencegovernance.source;

import ru.kuznetsov.qagraph.validationcore.scenarioauthority.ScenarioAuthorityManifestSchemaValidatorV1;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

/** Complete frozen contract/profile selection for Scenario Authority source normalization V1. */
public record ScenarioAuthorityNormalizationContractsV1(
        String parserContractIdentifier, String attributionContractIdentifier,
        String schemaContractIdentifier, String schemaContentIdentity,
        String diagnosticMappingContractIdentifier, String sourceNormalizationVersion,
        String manifestFormat, String manifestSchemaVersion, String manifestOccurrenceIdentityVersion,
        String scenarioDeclarationOccurrenceIdentityVersion, String scenarioIdentityScheme,
        String scenarioSemanticVersion, String stepIdentityVersion, String stepSemanticVersion,
        String operationRole, String operationTargetProfile, String operationDatumIdentityVersion,
        String operationSemanticVersion, String businessRuleDatumIdentityVersion,
        String businessRuleSemanticVersion) {
    public static final String DIAGNOSTIC_MAPPING = "scenario-authority-schema-diagnostic-mapping-v1";
    public static final String MANIFEST_FORMAT = "qaip-scenario-authority-manifest-v1";
    public static final String MANIFEST_SCHEMA_VERSION = "1.0";
    public static final String MANIFEST_OCCURRENCE_IDENTITY = "qaip-scenario-manifest-occurrence-identity-v1";
    public static final String SCENARIO_DECLARATION_OCCURRENCE_IDENTITY =
            "qaip-scenario-declaration-occurrence-identity-v1";
    public static final String STEP_IDENTITY = "qaip-scenario-step-identity-v1";
    public static final String OPERATION_ROLE = "OPERATION_REF";

    public ScenarioAuthorityNormalizationContractsV1 {
        exact(parserContractIdentifier, ScenarioAuthorityExactJsonParserV1.CONTRACT_IDENTIFIER, "parser");
        exact(attributionContractIdentifier, ScenarioAuthorityAttributorV1.CONTRACT_IDENTIFIER, "attribution");
        exact(schemaContractIdentifier, ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER, "schema");
        exact(schemaContentIdentity, ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTENT_IDENTITY, "schemaContent");
        exact(diagnosticMappingContractIdentifier, DIAGNOSTIC_MAPPING, "diagnosticMapping");
        exact(sourceNormalizationVersion, ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION, "normalization");
        exact(manifestFormat, MANIFEST_FORMAT, "manifestFormat");
        exact(manifestSchemaVersion, MANIFEST_SCHEMA_VERSION, "manifestSchemaVersion");
        exact(manifestOccurrenceIdentityVersion, MANIFEST_OCCURRENCE_IDENTITY, "manifestOccurrenceIdentity");
        exact(scenarioDeclarationOccurrenceIdentityVersion, SCENARIO_DECLARATION_OCCURRENCE_IDENTITY,
                "scenarioDeclarationOccurrenceIdentity");
        exact(scenarioIdentityScheme, ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentityScheme");
        exact(scenarioSemanticVersion, ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER, "scenarioSemantic");
        exact(stepIdentityVersion, STEP_IDENTITY, "stepIdentity");
        exact(stepSemanticVersion, StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER, "stepSemantic");
        exact(operationRole, OPERATION_ROLE, "operationRole");
        exact(operationTargetProfile, HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,
                "operationTargetProfile");
        exact(operationDatumIdentityVersion,
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION,
                "operationDatumIdentity");
        exact(operationSemanticVersion, HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "operationSemantic");
        exact(businessRuleDatumIdentityVersion,
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION,
                "businessRuleDatumIdentity");
        exact(businessRuleSemanticVersion, BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "businessRuleSemantic");
    }

    public static ScenarioAuthorityNormalizationContractsV1 selectedV1() {
        return new ScenarioAuthorityNormalizationContractsV1(
                ScenarioAuthorityExactJsonParserV1.CONTRACT_IDENTIFIER, ScenarioAuthorityAttributorV1.CONTRACT_IDENTIFIER,
                ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER,
                ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTENT_IDENTITY, DIAGNOSTIC_MAPPING,
                ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION, MANIFEST_FORMAT,
                MANIFEST_SCHEMA_VERSION, MANIFEST_OCCURRENCE_IDENTITY,
                SCENARIO_DECLARATION_OCCURRENCE_IDENTITY,
                ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER, STEP_IDENTITY,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER, OPERATION_ROLE,
                HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION,
                HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION,
                BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
    }

    private static void exact(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
