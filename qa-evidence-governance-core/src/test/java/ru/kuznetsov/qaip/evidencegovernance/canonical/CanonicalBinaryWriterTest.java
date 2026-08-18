package ru.kuznetsov.qaip.evidencegovernance.canonical;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalBinaryWriterTest {
    private static final HexFormat HEX = HexFormat.of();

    @Test
    void domainUsesOneZeroByteForEachActualUnicodeNullSeparator() {
        byte[] encoded = new CanonicalBinaryWriter()
                .writeDomain("QAIP\u0000X\u0000V1")
                .toByteArray();

        assertHex("0000000000000009514149500058005631", encoded);
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalBinaryWriter().writeDomain("QAIP\\0X\\0V1"));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalBinaryWriter().writeDomain("QAIP\\u0000X\\u0000V1"));
    }

    @Test
    void textUsesStrictUtf8WithUnsignedLengthPrefix() {
        assertHex("000000000000000141", new CanonicalBinaryWriter().writeText("A").toByteArray());
        assertHex("0000000000000006c3a9f09f9982",
                new CanonicalBinaryWriter().writeText("é🙂").toByteArray());
        assertFalse(java.util.Arrays.equals(
                new CanonicalBinaryWriter().writeText("é").toByteArray(),
                new CanonicalBinaryWriter().writeText("e\u0301").toByteArray()));
    }

    @Test
    void invalidUtf16SurrogatesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalBinaryWriter().writeText("\uD800"));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalBinaryWriter().writeText("\uDC00"));
    }

    @Test
    void unsigned64EncodesZeroMaximumAndBigEndianValues() {
        assertHex("0000000000000000", new CanonicalBinaryWriter()
                .writeUnsigned64(BigInteger.ZERO).toByteArray());
        assertHex("ffffffffffffffff", new CanonicalBinaryWriter()
                .writeUnsigned64(CanonicalBinaryWriter.MAX_UNSIGNED_64).toByteArray());
        assertHex("0102030405060708", new CanonicalBinaryWriter()
                .writeUnsigned64(new BigInteger("0102030405060708", 16)).toByteArray());
    }

    @Test
    void unsigned64RejectsNegativeAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBinaryWriter()
                .writeUnsigned64(BigInteger.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalBinaryWriter()
                .writeUnsigned64(BigInteger.ONE.shiftLeft(64)));
    }

    @Test
    void bytesAreLengthPrefixedAndDefensivelyConsumed() {
        assertHex("0000000000000000", new CanonicalBinaryWriter()
                .writeLengthPrefixedBytes(new byte[0]).toByteArray());
        byte[] mutable = {(byte) 0x00, (byte) 0xff};
        CanonicalBinaryWriter writer = new CanonicalBinaryWriter().writeLengthPrefixedBytes(mutable);
        mutable[0] = 0x7f;

        assertHex("000000000000000200ff", writer.toByteArray());
    }

    @Test
    void absentAndPresentEmptyCannotCollapse() {
        byte[] absent = new CanonicalBinaryWriter().writeAbsent().toByteArray();
        byte[] presentEmpty = new CanonicalBinaryWriter()
                .writePresent(value -> value.writeText(""))
                .toByteArray();
        assertHex("00", absent);
        assertHex("010000000000000000", presentEmpty);
        assertFalse(java.util.Arrays.equals(absent, presentEmpty));
        assertHex("010000000000000000", new CanonicalBinaryWriter()
                .writePresent(value -> value.writeOrderedCollection(List.<String>of(),
                        (nested, item) -> nested.writeText(item)))
                .toByteArray());
        assertHex("010000000000000000", new CanonicalBinaryWriter()
                .writePresent(value -> value.writeUnsigned64(BigInteger.ZERO))
                .toByteArray());
    }

    @Test
    void orderedCollectionPreservesCallerOrderAndDoesNotSort() {
        byte[] encoded = new CanonicalBinaryWriter()
                .writeOrderedCollection(List.of("b", "a"),
                        (writer, value) -> writer.writeText(value))
                .toByteArray();

        assertHex("0000000000000002000000000000000162000000000000000161", encoded);
    }

    @Test
    void repeatedEncodingIsDeterministicAndOutputIsDefensive() {
        byte[] first = sampleEncoding();
        byte[] second = sampleEncoding();
        assertArrayEquals(first, second);

        CanonicalBinaryWriter writer = new CanonicalBinaryWriter().writeText("stable");
        byte[] exposed = writer.toByteArray();
        exposed[0] = 0x7f;
        assertHex("0000000000000006737461626c65", writer.toByteArray());
    }

    @Test
    void sha256UsesLowercaseHexadecimalAndExactBytes() {
        assertEquals("sha-256-v1", CanonicalSha256.ALGORITHM_IDENTIFIER);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb924"
                        + "27ae41e4649b934ca495991b7852b855",
                CanonicalSha256.lowercaseHexDigest(new byte[0]));
        assertEquals(32, CanonicalSha256.digest(new byte[0]).length);
    }

    private static byte[] sampleEncoding() {
        return new CanonicalBinaryWriter()
                .writeDomain("QAIP\u0000TEST\u0000V1")
                .writeText("value")
                .writeOrderedCollection(List.of("one", "two"),
                        (writer, value) -> writer.writeText(value))
                .writeAbsent()
                .toByteArray();
    }

    private static void assertHex(String expected, byte[] actual) {
        assertEquals(expected, HEX.formatHex(actual));
    }
}
