package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;

/** Immutable ADR-015 Scenario semantic input containing only approved leaf fingerprint references. */
public record ScenarioSemanticFingerprintInput(
        String claimedScenarioAuthority,
        String scenarioKey,
        String scenarioIdentitySchemeVersion,
        String exactTitle,
        List<StepSemanticFingerprint> givenStepFingerprints,
        List<StepSemanticFingerprint> whenStepFingerprints,
        List<StepSemanticFingerprint> thenStepFingerprints,
        HttpOperationReferenceSemanticFingerprint operationReferenceFingerprint,
        List<BusinessRuleReferenceSemanticFingerprint> businessRuleReferenceFingerprints,
        String scenarioSemanticCanonicalizationVersion,
        String sourceNormalizationVersion,
        String scenarioSemanticContractVersion
) {
    public ScenarioSemanticFingerprintInput {
        claimedScenarioAuthority = requireNonEmpty(claimedScenarioAuthority, "claimedScenarioAuthority");
        scenarioKey = requireNonEmpty(scenarioKey, "scenarioKey");
        requireExact(scenarioIdentitySchemeVersion,
                ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentitySchemeVersion");
        exactTitle = requireNonEmpty(exactTitle, "exactTitle");
        givenStepFingerprints = immutable(givenStepFingerprints, "givenStepFingerprints");
        whenStepFingerprints = immutable(whenStepFingerprints, "whenStepFingerprints");
        thenStepFingerprints = immutable(thenStepFingerprints, "thenStepFingerprints");
        Objects.requireNonNull(operationReferenceFingerprint, "operationReferenceFingerprint");
        businessRuleReferenceFingerprints = immutable(
                businessRuleReferenceFingerprints, "businessRuleReferenceFingerprints");
        requireExact(scenarioSemanticCanonicalizationVersion,
                ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "scenarioSemanticCanonicalizationVersion");
        requireExact(sourceNormalizationVersion,
                ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                "sourceNormalizationVersion");
        requireExact(scenarioSemanticContractVersion,
                ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION,
                "scenarioSemanticContractVersion");
    }

    private static <T> List<T> immutable(List<T> values, String field) {
        return List.copyOf(Objects.requireNonNull(values, field));
    }

    private static String requireNonEmpty(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
