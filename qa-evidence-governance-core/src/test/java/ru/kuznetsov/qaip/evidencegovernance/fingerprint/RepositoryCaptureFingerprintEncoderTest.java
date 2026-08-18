package ru.kuznetsov.qaip.evidencegovernance.fingerprint;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryCaptureFingerprintEncoderTest {
    private static final String SOURCE_ID = "repository:orders";
    private static final String SOURCE_PROFILE = "qaip-scenario-authority-repository-json-v1";
    private static final String DISCOVERY_PROFILE = "scenario-authority-repository-discovery-v1";
    private static final String ANCHOR_INTERPRETATION = "scenario-authority-anchor-v1";
    private static final String PATH_NORMALIZATION = "scenario-authority-repository-path-v1";
    private static final String ORDERING = "unicode-code-point-order-v1";

    @Test
    void emptyMembershipHasStableGoldenFingerprintAndEncoding() {
        RepositoryCaptureFingerprintInput input = input(List.of(), List.of());

        RepositoryCaptureFingerprint result = RepositoryCaptureFingerprintEncoder.fingerprint(input);
        byte[] firstEncoding = RepositoryCaptureFingerprintEncoder.encode(input);
        byte[] secondEncoding = RepositoryCaptureFingerprintEncoder.encode(input);

        assertEquals(
                "scenario-authority-repository-capture-v1:"
                        + "07c05089409817dbac683e71a2f073e88be42f4ad61848965072a9dc72b97b17",
                result.value());
        assertArrayEquals(firstEncoding, secondEncoding);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 45}, Arrays.copyOf(firstEncoding, 8));
        assertTrue(new String(firstEncoding, 8, 45, java.nio.charset.StandardCharsets.UTF_8)
                .equals(RepositoryCaptureFingerprintEncoder.DOMAIN));
    }

    @Test
    void representativeUnicodeMembersAndUnsupportedEntryHaveStableGoldenFingerprint() {
        var members = List.of(
                member(".qaip/scenarios/\uE000.scenario.json", new byte[]{0x61, 0x62, 0x63}),
                member(".qaip/scenarios/\uD800\uDC00.scenario.json", new byte[]{0x00, (byte) 0xff, 0x10}));
        var unsupported = List.of(new RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry(
                ".qaip/scenarios/link.scenario.json", "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK"));

        assertEquals(
                "scenario-authority-repository-capture-v1:"
                        + "bb3821cdd89d3d62462bf73a6a795bd0f1082c6014018559d8b4d72685dc48e4",
                RepositoryCaptureFingerprintEncoder.fingerprint(input(members, unsupported)).value());
    }

    @Test
    void rejectsMembersOutsideExactUnicodeCodePointOrderInsteadOfSortingThem() {
        var supplementary = member(
                ".qaip/scenarios/\uD800\uDC00.scenario.json", new byte[]{2});
        var privateUse = member(".qaip/scenarios/\uE000.scenario.json", new byte[]{1});

        assertThrows(IllegalArgumentException.class,
                () -> input(List.of(supplementary, privateUse), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> input(List.of(privateUse, privateUse), List.of()));
    }

    @Test
    void normalizedPathParticipatesInAggregateFingerprint() {
        byte[] identicalBytes = new byte[]{9, 8, 7};
        var first = input(List.of(member(
                ".qaip/scenarios/a.scenario.json", identicalBytes)), List.of());
        var second = input(List.of(member(
                ".qaip/scenarios/b.scenario.json", identicalBytes)), List.of());

        assertEquals(first.capturedMembers().getFirst().rawMemberFingerprint(),
                second.capturedMembers().getFirst().rawMemberFingerprint());
        assertNotEquals(RepositoryCaptureFingerprintEncoder.fingerprint(first),
                RepositoryCaptureFingerprintEncoder.fingerprint(second));
    }

    @Test
    void unsupportedMatchingEntryParticipatesInAggregateFingerprint() {
        RepositoryCaptureFingerprintInput withoutUnsupported = input(List.of(), List.of());
        RepositoryCaptureFingerprintInput withUnsupported = input(List.of(), List.of(
                new RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry(
                        ".qaip/scenarios/link.scenario.json",
                        "SYMBOLIC_LINK",
                        "UNSUPPORTED_SYMBOLIC_LINK")));

        assertNotEquals(RepositoryCaptureFingerprintEncoder.fingerprint(withoutUnsupported),
                RepositoryCaptureFingerprintEncoder.fingerprint(withUnsupported));
    }

    @Test
    void provenanceAndHumanMetadataAreOutsideTheCanonicalInputContract() {
        List<String> componentNames = Arrays.stream(
                        RepositoryCaptureFingerprintInput.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();

        assertEquals(List.of(
                "sourceId",
                "sourceProfile",
                "discoveryProfileVersion",
                "discoveryAnchorInterpretationVersion",
                "pathNormalizationVersion",
                "orderingVersion",
                "memberByteFingerprintAlgorithm",
                "capturedMembers",
                "unsupportedMatchingEntries"), componentNames);
    }

    @Test
    void enforcesUnsigned64BoundsAndEncodesMaximumInBigEndianOrder() {
        RawSourceMemberFingerprint raw = RawSourceMemberFingerprint.calculate(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json", "REGULAR_FILE", BigInteger.valueOf(-1), raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json", "REGULAR_FILE",
                BigInteger.ONE.shiftLeft(64), raw));

        var maximum = new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json", "REGULAR_FILE",
                RepositoryCaptureFingerprintInput.MAX_UNSIGNED_64, raw);
        byte[] encoding = RepositoryCaptureFingerprintEncoder.encode(input(List.of(maximum), List.of()));

        assertTrue(containsConsecutiveBytes(encoding,
                new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}));
    }

    @Test
    void rejectsNoncanonicalPathsIdentifiersAlgorithmsAndInvalidUnicode() {
        RawSourceMemberFingerprint raw = RawSourceMemberFingerprint.calculate(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip\\scenarios\\a.scenario.json", "REGULAR_FILE", 0, raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/../a.scenario.json", "REGULAR_FILE", 0, raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json", "regular-file", 0, raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput(
                SOURCE_ID, SOURCE_PROFILE, DISCOVERY_PROFILE, ANCHOR_INTERPRETATION,
                PATH_NORMALIZATION, ORDERING, "SHA-256", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput(
                "repository:\uD800", SOURCE_PROFILE, DISCOVERY_PROFILE, ANCHOR_INTERPRETATION,
                PATH_NORMALIZATION, ORDERING, RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                List.of(), List.of()));
    }

    @Test
    void inputListsAndFingerprintValueHaveImmutableValueSemantics() {
        var mutableMembers = new java.util.ArrayList<RepositoryCaptureFingerprintInput.CapturedMember>();
        RepositoryCaptureFingerprintInput input = input(mutableMembers, List.of());
        mutableMembers.add(member(".qaip/scenarios/a.scenario.json", new byte[]{1}));

        assertEquals(List.of(), input.capturedMembers());
        assertThrows(UnsupportedOperationException.class, () -> input.capturedMembers().clear());
        RepositoryCaptureFingerprint first = RepositoryCaptureFingerprintEncoder.fingerprint(input);
        RepositoryCaptureFingerprint reconstructed = new RepositoryCaptureFingerprint(first.value());
        assertEquals(first, reconstructed);
        assertEquals(first.hashCode(), reconstructed.hashCode());
    }

    private static RepositoryCaptureFingerprintInput input(
            List<RepositoryCaptureFingerprintInput.CapturedMember> members,
            List<RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry> unsupported
    ) {
        return new RepositoryCaptureFingerprintInput(
                SOURCE_ID,
                SOURCE_PROFILE,
                DISCOVERY_PROFILE,
                ANCHOR_INTERPRETATION,
                PATH_NORMALIZATION,
                ORDERING,
                RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                members,
                unsupported);
    }

    private static RepositoryCaptureFingerprintInput.CapturedMember member(String path, byte[] bytes) {
        return new RepositoryCaptureFingerprintInput.CapturedMember(
                path,
                "REGULAR_FILE",
                bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
    }

    private static boolean containsConsecutiveBytes(byte[] source, byte[] expected) {
        outer:
        for (int offset = 0; offset <= source.length - expected.length; offset++) {
            for (int index = 0; index < expected.length; index++) {
                if (source[offset + index] != expected[index]) continue outer;
            }
            return true;
        }
        return false;
    }
}
