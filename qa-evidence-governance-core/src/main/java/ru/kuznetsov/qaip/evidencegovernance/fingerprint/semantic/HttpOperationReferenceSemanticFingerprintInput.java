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
        requireExact(scenarioIdentitySchemeVersion,
                HttpOperationReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentitySchemeVersion");
        requireExact(role, HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE, "role");
        requireExact(operationReferenceDatumIdentityVersion,
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION,
                "operationReferenceDatumIdentityVersion");
        requireExact(targetProfile, HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,
                "targetProfile");
        exactAdmittedMethod = requireNonEmpty(exactAdmittedMethod, "exactAdmittedMethod");
        exactAdmittedPath = requireNonEmpty(exactAdmittedPath, "exactAdmittedPath");
        requireExact(semanticCanonicalizationVersion,
                HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
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
