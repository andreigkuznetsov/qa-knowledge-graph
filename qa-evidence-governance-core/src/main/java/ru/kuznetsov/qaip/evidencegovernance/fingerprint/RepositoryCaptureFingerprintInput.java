package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable semantic input to the ADR-013 repository-capture encoder. */
public record RepositoryCaptureFingerprintInput(
        String sourceId,
        String sourceContractVersion,
        String sourceProfile,
        String discoveryProfileVersion,
        String pathNormalizationVersion,
        String orderingVersion,
        String memberByteFingerprintAlgorithm,
        List<CapturedMember> capturedMembers,
        List<UnsupportedMatchingEntry> unsupportedMatchingEntries
) {
    public static final BigInteger MAX_UNSIGNED_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    private static final Pattern STABLE_IDENTIFIER = Pattern.compile("[A-Z][A-Z0-9_]*");

    public RepositoryCaptureFingerprintInput {
        sourceId = requireText(sourceId, "sourceId");
        sourceContractVersion = requireText(sourceContractVersion, "sourceContractVersion");
        sourceProfile = requireText(sourceProfile, "sourceProfile");
        discoveryProfileVersion = requireText(discoveryProfileVersion, "discoveryProfileVersion");
        pathNormalizationVersion = requireText(pathNormalizationVersion, "pathNormalizationVersion");
        orderingVersion = requireText(orderingVersion, "orderingVersion");
        memberByteFingerprintAlgorithm = requireText(
                memberByteFingerprintAlgorithm, "memberByteFingerprintAlgorithm");
        if (!RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER.equals(memberByteFingerprintAlgorithm)) {
            throw new IllegalArgumentException("unsupported memberByteFingerprintAlgorithm: "
                    + memberByteFingerprintAlgorithm);
        }

        capturedMembers = List.copyOf(Objects.requireNonNull(capturedMembers, "capturedMembers"));
        unsupportedMatchingEntries = List.copyOf(Objects.requireNonNull(
                unsupportedMatchingEntries, "unsupportedMatchingEntries"));
        requireCanonicalMemberOrder(capturedMembers);
        requireCanonicalUnsupportedOrder(unsupportedMatchingEntries);
        requireDisjointPaths(capturedMembers, unsupportedMatchingEntries);
    }

    /** One regular member whose bytes have already been captured and fingerprinted. */
    public record CapturedMember(
            String normalizedRepositoryRelativePath,
            BigInteger rawByteLength,
            RawSourceMemberFingerprint rawMemberFingerprint
    ) {
        public CapturedMember {
            normalizedRepositoryRelativePath = requireCanonicalPath(
                    normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
            rawByteLength = requireUnsigned64(rawByteLength, "rawByteLength");
            Objects.requireNonNull(rawMemberFingerprint, "rawMemberFingerprint");
        }

        public CapturedMember(
                String normalizedRepositoryRelativePath,
                long rawByteLength,
                RawSourceMemberFingerprint rawMemberFingerprint
        ) {
            this(normalizedRepositoryRelativePath, BigInteger.valueOf(rawByteLength), rawMemberFingerprint);
        }
    }

    /** One matching path whose stable entry kind is unsupported by the discovery profile. */
    public record UnsupportedMatchingEntry(
            String normalizedRepositoryRelativePath,
            String entryKind,
            String stableDiagnosticCode
    ) {
        public UnsupportedMatchingEntry {
            normalizedRepositoryRelativePath = requireCanonicalPath(
                    normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
            entryKind = requireStableIdentifier(entryKind, "entryKind");
            stableDiagnosticCode = requireStableIdentifier(stableDiagnosticCode, "stableDiagnosticCode");
        }
    }

    private static void requireCanonicalMemberOrder(List<CapturedMember> members) {
        for (int index = 1; index < members.size(); index++) {
            String previous = members.get(index - 1).normalizedRepositoryRelativePath();
            String current = members.get(index).normalizedRepositoryRelativePath();
            if (compareCodePoints(previous, current) >= 0) {
                throw new IllegalArgumentException(
                        "capturedMembers must have unique paths in exact Unicode code-point order");
            }
        }
    }

    private static void requireCanonicalUnsupportedOrder(List<UnsupportedMatchingEntry> entries) {
        for (int index = 1; index < entries.size(); index++) {
            UnsupportedMatchingEntry previous = entries.get(index - 1);
            UnsupportedMatchingEntry current = entries.get(index);
            int comparison = compareCodePoints(
                    previous.normalizedRepositoryRelativePath(), current.normalizedRepositoryRelativePath());
            if (comparison == 0) comparison = previous.entryKind().compareTo(current.entryKind());
            if (comparison == 0) {
                comparison = previous.stableDiagnosticCode().compareTo(current.stableDiagnosticCode());
            }
            if (comparison >= 0) {
                throw new IllegalArgumentException(
                        "unsupportedMatchingEntries must be unique and canonically ordered");
            }
        }
    }

    private static void requireDisjointPaths(
            List<CapturedMember> members,
            List<UnsupportedMatchingEntry> unsupported
    ) {
        Set<String> memberPaths = new HashSet<>();
        members.forEach(member -> memberPaths.add(member.normalizedRepositoryRelativePath()));
        if (unsupported.stream().map(UnsupportedMatchingEntry::normalizedRepositoryRelativePath)
                .anyMatch(memberPaths::contains)) {
            throw new IllegalArgumentException(
                    "one path cannot be both a captured member and an unsupported matching entry");
        }
    }

    static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        strictUtf8(value, field);
        return value;
    }

    private static String requireCanonicalPath(String value, String field) {
        requireText(value, field);
        if (value.startsWith("/") || value.endsWith("/") || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(field + " must be a normalized repository-relative '/' path");
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(field + " contains a noncanonical path segment");
            }
        }
        return value;
    }

    private static String requireStableIdentifier(String value, String field) {
        requireText(value, field);
        if (!STABLE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be an uppercase stable identifier");
        }
        return value;
    }

    private static BigInteger requireUnsigned64(BigInteger value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.signum() < 0 || value.compareTo(MAX_UNSIGNED_64) > 0) {
            throw new IllegalArgumentException(field + " must be in unsigned 64-bit range");
        }
        return value;
    }

    private static void strictUtf8(String value, String field) {
        try {
            StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(field + " must be valid Unicode encodable as strict UTF-8", exception);
        }
    }

    static int compareCodePoints(String left, String right) {
        var leftPoints = left.codePoints().iterator();
        var rightPoints = right.codePoints().iterator();
        while (leftPoints.hasNext() && rightPoints.hasNext()) {
            int comparison = Integer.compare(leftPoints.nextInt(), rightPoints.nextInt());
            if (comparison != 0) return comparison;
        }
        return Boolean.compare(leftPoints.hasNext(), rightPoints.hasNext());
    }
}
