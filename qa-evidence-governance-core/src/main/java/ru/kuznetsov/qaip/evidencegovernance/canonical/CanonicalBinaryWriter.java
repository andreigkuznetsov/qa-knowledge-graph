package ru.kuznetsov.qaip.evidencegovernance.canonical;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Authoritative ADR-015 canonical binary mechanics.
 * Domain encoders, not this writer, own semantic field selection and order.
 */
public final class CanonicalBinaryWriter {
    public static final BigInteger MAX_UNSIGNED_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    public static final byte OPTIONAL_ABSENT = 0x00;
    public static final byte OPTIONAL_PRESENT = 0x01;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    /** Encodes a canonical domain containing actual U+0000 separators, never escape notation. */
    public CanonicalBinaryWriter writeDomain(String domain) {
        Objects.requireNonNull(domain, "domain");
        if (domain.indexOf('\u0000') < 0) {
            throw new IllegalArgumentException("canonical domain must contain a U+0000 separator");
        }
        if (domain.contains("\\0") || domain.contains("\\u0000")) {
            throw new IllegalArgumentException("canonical domain must contain U+0000, not escape notation");
        }
        return writeText(domain);
    }

    /** Encodes strict UTF-8 text as unsigned length followed by exact UTF-8 bytes. */
    public CanonicalBinaryWriter writeText(String value) {
        Objects.requireNonNull(value, "value");
        return writeLengthPrefixedBytes(strictUtf8(value));
    }

    /** Encodes exact bytes as unsigned length followed by the bytes, preserving caller content. */
    public CanonicalBinaryWriter writeLengthPrefixedBytes(byte[] value) {
        Objects.requireNonNull(value, "value");
        writeUnsigned64(BigInteger.valueOf(value.length));
        output.writeBytes(value);
        return this;
    }

    /** Encodes one unsigned 64-bit integer in exactly eight big-endian bytes. */
    public CanonicalBinaryWriter writeUnsigned64(BigInteger value) {
        Objects.requireNonNull(value, "value");
        if (value.signum() < 0 || value.compareTo(MAX_UNSIGNED_64) > 0) {
            throw new IllegalArgumentException("value is outside the unsigned 64-bit range");
        }
        byte[] source = value.toByteArray();
        byte[] encoded = new byte[Long.BYTES];
        int sourceOffset = Math.max(0, source.length - Long.BYTES);
        int copyLength = source.length - sourceOffset;
        System.arraycopy(source, sourceOffset, encoded, Long.BYTES - copyLength, copyLength);
        output.writeBytes(encoded);
        return this;
    }

    /** Writes the explicit absent tag. Absence is never inferred from null or an empty value. */
    public CanonicalBinaryWriter writeAbsent() {
        output.write(OPTIONAL_ABSENT);
        return this;
    }

    /** Writes the explicit present tag and the caller-supplied canonical value encoding. */
    public CanonicalBinaryWriter writePresent(Consumer<CanonicalBinaryWriter> valueEncoder) {
        Objects.requireNonNull(valueEncoder, "valueEncoder");
        CanonicalBinaryWriter nested = new CanonicalBinaryWriter();
        valueEncoder.accept(nested);
        output.write(OPTIONAL_PRESENT);
        output.writeBytes(nested.toByteArray());
        return this;
    }

    /** Writes the approved explicit optional tag followed by the canonical present value. */
    public <T> CanonicalBinaryWriter writeOptional(
            Optional<? extends T> value,
            BiConsumer<CanonicalBinaryWriter, T> valueEncoder
    ) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(valueEncoder, "valueEncoder");
        if (value.isEmpty()) return writeAbsent();
        return writePresent(writer -> valueEncoder.accept(writer, value.get()));
    }

    /** Writes the list count and elements in exact caller order; this method never sorts. */
    public <T> CanonicalBinaryWriter writeOrderedCollection(
            List<? extends T> values,
            BiConsumer<CanonicalBinaryWriter, T> elementEncoder
    ) {
        List<? extends T> snapshot = List.copyOf(Objects.requireNonNull(values, "values"));
        Objects.requireNonNull(elementEncoder, "elementEncoder");
        CanonicalBinaryWriter nested = new CanonicalBinaryWriter();
        for (T value : snapshot) {
            elementEncoder.accept(nested, value);
        }
        writeUnsigned64(BigInteger.valueOf(snapshot.size()));
        output.writeBytes(nested.toByteArray());
        return this;
    }

    /** Returns a defensive copy of the bytes written so far. */
    public byte[] toByteArray() {
        return output.toByteArray();
    }

    private static byte[] strictUtf8(String value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("canonical text contains invalid UTF-16", exception);
        }
    }
}
