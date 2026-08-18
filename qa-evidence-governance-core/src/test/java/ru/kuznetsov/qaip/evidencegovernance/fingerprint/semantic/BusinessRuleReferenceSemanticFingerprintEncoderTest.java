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
                    + "00000000000000066f7264657273"
                    + "0000000000000006435245415445"
                    + "0000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
                    + "0000000000000006706f6c696379"
                    + "000000000000000b6f726465722e6c696d6974"
                    + "000000000000001e716169702d627573696e6573732d72756c652d6964656e746974792d7631"
                    + "0000000000000037716169702d7363656e6172696f2d627573696e6573732d72756c652d7265666572656e63652d6461"
                    + "74756d2d6964656e746974792d7631"
                    + "000000000000003b7363656e6172696f2d617574686f726974792d627573696e6573732d72756c652d7265666572656e"
                    + "63652d73656d616e7469632d6331346e2d7631";

    @Test
    void freezesDomainEncodingDigestAndValueIdentifiers() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_BUSINESS_RULE_REFERENCE_SEMANTIC\u0000V1",
                BusinessRuleReferenceSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-business-rule-reference-semantic-c14n-v1",
                BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", BusinessRuleReferenceSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
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
                        + "5e9ca06e01fad46230823db6c0f0dde06557befdde89c1a07ceb5d3fcadb19be",
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void permittedDelimiterLikeValuesHaveNormativeGoldenVector() {
        BusinessRuleReferenceSemanticFingerprintInput input = base(
                "policy:eu", "order.limit:v1", "qaip-business-rule:identity-v1");
        String expected = BASE_BYTES
                .replace("0000000000000006706f6c696379",
                        "0000000000000009706f6c6963793a6575")
                .replace("000000000000000b6f726465722e6c696d6974",
                        "000000000000000e6f726465722e6c696d69743a7631")
                .replace("000000000000001e716169702d627573696e6573732d72756c652d6964656e746974792d7631",
                        "000000000000001e716169702d627573696e6573732d72756c653a6964656e746974792d7631");

        assertEquals(expected,
                HEX.formatHex(BusinessRuleReferenceSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-business-rule-reference-semantic-v1:"
                        + "61046bdb4e854ffa461300495c328d078eaa8972b7bee0d7399240bfd9053da7",
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
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v2",
                "policy", "order.limit", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1", currentVersion()));
        assertDifferent(base, base("legal", "order.limit", "qaip-business-rule-identity-v1"));
        assertDifferent(base, base("policy", "order.minimum", "qaip-business-rule-identity-v1"));
        assertDifferent(base, base("policy", "order.limit", "custom-rule-identity-v1"));
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v1",
                "policy", "order.limit", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v2", currentVersion()));
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v1",
                "policy", "order.limit", "qaip-business-rule-identity-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1",
                "scenario-authority-business-rule-reference-semantic-c14n-v2"));
    }

    @Test
    void sourceNativeValuesAreNotInferredOrNormalized() {
        assertDifferent(base("policy", "order.limit", "qaip-business-rule-identity-v1"),
                base("Policy", "order.limit", "qaip-business-rule-identity-v1"));
        assertDifferent(base("policy", "order.limit", "qaip-business-rule-identity-v1"),
                base("policy", "ORDER.LIMIT", "qaip-business-rule-identity-v1"));
        assertDifferent(base("policy", "order.limit", "qaip-business-rule-identity-v1"),
                base("policy", "order.limit", "QAIP-BUSINESS-RULE-IDENTITY-V1"));
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

    private static void assertDifferent(
            BusinessRuleReferenceSemanticFingerprintInput left,
            BusinessRuleReferenceSemanticFingerprintInput right
    ) {
        assertNotEquals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(left),
                BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(right));
    }

    private record AuthoredRuleReference(
            int authoredArrayPosition,
            BusinessRuleReferenceSemanticFingerprintInput input
    ) {
    }
}
