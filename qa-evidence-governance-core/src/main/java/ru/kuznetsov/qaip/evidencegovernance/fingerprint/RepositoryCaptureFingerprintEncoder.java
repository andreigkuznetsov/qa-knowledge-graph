package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** The single authoritative ADR-013 repository-capture canonical encoder and fingerprinter. */
public final class RepositoryCaptureFingerprintEncoder {
    public static final String ENCODING_VERSION = "scenario-authority-repository-capture-c14n-v1";
    public static final String DIGEST_ALGORITHM = RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER;
    public static final String DOMAIN = "QAIP\u0000SCENARIO_AUTHORITY_REPOSITORY_CAPTURE\u0000V1";

    /** ADR-013 v1 contract for interpreting the fixed repository discovery anchor. */
    public static final String DISCOVERY_ANCHOR_CONTRACT_VERSION =
            "scenario-authority-repository-discovery-anchor-v1";
    /** ADR-011/ADR-013 v1 repository-relative discovery anchor. */
    public static final String EXACT_RELATIVE_DISCOVERY_ANCHOR = ".qaip/scenarios";
    /** ADR-013 v1 baseline D1/read/D2 capture-stability contract. */
    public static final String MUTATION_DETECTION_VERSION = "scenario-authority-capture-stability-v1";
    /** The only outcome for which ADR-013 v1 permits a repository content fingerprint. */
    public static final String SUCCESSFUL_STABLE_CAPTURE_OUTCOME = "STABLE_CAPTURE_COMPLETED";

    private static final String JCA_DIGEST_ALGORITHM = "SHA-256";

    private RepositoryCaptureFingerprintEncoder() {
    }

    /** Encodes the already validated input using the authoritative deterministic binary contract. */
    public static byte[] encode(RepositoryCaptureFingerprintInput input) {
        Objects.requireNonNull(input, "input must not be null");
        CanonicalWriter writer = new CanonicalWriter();
        writer.text(DOMAIN);
        writer.text(ENCODING_VERSION);
        writer.text(DIGEST_ALGORITHM);
        writer.text(input.sourceId());
        writer.text(input.sourceContractVersion());
        writer.text(input.sourceProfile());
        writer.text(input.discoveryProfileVersion());
        writer.text(DISCOVERY_ANCHOR_CONTRACT_VERSION);
        writer.text(EXACT_RELATIVE_DISCOVERY_ANCHOR);
        writer.text(input.pathNormalizationVersion());
        writer.text(input.orderingVersion());
        writer.text(input.memberByteFingerprintAlgorithm());
        writer.text(MUTATION_DETECTION_VERSION);
        writer.text(SUCCESSFUL_STABLE_CAPTURE_OUTCOME);

        writer.unsigned64(BigInteger.valueOf(input.capturedMembers().size()));
        for (RepositoryCaptureFingerprintInput.CapturedMember member : input.capturedMembers()) {
            writer.text(member.normalizedRepositoryRelativePath());
            writer.unsigned64(member.rawByteLength());
            writer.text(RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);
            writer.text(member.rawMemberFingerprint().value());
        }

        writer.unsigned64(BigInteger.valueOf(input.unsupportedMatchingEntries().size()));
        for (RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry entry
                : input.unsupportedMatchingEntries()) {
            writer.text(entry.normalizedRepositoryRelativePath());
            writer.text(entry.entryKind());
            writer.text(entry.stableDiagnosticCode());
        }
        return writer.toByteArray();
    }

    /** Calculates the canonical Repository Capture Snapshot content fingerprint. */
    public static RepositoryCaptureFingerprint fingerprint(RepositoryCaptureFingerprintInput input) {
        byte[] canonicalEncoding = encode(input);
        try {
            byte[] digest = MessageDigest.getInstance(JCA_DIGEST_ALGORITHM).digest(canonicalEncoding);
            return new RepositoryCaptureFingerprint(
                    RepositoryCaptureFingerprint.VALUE_PREFIX + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required SHA-256 digest is unavailable", exception);
        }
    }

    private static final class CanonicalWriter {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        void text(String value) {
            byte[] utf8;
            try {
                ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .encode(java.nio.CharBuffer.wrap(value));
                utf8 = new byte[encoded.remaining()];
                encoded.get(utf8);
            } catch (CharacterCodingException exception) {
                throw new IllegalArgumentException("canonical text is not strict UTF-8", exception);
            }
            unsigned64(BigInteger.valueOf(utf8.length));
            output.writeBytes(utf8);
        }

        void unsigned64(BigInteger value) {
            if (value.signum() < 0 || value.compareTo(RepositoryCaptureFingerprintInput.MAX_UNSIGNED_64) > 0) {
                throw new IllegalArgumentException("canonical integer is outside unsigned 64-bit range");
            }
            byte[] bytes = value.toByteArray();
            byte[] encoded = new byte[Long.BYTES];
            int sourceOffset = bytes.length > Long.BYTES ? bytes.length - Long.BYTES : 0;
            int copyLength = bytes.length - sourceOffset;
            System.arraycopy(bytes, sourceOffset, encoded, Long.BYTES - copyLength, copyLength);
            output.writeBytes(ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN).array());
        }

        byte[] toByteArray() {
            return output.toByteArray();
        }
    }
}
