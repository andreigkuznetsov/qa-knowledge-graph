package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessRuleReferenceSemanticFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final String BASE_BYTES =
            "000000000000003b51414950005343454e4152494f5f415554484f524954595f425553494e4553535f52554c455f5245"
                    + "464552454e43455f53454d414e544943005631"
                    + "000000000000003b7363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e"
                    + "63652d73656d616e7469632d6331346e2d7631"
                    + "000000000000000a7368612d3235362d7631"
                    + "000000000000003b7363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e"
                    + "63652d73656d616e7469632d6331346e2d7631"
                    + "00000000000000066f7264657273"
                    + "0000000000000006435245415445"
                    + "0000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
                    + "0000000000000006706f6c696379"
                    + "000000000000000b6f726465722e6c696d6974"
                    + "000000000000001e716169702d627573696e6573732d72756c652d6964656e746974792d7631"
                    + "0000000000000037716169702d7363656e6172696f2d627573696e6573732d72756c652d7265666572656e63652d6461"
                    + "74756d2d6964656e746974792d7631"
                    + "0000000000000006706f6c696379"
                    + "000000000000000b6f726465722e6c696d6974"
                    + "000000000000001e716169702d627573696e6573732d72756c652d6964656e746974792d7631";

    @Test
    void freezesDomainEncodingDigestAndValueIdentifiers() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_BUSINESS_RULE_REFERENCE_SEMANTIC\u0000V1",
                BusinessRuleReferenceSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-business-rule-reference-semantic-c14n-v1",
                BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", BusinessRuleReferenceSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("qaip-scenario-identity-v1",
                BusinessRuleReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION);
        assertEquals("qaip-business-rule-identity-v1",
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_IDENTITY_SCHEME);
        assertEquals("qaip-scenario-business-rule-reference-datum-identity-v1",
                BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION);
        assertEquals("scenario-authority-business-rule-reference-semantic-v1",
                BusinessRuleReferenceSemanticFingerprint.VALUE_IDENTIFIER);
    }

    @Test
    void representativeRuleGoldenVectorLocksCompleteCanonicalAndDomainBytes() {
        BusinessRuleReferenceSemanticFingerprintInput input = base(
                "policy", "order.limit", "qaip-business-rule-identity-v1");

        assertEquals(BASE_BYTES,
                HEX.formatHex(BusinessRuleReferenceSemanticFingerprintEncoder.encode(input)));
        assertEquals("000000000000003b51414950005343454e4152494f5f415554484f524954595f425553494e4553535f52554c455f5245"
                        + "464552454e43455f53454d414e544943005631",
                BASE_BYTES.substring(0, 134));
        assertEquals("scenario-authority-business-rule-reference-semantic-v1:"
                        + "ef9938ae599715ac4eace9bc7dec1ee9b4595764362f751a1b7e08b7682ea889",
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void goldenVectorContainsNormativeSelfCheckingPayloadFieldsTwice() {
        assertEquals(2, occurrences(BASE_BYTES, "0000000000000006706f6c696379"));
        assertEquals(2, occurrences(BASE_BYTES, "000000000000000b6f726465722e6c696d6974"));
        assertEquals(2, occurrences(BASE_BYTES,
                "000000000000001e716169702d627573696e6573732d72756c652d6964656e746974792d7631"));
    }

    @Test
    void permittedDelimiterLikeValuesHaveNormativeGoldenVector() {
        BusinessRuleReferenceSemanticFingerprintInput input = base(
                "policy:eu", "order.limit:v1", "qaip-business-rule-identity-v1");
        String expected = BASE_BYTES
                .replace("0000000000000006706f6c696379",
                        "0000000000000009706f6c6963793a6575")
                .replace("000000000000000b6f726465722e6c696d6974",
                        "000000000000000e6f726465722e6c696d69743a7631")
                ;

        assertEquals(expected,
                HEX.formatHex(BusinessRuleReferenceSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-business-rule-reference-semantic-v1:"
                        + "37e48a97c6f00ddd2430025c58028e88bc54162397568cc74b2c6203ca13e026",
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void everyCanonicalIdentityContentAndVersionFieldChangesFingerprint() {
        BusinessRuleReferenceSemanticFingerprintInput base = base(
                "policy", "order.limit", "qaip-business-rule-identity-v1");

        assertDifferent(base, input("payments", "CREATE", "qaip-scenario-identity-v1",
                "policy", "order.limit", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion()));
        assertDifferent(base, input("orders", "UPDATE", "qaip-scenario-identity-v1",
                "policy", "order.limit", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion()));
        assertDifferent(base, base("legal", "order.limit", "qaip-business-rule-identity-v1"));
        assertDifferent(base, base("policy", "order.minimum", "qaip-business-rule-identity-v1"));
    }

    @Test
    void v1EncoderRejectsUnsupportedIdentityAndSemanticVersions() {
        assertUnsupported("qaip-scenario-identity-v2", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "custom-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v2", currentVersion());
        assertUnsupported("qaip-scenario-identity-v1", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1",
                "scenario-authority-business-rule-reference-semantic-c14n-v2");
    }

    @Test
    void sourceNativeValuesAreNotInferredOrNormalized() {
        assertDifferent(base("policy", "order.limit", "qaip-business-rule-identity-v1"),
                base("Policy", "order.limit", "qaip-business-rule-identity-v1"));
        assertDifferent(base("policy", "order.limit", "qaip-business-rule-identity-v1"),
                base("policy", "ORDER.LIMIT", "qaip-business-rule-identity-v1"));
    }

    @Test
    void authoredArrayPositionIsStructurallyExcludedFromIndividualFingerprint() {
        BusinessRuleReferenceSemanticFingerprintInput input = base(
                "policy", "order.limit", "qaip-business-rule-identity-v1");
        AuthoredRuleReference first = new AuthoredRuleReference(0, input);
        AuthoredRuleReference later = new AuthoredRuleReference(7, input);

        assertEquals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(first.input()),
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(later.input()));
        assertFalse(Arrays.stream(BusinessRuleReferenceSemanticFingerprintInput.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("authoredArrayPosition")));
    }

    @Test
    void repeatedCalculationIsDeterministicAndReturnedBytesAreDefensive() {
        BusinessRuleReferenceSemanticFingerprintInput input = base(
                "policy", "order.limit", "qaip-business-rule-identity-v1");
        byte[] first = BusinessRuleReferenceSemanticFingerprintEncoder.encode(input);
        byte[] second = BusinessRuleReferenceSemanticFingerprintEncoder.encode(input);
        assertArrayEquals(first, second);
        assertEquals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input),
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input));

        first[0] = 0x7f;
        assertArrayEquals(second, BusinessRuleReferenceSemanticFingerprintEncoder.encode(input));
    }

    @Test
    void immutableValueRejectsMalformedOrNonLowercaseFingerprint() {
        assertThrows(IllegalArgumentException.class,
                () -> new BusinessRuleReferenceSemanticFingerprint(
                        BusinessRuleReferenceSemanticFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class,
                () -> new BusinessRuleReferenceSemanticFingerprint("sha-256-v1:" + "a".repeat(64)));
    }

    private static BusinessRuleReferenceSemanticFingerprintInput base(
            String referencedAuthority,
            String stableRuleKey,
            String identityScheme
    ) {
        return input("orders", "CREATE", "qaip-scenario-identity-v1",
                referencedAuthority, stableRuleKey, identityScheme,
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion());
    }

    private static BusinessRuleReferenceSemanticFingerprintInput input(
            String scenarioAuthority,
            String scenarioKey,
            String scenarioIdentityVersion,
            String referencedAuthority,
            String stableRuleKey,
            String identityScheme,
            String datumIdentityVersion,
            String semanticVersion
    ) {
        return new BusinessRuleReferenceSemanticFingerprintInput(scenarioAuthority, scenarioKey,
                scenarioIdentityVersion, referencedAuthority, stableRuleKey, identityScheme,
                datumIdentityVersion, semanticVersion);
    }

    private static String currentVersion() {
        return BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER;
    }

    private static void assertUnsupported(String scenarioVersion, String identityScheme,
                                          String datumVersion, String semanticVersion) {
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                scenarioVersion, "policy", "order.limit", identityScheme,
                datumVersion, semanticVersion));
    }

    private static void assertDifferent(
            BusinessRuleReferenceSemanticFingerprintInput left,
            BusinessRuleReferenceSemanticFingerprintInput right
    ) {
        assertNotEquals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(left),
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(right));
    }

    private static int occurrences(String value, String sequence) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(sequence, from)) >= 0) {
            count++;
            from += sequence.length();
        }
        return count;
    }

    private record AuthoredRuleReference(
            int authoredArrayPosition,
            BusinessRuleReferenceSemanticFingerprintInput input
    ) {
    }
}
