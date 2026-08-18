package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintInput;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioRepositoryCaptureSnapshotCandidateTest {
    private static final String SOURCE_ID = "repository:book-shop";
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "scenario-authority-source-contract-v1",
                    "scenario-authority-repository-profile-v1",
                    "scenario-manifest-discovery-v1",
                    "repository-relative-path-v1",
                    "unicode-code-point-order-v1",
                    RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    @TempDir
    Path repository;

    @Test
    void constructsEmptyCandidateFromStableCapture() {
        ScenarioManifestStableCaptureResult.Completed capture = stableCapture();

        ScenarioRepositoryCaptureSnapshotCandidate candidate = candidate(capture, "snapshot-empty");

        assertEquals(List.of(), candidate.members());
        assertEquals(List.of(), candidate.unsupportedMatchingEntries());
        assertEquals(expectedFingerprint(capture), candidate.contentFingerprint());
    }

    @Test
    void preservesNonEmptyCaptureAndWiresOnlyApprovedEncoderInput() throws Exception {
        byte[] expected = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, 0x0a, 0x00};
        Path member = repository.resolve(".qaip/scenarios/a.scenario.json");
        Files.createDirectories(member.getParent());
        Files.write(member, expected);
        ScenarioManifestStableCaptureResult.Completed capture = stableCapture();

        ScenarioRepositoryCaptureSnapshotCandidate candidate = candidate(capture, "snapshot-1");

        assertEquals(CONTRACT, candidate.contractIdentifiers());
        assertEquals(RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION,
                candidate.mutationDetectionVersion());
        assertEquals(expectedFingerprint(capture), candidate.contentFingerprint());
        assertEquals(".qaip/scenarios/a.scenario.json",
                candidate.members().getFirst().repositoryRelativePath());
        assertEquals(expected.length, candidate.members().getFirst().rawByteLength());
        assertEquals(RawSourceMemberFingerprint.calculate(expected),
                candidate.members().getFirst().rawMemberFingerprint());
        assertArrayEquals(expected, candidate.members().getFirst().bytes());
    }

    @Test
    void identityIsExactSourceSnapshotAndContentFingerprintTuple() {
        ScenarioRepositoryCaptureSnapshotCandidate candidate = candidate(stableCapture(), "external-observation-17");

        assertEquals(new ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity(
                        SOURCE_ID, "external-observation-17", candidate.contentFingerprint()),
                candidate.identity());
        assertEquals(SOURCE_ID, candidate.sourceId());
        assertEquals("external-observation-17", candidate.snapshotId());
    }

    @Test
    void equalContentWithDifferentExternalSnapshotIdsRemainsDistinctObservations() {
        ScenarioManifestStableCaptureResult.Completed capture = stableCapture();
        ScenarioRepositoryCaptureSnapshotCandidate first = candidate(capture, "observation-1");
        ScenarioRepositoryCaptureSnapshotCandidate second = candidate(capture, "observation-2");

        assertEquals(first.contentFingerprint(), second.contentFingerprint());
        assertNotEquals(first.identity(), second.identity());
        assertEquals("observation-1", first.snapshotId());
        assertEquals("observation-2", second.snapshotId());
    }

    @Test
    void changedPathContentOrUnsupportedEntryChangesFingerprint() {
        ScenarioManifestStableCaptureResult.Completed baseline = completed(
                List.of(member(".qaip/scenarios/a.scenario.json", new byte[]{1})), List.of());
        ScenarioManifestStableCaptureResult.Completed changedPath = completed(
                List.of(member(".qaip/scenarios/b.scenario.json", new byte[]{1})), List.of());
        ScenarioManifestStableCaptureResult.Completed changedContent = completed(
                List.of(member(".qaip/scenarios/a.scenario.json", new byte[]{2})), List.of());
        ScenarioManifestStableCaptureResult.Completed withUnsupported = completed(List.of(), List.of(
                new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        ".qaip/scenarios/link.scenario.json", "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK")));

        assertNotEquals(candidate(baseline, "same").contentFingerprint(),
                candidate(changedPath, "same").contentFingerprint());
        assertNotEquals(candidate(baseline, "same").contentFingerprint(),
                candidate(changedContent, "same").contentFingerprint());
        assertNotEquals(candidate(completed(List.of(), List.of()), "same").contentFingerprint(),
                candidate(withUnsupported, "same").contentFingerprint());
    }

    @Test
    void discoveredMatchingDirectoryAppearanceChangesCandidateFingerprint() throws Exception {
        ScenarioRepositoryCaptureSnapshotCandidate withoutDirectory =
                candidate(stableCapture(), "before");
        Files.createDirectories(repository.resolve(".qaip/scenarios/directory.scenario.json"));
        ScenarioRepositoryCaptureSnapshotCandidate withDirectory =
                candidate(stableCapture(), "after");

        assertNotEquals(withoutDirectory.contentFingerprint(), withDirectory.contentFingerprint());
        assertEquals(List.of(new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        ".qaip/scenarios/directory.scenario.json",
                        "DIRECTORY",
                        "UNSUPPORTED_DIRECTORY")),
                withDirectory.unsupportedMatchingEntries());
    }

    @Test
    void provenanceOnlyMetadataCannotChangeContentFingerprint() {
        ScenarioManifestStableCaptureResult.Completed capture = stableCapture();
        var firstProvenance = new ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance(
                Optional.of(Instant.parse("2026-08-18T10:00:00Z")), Optional.of("abc123"),
                Optional.of("main"), Optional.of("C:/checkout-one"), Optional.of("prov:1"),
                Map.of("host", "host-one"));
        var secondProvenance = new ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance(
                Optional.of(Instant.parse("2026-08-19T10:00:00Z")), Optional.of("def456"),
                Optional.of("feature"), Optional.of("D:/checkout-two"), Optional.of("prov:2"),
                Map.of("host", "host-two"));

        ScenarioRepositoryCaptureSnapshotCandidate first = create(capture, "same", firstProvenance);
        ScenarioRepositoryCaptureSnapshotCandidate second = create(capture, "same", secondProvenance);

        assertEquals(first.contentFingerprint(), second.contentFingerprint());
        assertNotEquals(first.provenance(), second.provenance());
    }

    @Test
    void candidateAndExactBytesAreImmutable() {
        byte[] input = {1, 2, 3};
        var mutableMembers = new ArrayList<ScenarioManifestStableCaptureResult.CapturedMember>();
        mutableMembers.add(member(".qaip/scenarios/a.scenario.json", input));
        ScenarioManifestStableCaptureResult.Completed capture = completed(mutableMembers, List.of());
        ScenarioRepositoryCaptureSnapshotCandidate candidate = candidate(capture, "immutable");

        input[0] = 9;
        mutableMembers.clear();
        byte[] exposed = candidate.members().getFirst().bytes();
        exposed[1] = 9;

        assertArrayEquals(new byte[]{1, 2, 3}, candidate.members().getFirst().bytes());
        assertThrows(UnsupportedOperationException.class, () -> candidate.members().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> candidate.unsupportedMatchingEntries().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> candidate.provenance().captureMetadata().put("host", "changed"));
    }

    @Test
    void rejectsFailedNonV1AndInvalidCaptureInput() {
        var failed = new ScenarioManifestStableCaptureResult.Failed(
                new ScenarioManifestStableCaptureResult.Failure(
                        ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                        ".qaip/scenarios", "changed"));
        var wrongVersion = new ScenarioManifestStableCaptureResult.Completed(
                List.of(), List.of(), "different-mutation-contract");

        assertThrows(IllegalArgumentException.class, () -> candidate(failed, "failed"));
        assertThrows(IllegalArgumentException.class, () -> candidate(wrongVersion, "wrong-version"));
        assertThrows(NullPointerException.class, () -> candidate(null, "null"));
        assertThrows(IllegalArgumentException.class, () -> candidate(stableCapture(), " "));
    }

    @Test
    void rejectsNoncanonicalCaptureOrderingRatherThanNormalizingIt() {
        ScenarioManifestStableCaptureResult.Completed capture = completed(List.of(
                member(".qaip/scenarios/z.scenario.json", new byte[]{1}),
                member(".qaip/scenarios/a.scenario.json", new byte[]{2})), List.of());

        assertThrows(IllegalArgumentException.class, () -> candidate(capture, "unordered"));
    }

    private ScenarioManifestStableCaptureResult.Completed stableCapture() {
        return (ScenarioManifestStableCaptureResult.Completed)
                new ScenarioManifestStableCapture().capture(repository);
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult capture,
            String snapshotId
    ) {
        return create(capture, snapshotId, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate create(
            ScenarioManifestStableCaptureResult capture,
            String snapshotId,
            ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance provenance
    ) {
        return ScenarioRepositoryCaptureSnapshotCandidate.create(
                capture, SOURCE_ID, snapshotId, CONTRACT, provenance);
    }

    private static ScenarioManifestStableCaptureResult.Completed completed(
            List<ScenarioManifestStableCaptureResult.CapturedMember> members,
            List<ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry> unsupported
    ) {
        return new ScenarioManifestStableCaptureResult.Completed(
                members, unsupported, RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String path, byte[] bytes) {
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                path, bytes, bytes.length, RawSourceMemberFingerprint.calculate(bytes));
    }

    private static ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint
            expectedFingerprint(ScenarioManifestStableCaptureResult.Completed capture) {
        return RepositoryCaptureFingerprintEncoder.fingerprint(new RepositoryCaptureFingerprintInput(
                SOURCE_ID,
                CONTRACT.sourceContractVersion(),
                CONTRACT.sourceProfile(),
                CONTRACT.discoveryProfileVersion(),
                CONTRACT.pathNormalizationVersion(),
                CONTRACT.orderingVersion(),
                CONTRACT.memberByteFingerprintAlgorithm(),
                capture.members().stream().map(member -> new RepositoryCaptureFingerprintInput.CapturedMember(
                        member.repositoryRelativePath(), member.rawByteLength(), member.rawMemberFingerprint())).toList(),
                capture.unsupportedMatchingEntries().stream()
                        .map(entry -> new RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry(
                                entry.repositoryRelativePath(), entry.entryKind(), entry.stableDiagnosticCode()))
                        .toList()));
    }
}
