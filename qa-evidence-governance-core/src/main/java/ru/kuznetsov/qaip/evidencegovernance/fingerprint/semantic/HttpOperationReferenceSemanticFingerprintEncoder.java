package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;

import java.util.Objects;

/** Authoritative ADR-015 unresolved HTTP Operation-reference semantic encoder. */
public final class HttpOperationReferenceSemanticFingerprintEncoder {
    public static final String ENCODING_IDENTIFIER =
            "scenario-authority-http-operation-reference-semantic-c14n-v1";
    public static final String DIGEST_IDENTIFIER = CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN =
            "QAIP\u0000SCENARIO_AUTHORITY_HTTP_OPERATION_REFERENCE_SEMANTIC\u0000V1";
    public static final String OPERATION_REFERENCE_ROLE = "OPERATION_REF";
    public static final String TARGET_PROFILE = "qaip-http-operation-reference-v1";

    private HttpOperationReferenceSemanticFingerprintEncoder() {
    }

    /** Encodes the exact ADR-015 source-native field sequence without resolving or transforming it. */
    public static byte[] encode(HttpOperationReferenceSemanticFingerprintInput input) {
        Objects.requireNonNull(input, "input");
        return new CanonicalBinaryWriter()
                .writeDomain(DOMAIN)
                .writeText(ENCODING_IDENTIFIER)
                .writeText(DIGEST_IDENTIFIER)
                .writeText(input.claimedScenarioAuthority())
                .writeText(input.scenarioKey())
                .writeText(input.scenarioIdentitySchemeVersion())
                .writeText(input.role())
                .writeText(input.operationReferenceDatumIdentityVersion())
                .writeText(input.targetProfile())
                .writeText(input.exactAdmittedMethod())
                .writeText(input.exactAdmittedPath())
                .writeText(input.semanticCanonicalizationVersion())
                .toByteArray();
    }

    public static HttpOperationReferenceSemanticFingerprint fingerprint(
            HttpOperationReferenceSemanticFingerprintInput input
    ) {
        return new HttpOperationReferenceSemanticFingerprint(
                HttpOperationReferenceSemanticFingerprint.VALUE_PREFIX
                        + CanonicalSha256.lowercaseHexDigest(encode(input)));
    }
}
