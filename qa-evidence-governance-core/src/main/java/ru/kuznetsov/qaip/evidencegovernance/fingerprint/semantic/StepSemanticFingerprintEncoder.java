package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;

import java.util.Objects;

/** Single authoritative ADR-015 Step semantic canonical encoder and fingerprinter. */
public final class StepSemanticFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER = "scenario-authority-step-semantic-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN = "QAIP\u0000SCENARIO_AUTHORITY_STEP_SEMANTIC\u0000V1";
    public static final String SCENARIO_IDENTITY_SCHEME_VERSION = "qaip-scenario-identity-v1";
    public static final String STEP_IDENTITY_SCHEME_VERSION = "qaip-scenario-step-identity-v1";

    private StepSemanticFingerprintEncoder() {
    }

    /** Encodes the exact ADR-015 Step field sequence without semantic transformation. */
    public static byte[] encode(StepSemanticFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.semanticCanonicalizationVersion())
                .writeText(input.claimedScenarioAuthority())
                .writeText(input.scenarioKey())
                .writeText(input.scenarioIdentitySchemeVersion())
                .writeText(input.phase().name())
                .writeUnsigned64(input.ordinalWithinPhase())
                .writeText(input.stepIdentitySchemeVersion())
                .writeText(input.exactDecodedAuthoredText())
                .toByteArray();
    }

    public static StepSemanticFingerprint fingerprint(StepSemanticFingerprintInput input) {
        return new StepSemanticFingerprint(StepSemanticFingerprint.VALUE_PREFIX
                + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }
}
