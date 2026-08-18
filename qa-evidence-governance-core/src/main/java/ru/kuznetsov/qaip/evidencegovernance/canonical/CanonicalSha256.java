package ru.kuznetsov.qaip.evidencegovernance.canonical;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Authoritative SHA-256 and lowercase-hex mechanics shared by ADR-015 semantic domains. */
public final class CanonicalSha256 {
    public static final String ALGORITHM_IDENTIFIER = "sha-256-v1";
    private static final String JCA_ALGORITHM = "SHA-256";

    private CanonicalSha256() {
    }

    /** Calculates SHA-256 over the exact supplied bytes and returns a new 32-byte array. */
    public static byte[] digest(byte[] canonicalBytes) {
        Objects.requireNonNull(canonicalBytes, "canonicalBytes");
        try {
            return MessageDigest.getInstance(JCA_ALGORITHM).digest(canonicalBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required SHA-256 digest is unavailable", exception);
        }
    }

    /** Calculates SHA-256 and returns exactly 64 lowercase hexadecimal digits. */
    public static String lowercaseHexDigest(byte[] canonicalBytes) {
        return HexFormat.of().formatHex(digest(canonicalBytes));
    }
}
