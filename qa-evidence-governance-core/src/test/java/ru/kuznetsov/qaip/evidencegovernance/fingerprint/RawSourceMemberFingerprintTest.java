package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawSourceMemberFingerprintTest {
    @Test
    void fingerprintsEmptyBytesWithPublishedSha256Vector() {
        assertFingerprint(
                new byte[0],
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void fingerprintsAsciiBytesWithPublishedSha256Vector() {
        assertFingerprint(
                new byte[]{0x61, 0x62, 0x63},
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void fingerprintsExactUtf8MultibyteBytes() {
        byte[] utf8 = new byte[]{
                (byte) 0xd0, (byte) 0x9f, (byte) 0xd1, (byte) 0x80,
                (byte) 0xd0, (byte) 0xb8, (byte) 0xd0, (byte) 0xb2,
                (byte) 0xd0, (byte) 0xb5, (byte) 0xd1, (byte) 0x82,
                0x20, (byte) 0xe2, (byte) 0x9c, (byte) 0x93
        };

        assertFingerprint(
                utf8,
                "36d5baef14cad0cf942acc9a8221446215bbc7b033d9fd11a3a71140b66703e9");
    }

    @Test
    void fingerprintsArbitraryBinaryBytesWithoutInterpretation() {
        byte[] binary = new byte[]{
                0x00, (byte) 0xff, 0x10, 0x0a, (byte) 0x80, 0x01, (byte) 0xfe
        };

        assertFingerprint(
                binary,
                "51d88ef9eee0ba6512118142663e28680b6c91416c07cb9030e432a17374bafb");
    }

    @Test
    void newlineBytesAreNotNormalized() {
        RawSourceMemberFingerprint lf = assertFingerprint(
                "line1\nline2\n".getBytes(StandardCharsets.UTF_8),
                "2751a3a2f303ad21752038085e2b8c5f98ecff61a2e4ebbd43506a941725be80");
        RawSourceMemberFingerprint crlf = assertFingerprint(
                "line1\r\nline2\r\n".getBytes(StandardCharsets.UTF_8),
                "4ad3ef64dfb83f7a8f789bce6f30cc1f8d18491b14db4c875309b150d2a7d213");

        assertNotEquals(lf, crlf);
    }

    @Test
    void utf8BomBytesAreNotRemoved() {
        RawSourceMemberFingerprint withoutBom = RawSourceMemberFingerprint.calculate(
                new byte[]{0x61, 0x62, 0x63});
        RawSourceMemberFingerprint withBom = assertFingerprint(
                new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 0x61, 0x62, 0x63},
                "1c28dc3f1f804a1ad9c9b4b4cf5e2658d16ad4ed08e3020d04a8d2865018947c");

        assertNotEquals(withoutBom, withBom);
    }

    @Test
    void repeatedCalculationIsDeterministicAndHasValueSemantics() {
        byte[] bytes = new byte[]{5, 4, 3, 2, 1, 0};

        RawSourceMemberFingerprint first = RawSourceMemberFingerprint.calculate(bytes);
        RawSourceMemberFingerprint second = RawSourceMemberFingerprint.calculate(bytes);
        RawSourceMemberFingerprint reconstructed = new RawSourceMemberFingerprint(first.value());

        assertEquals(first, second);
        assertEquals(first, reconstructed);
        assertEquals(first.hashCode(), reconstructed.hashCode());
        assertEquals("sha-256-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);
        assertEquals(64, first.digestHex().length());
    }

    @Test
    void calculationDoesNotRetainMutableInput() {
        byte[] input = new byte[]{0x61, 0x62, 0x63};
        RawSourceMemberFingerprint fingerprint = RawSourceMemberFingerprint.calculate(input);

        input[0] = 0x7a;

        assertEquals(
                "sha-256-v1:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                fingerprint.value());
    }

    @Test
    void rejectsNullInputAndNonCanonicalValues() {
        assertThrows(NullPointerException.class, () -> RawSourceMemberFingerprint.calculate(null));
        assertThrows(NullPointerException.class, () -> new RawSourceMemberFingerprint(null));
        assertThrows(IllegalArgumentException.class,
                () -> new RawSourceMemberFingerprint("sha-256-v1:ABCDEF"));
        assertThrows(IllegalArgumentException.class,
                () -> new RawSourceMemberFingerprint("sha256:" + "0".repeat(64)));
    }

    private static RawSourceMemberFingerprint assertFingerprint(byte[] bytes, String expectedDigest) {
        RawSourceMemberFingerprint result = RawSourceMemberFingerprint.calculate(bytes);
        assertEquals("sha-256-v1:" + expectedDigest, result.value());
        assertEquals(expectedDigest, result.digestHex());
        return result;
    }
}
