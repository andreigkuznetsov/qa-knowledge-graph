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
    private static final String SOURCE_CONTRACT = "qaip-source-snapshot-contract-v1";
    private static final String SOURCE_PROFILE = "qaip-scenario-authority-repository-json-v1";
    private static final String DISCOVERY_PROFILE = "scenario-authority-repository-discovery-v1";
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
                        + "7ff6c649a37ca756c6a635a005a7e3f74ea1294cc22f83a1fb285d3cf35dc622",
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
                        + "f5419ab2f111872ccc152cc72fefc1fd01ef423d8e7bc77ad85d66f6571ae0a9",
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
                "sourceContractVersion",
                "sourceProfile",
                "discoveryProfileVersion",
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
                ".qaip/scenarios/a.scenario.json", BigInteger.valueOf(-1), raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json",
                BigInteger.ONE.shiftLeft(64), raw));

        var maximum = new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/a.scenario.json",
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
                ".qaip\\scenarios\\a.scenario.json", 0, raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput.CapturedMember(
                ".qaip/scenarios/../a.scenario.json", 0, raw));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput(
                SOURCE_ID, SOURCE_CONTRACT, SOURCE_PROFILE, DISCOVERY_PROFILE,
                PATH_NORMALIZATION, ORDERING, "SHA-256", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryCaptureFingerprintInput(
                "repository:\uD800", SOURCE_CONTRACT, SOURCE_PROFILE, DISCOVERY_PROFILE,
                PATH_NORMALIZATION, ORDERING, RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                List.of(), List.of()));
    }

    @Test
    void sourceContractVersionParticipatesInFingerprint() {
        RepositoryCaptureFingerprintInput first = input(List.of(), List.of());
        RepositoryCaptureFingerprintInput second = new RepositoryCaptureFingerprintInput(
                SOURCE_ID,
                "qaip-source-snapshot-contract-v2",
                SOURCE_PROFILE,
                DISCOVERY_PROFILE,
                PATH_NORMALIZATION,
                ORDERING,
                RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                List.of(),
                List.of());

        assertNotEquals(RepositoryCaptureFingerprintEncoder.fingerprint(first),
                RepositoryCaptureFingerprintEncoder.fingerprint(second));
    }

    @Test
    void v1FrozenIdentifiersAndPerMemberAlgorithmAreEncodedAsSeparateFields() {
        assertEquals("scenario-authority-repository-discovery-anchor-v1",
                RepositoryCaptureFingerprintEncoder.DISCOVERY_ANCHOR_CONTRACT_VERSION);
        assertEquals(".qaip/scenarios",
                RepositoryCaptureFingerprintEncoder.EXACT_RELATIVE_DISCOVERY_ANCHOR);
        assertEquals("scenario-authority-capture-stability-v1",
                RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        assertEquals("STABLE_CAPTURE_COMPLETED",
                RepositoryCaptureFingerprintEncoder.SUCCESSFUL_STABLE_CAPTURE_OUTCOME);

        byte[] encoding = RepositoryCaptureFingerprintEncoder.encode(input(List.of(
                member(".qaip/scenarios/a.scenario.json", new byte[]{1})), List.of()));

        assertTrue(containsLengthPrefixedText(encoding,
                RepositoryCaptureFingerprintEncoder.DISCOVERY_ANCHOR_CONTRACT_VERSION));
        assertTrue(containsLengthPrefixedText(encoding,
                RepositoryCaptureFingerprintEncoder.EXACT_RELATIVE_DISCOVERY_ANCHOR));
        assertTrue(containsLengthPrefixedText(encoding,
                RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION));
        assertTrue(containsLengthPrefixedText(encoding,
                RepositoryCaptureFingerprintEncoder.SUCCESSFUL_STABLE_CAPTURE_OUTCOME));
        assertEquals(3, countLengthPrefixedText(
                encoding, RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER));
    }

    @Test
    void capturedMemberContractContainsNoEntryKind() {
        assertEquals(List.of(
                        "normalizedRepositoryRelativePath",
                        "rawByteLength",
                        "rawMemberFingerprint"),
                Arrays.stream(RepositoryCaptureFingerprintInput.CapturedMember.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toList());
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
                SOURCE_CONTRACT,
                SOURCE_PROFILE,
                DISCOVERY_PROFILE,
                PATH_NORMALIZATION,
                ORDERING,
                RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,
                members,
                unsupported);
    }

    private static RepositoryCaptureFingerprintInput.CapturedMember member(String path, byte[] bytes) {
        return new RepositoryCaptureFingerprintInput.CapturedMember(
                path,
                bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
    }

    private static boolean containsLengthPrefixedText(byte[] source, String value) {
        return countLengthPrefixedText(source, value) > 0;
    }

    private static int countLengthPrefixedText(byte[] source, String value) {
        byte[] text = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] encoded = new byte[Long.BYTES + text.length];
        java.nio.ByteBuffer.wrap(encoded).putLong(text.length).put(text);
        int count = 0;
        for (int offset = 0; offset <= source.length - encoded.length; offset++) {
            boolean equal = true;
            for (int index = 0; index < encoded.length; index++) {
                if (source[offset + index] != encoded[index]) {
                    equal = false;
                    break;
                }
            }
            if (equal) count++;
        }
        return count;
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
