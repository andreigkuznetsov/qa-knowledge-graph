package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable ADR-013 fingerprint of one exact captured source-member byte sequence.
 *
 * <p>The authoritative {@link #calculate(byte[])} operation hashes a defensive copy
 * of the supplied bytes directly. It performs no decoding, normalization, BOM
 * handling, parsing, or canonicalization.</p>
 */
public record RawSourceMemberFingerprint(String value) {
    public static final String ALGORITHM_IDENTIFIER = "sha-256-v1";
    public static final String VALUE_PREFIX = ALGORITHM_IDENTIFIER + ":";

    private static final String JCA_ALGORITHM = "SHA-256";
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "^" + ALGORITHM_IDENTIFIER + ":[0-9a-f]{64}$");

    public RawSourceMemberFingerprint {
        Objects.requireNonNull(value, "value must not be null");
        if (!VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "value must be sha-256-v1 followed by 64 lowercase hexadecimal characters");
        }
    }

    /**
     * Calculates {@code sha-256-v1:<lowercase SHA-256 hex>} over the exact input bytes.
     *
     * @param exactBytes exact captured source bytes; never interpreted as text
     * @return immutable raw source-member fingerprint
     */
    public static RawSourceMemberFingerprint calculate(byte[] exactBytes) {
        Objects.requireNonNull(exactBytes, "exactBytes must not be null");
        byte[] defensiveCopy = exactBytes.clone();
        try {
            byte[] digest = MessageDigest.getInstance(JCA_ALGORITHM).digest(defensiveCopy);
            return new RawSourceMemberFingerprint(VALUE_PREFIX + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required SHA-256 digest is unavailable", exception);
        }
    }

    /** Returns the lowercase hexadecimal digest without its algorithm prefix. */
    public String digestHex() {
        return value.substring(VALUE_PREFIX.length());
    }
}
