package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Immutable ADR-015 normalized unresolved HTTP Operation-reference fingerprint input. */
public record HttpOperationReferenceSemanticFingerprintInput(
        String claimedScenarioAuthority,
        String scenarioKey,
        String scenarioIdentitySchemeVersion,
        String role,
        String operationReferenceDatumIdentityVersion,
        String targetProfile,
        String exactAdmittedMethod,
        String exactAdmittedPath,
        String semanticCanonicalizationVersion
) {
    public HttpOperationReferenceSemanticFingerprintInput {
        claimedScenarioAuthority = requireNonEmpty(claimedScenarioAuthority, "claimedScenarioAuthority");
        scenarioKey = requireNonEmpty(scenarioKey, "scenarioKey");
        scenarioIdentitySchemeVersion = requireNonEmpty(
                scenarioIdentitySchemeVersion, "scenarioIdentitySchemeVersion");
        role = requireNonEmpty(role, "role");
        operationReferenceDatumIdentityVersion = requireNonEmpty(
                operationReferenceDatumIdentityVersion, "operationReferenceDatumIdentityVersion");
        targetProfile = requireNonEmpty(targetProfile, "targetProfile");
        exactAdmittedMethod = requireNonEmpty(exactAdmittedMethod, "exactAdmittedMethod");
        exactAdmittedPath = requireNonEmpty(exactAdmittedPath, "exactAdmittedPath");
        semanticCanonicalizationVersion = requireNonEmpty(
                semanticCanonicalizationVersion, "semanticCanonicalizationVersion");
    }

    private static String requireNonEmpty(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        return value;
    }
}
