package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;

import java.math.BigInteger;
import java.util.Objects;

/** Immutable ADR-015 normalized Step semantic fingerprint input. */
public record StepSemanticFingerprintInput(
        String claimedScenarioAuthority,
        String scenarioKey,
        String scenarioIdentitySchemeVersion,
        Phase phase,
        BigInteger ordinalWithinPhase,
        String stepIdentitySchemeVersion,
        String exactDecodedAuthoredText,
        String semanticCanonicalizationVersion
) {
    public StepSemanticFingerprintInput {
        claimedScenarioAuthority = requireNonEmpty(claimedScenarioAuthority, "claimedScenarioAuthority");
        scenarioKey = requireNonEmpty(scenarioKey, "scenarioKey");
        requireExact(scenarioIdentitySchemeVersion,
                StepSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,
                "scenarioIdentitySchemeVersion");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(ordinalWithinPhase, "ordinalWithinPhase");
        if (ordinalWithinPhase.signum() < 0
                || ordinalWithinPhase.compareTo(CanonicalBinaryWriter.MAX_UNSIGNED_64) > 0) {
            throw new IllegalArgumentException("ordinalWithinPhase must be an unsigned 64-bit value");
        }
        requireExact(stepIdentitySchemeVersion,
                StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION,
                "stepIdentitySchemeVersion");
        Objects.requireNonNull(exactDecodedAuthoredText, "exactDecodedAuthoredText");
        requireExact(semanticCanonicalizationVersion,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                "semanticCanonicalizationVersion");
    }

    public enum Phase {
        GIVEN,
        WHEN,
        THEN
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
