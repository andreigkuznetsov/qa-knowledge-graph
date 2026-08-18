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
        scenarioIdentitySchemeVersion = requireNonEmpty(
                scenarioIdentitySchemeVersion, "scenarioIdentitySchemeVersion");
        referencedBusinessRuleAuthority = requireNonEmpty(
                referencedBusinessRuleAuthority, "referencedBusinessRuleAuthority");
        stableRuleKey = requireNonEmpty(stableRuleKey, "stableRuleKey");
        businessRuleIdentityScheme = requireNonEmpty(
                businessRuleIdentityScheme, "businessRuleIdentityScheme");
        businessRuleReferenceDatumIdentityVersion = requireNonEmpty(
                businessRuleReferenceDatumIdentityVersion,
                "businessRuleReferenceDatumIdentityVersion");
        semanticCanonicalizationVersion = requireNonEmpty(
                semanticCanonicalizationVersion, "semanticCanonicalizationVersion");
    }

    private static String requireNonEmpty(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        return value;
    }
}
