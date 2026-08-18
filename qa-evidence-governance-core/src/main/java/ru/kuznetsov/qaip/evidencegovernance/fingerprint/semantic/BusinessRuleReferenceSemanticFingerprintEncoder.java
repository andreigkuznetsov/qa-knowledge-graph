package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;

import java.util.Objects;

/** Authoritative ADR-015 unresolved Business Rule-reference semantic encoder. */
public final class BusinessRuleReferenceSemanticFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-business-rule-reference-semantic-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_BUSINESS_RULE_REFERENCE_SEMANTIC\u0000V1";

    private BusinessRuleReferenceSemanticFingerprintEncoder() {
    }

    /** Encodes the exact ADR-015 authority-qualified reference tuple without resolution or normalization. */
    public static byte[] encode(BusinessRuleReferenceSemanticFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.claimedScenarioAuthority())
                .writeText(input.scenarioKey())
                .writeText(input.scenarioIdentitySchemeVersion())
                .writeText(input.referencedBusinessRuleAuthority())
                .writeText(input.stableRuleKey())
                .writeText(input.businessRuleIdentityScheme())
                .writeText(input.businessRuleReferenceDatumIdentityVersion())
                .writeText(input.semanticCanonicalizationVersion())
                .toByteArray();
    }

    public static BusinessRuleReferenceSemanticFingerprint fingerprint(
            BusinessRuleReferenceSemanticFingerprintInput input
    ) {
        return new BusinessRuleReferenceSemanticFingerprint(
                BusinessRuleReferenceSemanticFingerprint.VALUE_PREFIX
                        + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }
}
