package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpOperationReferenceSemanticFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final String BASE_BYTES =
            "000000000000003c51414950005343454e4152494f5f415554484f524954595f485454505f4f5045524154494f4e5f"
                    + "5245464552454e43455f53454d414e544943005631"
                    + "000000000000003c7363656e6172696f2d617574686f726974792d687474702d6f7065726174696f6e2d7265666572"
                    + "656e63652d73656d616e7469632d6331346e2d7631"
                    + "000000000000000a7368612d3235362d7631"
                    + "000000000000003c7363656e6172696f2d617574686f726974792d687474702d6f7065726174696f6e2d7265666572"
                    + "656e63652d73656d616e7469632d6331346e2d7631"
                    + "00000000000000066f7264657273"
                    + "0000000000000006435245415445"
                    + "0000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
                    + "000000000000000d4f5045524154494f4e5f524546"
                    + "0000000000000033716169702d7363656e6172696f2d6f7065726174696f6e2d7265666572656e63652d646174756d"
                    + "2d6964656e746974792d7631"
                    + "0000000000000020716169702d687474702d6f7065726174696f6e2d7265666572656e63652d7631"
                    + "0000000000000004504f5354"
                    + "000000000000000b2f6170692f6f7264657273";

    @Test
    void freezesDomainEncodingDigestRoleProfileAndValueIdentifiers() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_HTTP_OPERATION_REFERENCE_SEMANTIC\u0000V1",
                HttpOperationReferenceSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-http-operation-reference-semantic-c14n-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", HttpOperationReferenceSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("qaip-scenario-identity-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION);
        assertEquals("OPERATION_REF",
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE);
        assertEquals("qaip-scenario-operation-reference-datum-identity-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION);
        assertEquals("qaip-http-operation-reference-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE);
        assertEquals("scenario-authority-http-operation-reference-semantic-v1",
                HttpOperationReferenceSemanticFingerprint.VALUE_IDENTIFIER);
    }

    @Test
    void postOrdersGoldenVectorLocksCompleteCanonicalAndDomainBytesAndFingerprint() {
        HttpOperationReferenceSemanticFingerprintInput input = base("POST", "/api/orders");

        assertEquals(BASE_BYTES,
                HEX.formatHex(HttpOperationReferenceSemanticFingerprintEncoder.encode(input)));
        assertEquals("000000000000003c51414950005343454e4152494f5f415554484f524954595f485454505f4f5045524154494f4e5f"
                        + "5245464552454e43455f53454d414e544943005631",
                BASE_BYTES.substring(0, 136));
        assertEquals("scenario-authority-http-operation-reference-semantic-v1:"
                        + "320be195a2af272dc84eb6d9b5aceddaf8dd8005c05af34c65419deee77e03c9",
                HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void unicodeDelimiterAndNullPathGoldenVectorPreservesExactContent() {
        HttpOperationReferenceSemanticFingerprintInput input = base(
                "POST", "/api/заказы|special\u0000tail");
        String expectedBytes = BASE_BYTES.replace(
                "000000000000000b2f6170692f6f7264657273",
                "000000000000001e2f6170692fd0b7d0b0d0bad0b0d0b7d18b7c7370656369616c007461696c");

        assertEquals(expectedBytes,
                HEX.formatHex(HttpOperationReferenceSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-http-operation-reference-semantic-v1:"
                        + "efd17185b91d4d8afb966dd75e00f0702a686926c076da2ef831637d34d32fb5",
                HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void everyCanonicalIdentityContentAndVersionFieldChangesFingerprint() {
        HttpOperationReferenceSemanticFingerprintInput base = base("POST", "/api/orders");

        assertDifferent(base, input("payments", "CREATE", "qaip-scenario-identity-v1",
                "OPERATION_REF", "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v1", "POST", "/api/orders", currentVersion()));
        assertDifferent(base, input("orders", "UPDATE", "qaip-scenario-identity-v1",
                "OPERATION_REF", "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v1", "POST", "/api/orders", currentVersion()));
        assertDifferent(base, base("PUT", "/api/orders"));
        assertDifferent(base, base("POST", "/api/orders/1"));
    }

    @Test
    void v1EncoderRejectsUnsupportedFixedAndVersionValues() {
        assertUnsupported("qaip-scenario-identity-v2", "OPERATION_REF",
                "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v1", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "OPERATION_REF_V2",
                "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v1", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "OPERATION_REF",
                "qaip-scenario-operation-reference-datum-identity-v2",
                "qaip-http-operation-reference-v1", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "OPERATION_REF",
                "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v2", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "OPERATION_REF",
                "qaip-scenario-operation-reference-datum-identity-v1",
                "qaip-http-operation-reference-v1",
                "scenario-authority-http-operation-reference-semantic-c14n-v2");
    }

    @Test
    void methodAndPathAreNotSilentlyNormalized() {
        assertDifferent(base("POST", "/api/orders"), base("post", "/api/orders"));
        assertDifferent(base("POST", "/api/orders"), base("POST", "/api/orders/"));
        assertDifferent(base("POST", "/api/orders"), base("POST", " /api/orders"));
    }

    @Test
    void repeatedCalculationIsDeterministicAndReturnedBytesAreDefensive() {
        HttpOperationReferenceSemanticFingerprintInput input = base("POST", "/api/orders");
        byte[] first = HttpOperationReferenceSemanticFingerprintEncoder.encode(input);
        byte[] second = HttpOperationReferenceSemanticFingerprintEncoder.encode(input);
        assertArrayEquals(first, second);
        assertEquals(HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input),
                HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input));

        first[0] = 0x7f;
        assertArrayEquals(second, HttpOperationReferenceSemanticFingerprintEncoder.encode(input));
    }

    @Test
    void valueTypeRejectsNonLowercaseOrMalformedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new HttpOperationReferenceSemanticFingerprint(
                        HttpOperationReferenceSemanticFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new HttpOperationReferenceSemanticFingerprint("sha-256-v1:" + "a".repeat(64)));
    }

    private static HttpOperationReferenceSemanticFingerprintInput base(String method, String path) {
        return input("orders", "CREATE", "qaip-scenario-identity-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE,
                "qaip-scenario-operation-reference-datum-identity-v1",
                HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,
                method, path, currentVersion());
    }

    private static HttpOperationReferenceSemanticFingerprintInput input(
            String authority,
            String scenarioKey,
            String scenarioIdentityVersion,
            String role,
            String datumIdentityVersion,
            String targetProfile,
            String method,
            String path,
            String semanticVersion
    ) {
        return new HttpOperationReferenceSemanticFingerprintInput(authority, scenarioKey,
                scenarioIdentityVersion, role, datumIdentityVersion, targetProfile,
                method, path, semanticVersion);
    }

    private static String currentVersion() {
        return HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER;
    }

    private static void assertUnsupported(String scenarioVersion, String role,
                                          String datumVersion, String targetProfile,
                                          String semanticVersion) {
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                scenarioVersion, role, datumVersion, targetProfile,
                "POST", "/api/orders", semanticVersion));
    }

    private static void assertDifferent(
            HttpOperationReferenceSemanticFingerprintInput left,
            HttpOperationReferenceSemanticFingerprintInput right
    ) {
        assertNotEquals(HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(left),
                HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(right));
    }
}
