package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;

import java.util.Objects;

/** Single authoritative ADR-015 Scenario semantic encoder over approved leaf fingerprints. */
public final class ScenarioSemanticFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-scenario-semantic-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_SEMANTIC\u0000V1";
    public static final String SCENARIO_IDENTITY_SCHEME_VERSION = "qaip-scenario-identity-v1";
    public static final String SOURCE_NORMALIZATION_VERSION =
            "scenario-authority-source-normalization-v1";
    public static final String SCENARIO_SEMANTIC_CONTRACT_VERSION = ENCODING_IDENTIFIER;

    private ScenarioSemanticFingerprintEncoder() {
    }

    /** Encodes leaf references without decoding or reserializing their semantic content. */
    public static byte[] encode(ScenarioSemanticFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.scenarioSemanticCanonicalizationVersion())
                .writeText(input.claimedScenarioAuthority())
                .writeText(input.scenarioKey())
                .writeText(input.scenarioIdentitySchemeVersion())
                .writeText(input.exactTitle())
                .writeOrderedCollection(input.givenStepFingerprints(),
                        ScenarioSemanticFingerprintEncoder::writeStepFingerprintReference)
                .writeOrderedCollection(input.whenStepFingerprints(),
                        ScenarioSemanticFingerprintEncoder::writeStepFingerprintReference)
                .writeOrderedCollection(input.thenStepFingerprints(),
                        ScenarioSemanticFingerprintEncoder::writeStepFingerprintReference)
                .writeText(HttpOperationReferenceSemanticFingerprint.VALUE_IDENTIFIER)
                .writeText(input.operationReferenceFingerprint().value())
                .writeOrderedCollection(input.businessRuleReferenceFingerprints(),
                        ScenarioSemanticFingerprintEncoder::writeBusinessRuleFingerprintReference)
                .writeText(input.sourceNormalizationVersion())
                .writeText(input.scenarioSemanticContractVersion())
                .toByteArray();
    }

    public static ScenarioSemanticFingerprint fingerprint(ScenarioSemanticFingerprintInput input) {
        return new ScenarioSemanticFingerprint(ScenarioSemanticFingerprint.VALUE_PREFIX
                + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }

    private static void writeStepFingerprintReference(
            CanonicalBinaryWriter writer,
            StepSemanticFingerprint fingerprint
    ) {
        writer.writeText(StepSemanticFingerprint.VALUE_IDENTIFIER)
                .writeText(Objects.requireNonNull(fingerprint, "step fingerprint").value());
    }

    private static void writeBusinessRuleFingerprintReference(
            CanonicalBinaryWriter writer,
            BusinessRuleReferenceSemanticFingerprint fingerprint
    ) {
        writer.writeText(BusinessRuleReferenceSemanticFingerprint.VALUE_IDENTIFIER)
                .writeText(Objects.requireNonNull(fingerprint, "Business Rule fingerprint").value());
    }
}
