package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Immutable ADR-015 normalized unresolved Business Rule-reference fingerprint input. */
public record BusinessRuleReferenceSemanticFingerprintInput(
        String claimedScenarioAuthority,
        String scenarioKey,
        String scenarioIdentitySchemeVersion,
        String referencedBusinessRuleAuthority,
        String stableRuleKey,
        String businessRuleIdentityScheme,
        String businessRuleReferenceDatumIdentityVersion,
        String semanticCanonicalizationVersion
) {
    public BusinessRuleReferenceSemanticFingerprintInput {
        claimedScenarioAuthority = requireNonEmpty(claimedScenarioAuthority, "claimedScenarioAuthority");
        scenarioKey = requireNonEmpty(scenarioKey, "scenarioKey");
        requireExact(scenarioIdentitySchemeVersion,
                BusinessRuleReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentitySchemeVersion");
        referencedBusinessRuleAuthority = requireNonEmpty(
                referencedBusinessRuleAuthority, "referencedBusinessRuleAuthority");
        stableRuleKey = requireNonEmpty(stableRuleKey, "stableRuleKey");
        requireExact(businessRuleIdentityScheme,
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_IDENTITY_SCHEME,
                "businessRuleIdentityScheme");
        requireExact(businessRuleReferenceDatumIdentityVersion,
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION,
                "businessRuleReferenceDatumIdentityVersion");
        requireExact(semanticCanonicalizationVersion,
                BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "semanticCanonicalizationVersion");
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
