package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepSemanticFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();

    private static final String SIMPLE_GIVEN_BYTES =
            "000000000000002851414950005343454e4152494f5f415554484f524954595f535445505f53454d414e544943005631"
                    + "00000000000000287363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d6331346e2d7631"
                    + "000000000000000a7368612d3235362d7631"
                    + "00000000000000287363656e6172696f2d617574686f726974792d737465702d73656d616e7469632d6331346e2d7631"
                    + "00000000000000066f7264657273"
                    + "0000000000000006435245415445"
                    + "0000000000000019716169702d7363656e6172696f2d6964656e746974792d7631"
                    + "0000000000000005474956454e"
                    + "0000000000000000"
                    + "000000000000001e716169702d7363656e6172696f2d737465702d6964656e746974792d7631"
                    + "00000000000000114120637573746f6d657220657869737473";

    @Test
    void freezesDomainEncodingDigestAndValueIdentifiers() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_STEP_SEMANTIC\u0000V1",
                StepSemanticFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-step-semantic-c14n-v1",
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", StepSemanticFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("qaip-scenario-identity-v1",
                StepSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION);
        assertEquals("qaip-scenario-step-identity-v1",
                StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION);
        assertEquals("scenario-authority-step-semantic-v1",
                StepSemanticFingerprint.VALUE_IDENTIFIER);
    }

    @Test
    void simpleGivenGoldenVectorLocksCompleteCanonicalBytesAndFingerprint() {
        StepSemanticFingerprintInput input = simple("A customer exists");

        assertEquals(SIMPLE_GIVEN_BYTES, HEX.formatHex(StepSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-step-semantic-v1:"
                        + "e124c1eb9794c9765a845bdcad66b45418c864dc80c336503992a82065775244",
                StepSemanticFingerprintEncoder.fingerprint(input).value());
        assertEquals("000000000000002851414950005343454e4152494f5f415554484f524954595f"
                        + "535445505f53454d414e544943005631",
                SIMPLE_GIVEN_BYTES.substring(0, 96));
    }

    @Test
    void unicodeGoldenVectorPreservesExactDecodedText() {
        StepSemanticFingerprintInput input = simple("Заказ готов 🙂");
        String expectedBytes = SIMPLE_GIVEN_BYTES
                .replace("00000000000000114120637573746f6d657220657869737473",
                        "000000000000001ad097d0b0d0bad0b0d0b720d0b3d0bed182d0bed0b220f09f9982");

        assertEquals(expectedBytes, HEX.formatHex(StepSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-step-semantic-v1:"
                        + "94e7c6cf7cf3eb9dab76b614240cd2d0b00b9014f1a51de9cda1f225c8688f3d",
                StepSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void delimiterAndUnicodeNullTextGoldenVectorRemainsUnambiguous() {
        StepSemanticFingerprintInput input = simple("left|right\u0000tail");
        String expectedBytes = SIMPLE_GIVEN_BYTES
                .replace("00000000000000114120637573746f6d657220657869737473",
                        "000000000000000f6c6566747c7269676874007461696c");

        assertEquals(expectedBytes, HEX.formatHex(StepSemanticFingerprintEncoder.encode(input)));
        assertEquals("scenario-authority-step-semantic-v1:"
                        + "d7174664c1638b0d63bb902ea34354a1b5308a09b3adf6ab1db12222d3085723",
                StepSemanticFingerprintEncoder.fingerprint(input).value());
    }

    @Test
    void everyCanonicalInputFieldChangesFingerprint() {
        StepSemanticFingerprintInput base = simple("text");

        assertDifferent(base, input("payments", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 0, "qaip-scenario-step-identity-v1",
                "text", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(base, input("orders", "UPDATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 0, "qaip-scenario-step-identity-v1",
                "text", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.WHEN, 0, "qaip-scenario-step-identity-v1",
                "text", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 1, "qaip-scenario-step-identity-v1",
                "text", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(base, input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 0, "qaip-scenario-step-identity-v1",
                "different", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    @Test
    void v1EncoderRejectsUnsupportedIdentityAndSemanticVersions() {
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                "qaip-scenario-identity-v2", StepSemanticFingerprintInput.Phase.GIVEN, 0,
                "qaip-scenario-step-identity-v1", "text", currentVersion()));
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                "qaip-scenario-identity-v1", StepSemanticFingerprintInput.Phase.GIVEN, 0,
                "qaip-scenario-step-identity-v2", "text", currentVersion()));
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                "qaip-scenario-identity-v1", StepSemanticFingerprintInput.Phase.GIVEN, 0,
                "qaip-scenario-step-identity-v1", "text",
                "scenario-authority-step-semantic-c14n-v2"));
    }

    @Test
    void phaseOrdinalAndWhitespaceDifferencesAreSemantic() {
        assertDifferent(simple("same"), input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.WHEN, 0, "qaip-scenario-step-identity-v1",
                "same", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(simple("same"), input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 1, "qaip-scenario-step-identity-v1",
                "same", StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertDifferent(simple("same"), simple(" same "));
        assertDifferent(simple("line one\nline two"), simple("line one\r\nline two"));
    }

    @Test
    void repeatedCalculationIsDeterministicAndEncodingIsDefensive() {
        StepSemanticFingerprintInput input = simple("repeat");
        byte[] first = StepSemanticFingerprintEncoder.encode(input);
        byte[] second = StepSemanticFingerprintEncoder.encode(input);
        assertArrayEquals(first, second);
        assertEquals(StepSemanticFingerprintEncoder.fingerprint(input),
                StepSemanticFingerprintEncoder.fingerprint(input));

        first[0] = 0x7f;
        assertArrayEquals(second, StepSemanticFingerprintEncoder.encode(input));
    }

    @Test
    void valueAndOrdinalContractsRejectNoncanonicalValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new StepSemanticFingerprint("scenario-authority-step-semantic-v1:"
                        + "ABCDEF0000000000000000000000000000000000000000000000000000ABCDEF"));
        assertThrows(IllegalArgumentException.class, () -> input("orders", "CREATE",
                "qaip-scenario-identity-v1", StepSemanticFingerprintInput.Phase.GIVEN, -1,
                "qaip-scenario-step-identity-v1", "text",
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertThrows(IllegalArgumentException.class, () -> new StepSemanticFingerprintInput(
                "orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, BigInteger.ONE.shiftLeft(64),
                "qaip-scenario-step-identity-v1", "text",
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static StepSemanticFingerprintInput simple(String text) {
        return input("orders", "CREATE", "qaip-scenario-identity-v1",
                StepSemanticFingerprintInput.Phase.GIVEN, 0,
                "qaip-scenario-step-identity-v1", text,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
    }

    private static String currentVersion() {
        return StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER;
    }

    private static StepSemanticFingerprintInput input(
            String authority,
            String scenarioKey,
            String scenarioIdentityVersion,
            StepSemanticFingerprintInput.Phase phase,
            long ordinal,
            String stepIdentityVersion,
            String text,
            String semanticVersion
    ) {
        return new StepSemanticFingerprintInput(authority, scenarioKey, scenarioIdentityVersion,
                phase, BigInteger.valueOf(ordinal), stepIdentityVersion, text, semanticVersion);
    }

    private static void assertDifferent(
            StepSemanticFingerprintInput left,
            StepSemanticFingerprintInput right
    ) {
        assertNotEquals(StepSemanticFingerprintEncoder.fingerprint(left),
                StepSemanticFingerprintEncoder.fingerprint(right));
    }
}
