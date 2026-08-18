package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioManifestStableCaptureTest {
    @TempDir
    Path repository;

    @Test
    void capturesStableMembersInDeterministicOrderWithExactBytesAndFingerprints() throws Exception {
        byte[] zBytes = {(byte) 0xff, 0x00, 0x0a};
        byte[] aBytes = {1, 2, 3, 4};
        write("z.scenario.json", zBytes);
        write("nested/a.scenario.json", aBytes);

        ScenarioManifestStableCaptureResult.Completed result = completed(
                new ScenarioManifestStableCapture());

        assertEquals(List.of(
                        ".qaip/scenarios/nested/a.scenario.json",
                        ".qaip/scenarios/z.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestStableCaptureResult.CapturedMember::repositoryRelativePath)
                        .toList());
        assertMember(result.members().get(0), aBytes);
        assertMember(result.members().get(1), zBytes);
        assertEquals(RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION,
                result.mutationDetectionVersion());
        assertEquals(List.of(), result.unsupportedMatchingEntries());
    }

    @Test
    void emptyRepositoryProducesStableEmptyMembership() {
        ScenarioManifestStableCaptureResult.Completed result = completed(
                new ScenarioManifestStableCapture());

        assertEquals(List.of(), result.members());
        assertEquals(List.of(), result.unsupportedMatchingEntries());
    }

    @Test
    void memberAdditionBetweenDiscoveriesIsConcurrentMutation() throws Exception {
        write("a.scenario.json", new byte[]{1});
        ScenarioManifestStableCapture capture = withObserver(new ScenarioManifestStableCapture.CaptureObserver() {
            @Override
            public void beforeDiscoveryD2() throws IOException {
                write("b.scenario.json", new byte[]{2});
            }
        });

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION);
    }

    @Test
    void memberRemovalAfterD1IsConcurrentMutationAndExposesNoPartialSuccess() throws Exception {
        write("a.scenario.json", new byte[]{1});
        Path removed = write("b.scenario.json", new byte[]{2});
        ScenarioManifestStableCapture capture = withObserver(new ScenarioManifestStableCapture.CaptureObserver() {
            @Override
            public void afterDiscoveryD1(ScenarioManifestDiscoveryResult first) throws IOException {
                Files.delete(removed);
            }
        });

        ScenarioManifestStableCaptureResult result = capture.capture(repository);
        ScenarioManifestStableCaptureResult.Failed failed = assertInstanceOf(
                ScenarioManifestStableCaptureResult.Failed.class, result);
        assertEquals(ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                failed.failure().code());
    }

    @Test
    void contentAndSizeChangeBetweenReadAndPostAttributesIsConcurrentMutation() throws Exception {
        Path changed = write("a.scenario.json", new byte[]{1, 2, 3});
        ScenarioManifestStableCapture capture = withObserver(new ScenarioManifestStableCapture.CaptureObserver() {
            @Override
            public void afterMemberBytesRead(
                    ScenarioManifestDiscoveryResult.Member member,
                    byte[] bytes
            ) throws IOException {
                Files.write(changed, new byte[]{1, 2, 3, 4, 5, 6});
            }
        });

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION);
    }

    @Test
    void replacementWithDifferentEntryTypeIsConcurrentMutationWhereFilesystemPermitsIt() throws Exception {
        Path replaced = write("a.scenario.json", new byte[]{1, 2, 3});
        ScenarioManifestStableCapture capture = withObserver(new ScenarioManifestStableCapture.CaptureObserver() {
            @Override
            public void afterMemberBytesRead(
                    ScenarioManifestDiscoveryResult.Member member,
                    byte[] bytes
            ) throws IOException {
                try {
                    Files.delete(replaced);
                    Files.createDirectory(replaced);
                } catch (IOException exception) {
                    Assumptions.abort("Filesystem does not permit replacing an open member: "
                            + exception.getMessage());
                }
            }
        });

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION);
    }

    @Test
    void unsupportedEntrySetChangeBetweenDiscoveriesIsConcurrentMutation() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        ScenarioManifestStableCapture capture = new ScenarioManifestStableCapture(
                ignored -> discoveryWithUnsupported(calls.getAndIncrement() > 0),
                ScenarioManifestStableCapture.CaptureObserver.NONE);

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION);
    }

    @Test
    void matchingUnsupportedDirectoryAdditionBetweenDiscoveriesIsConcurrentMutation() throws Exception {
        Files.createDirectories(repository.resolve(".qaip/scenarios"));
        ScenarioManifestStableCapture capture = withObserver(new ScenarioManifestStableCapture.CaptureObserver() {
            @Override
            public void beforeDiscoveryD2() throws IOException {
                Files.createDirectory(repository.resolve(".qaip/scenarios/added.scenario.json"));
            }
        });

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION);
    }

    @Test
    void matchingUnsupportedDirectoryFlowsWithStableKindAndCode() throws Exception {
        Files.createDirectories(repository.resolve(".qaip/scenarios/nested/directory.scenario.json"));

        ScenarioManifestStableCaptureResult.Completed result = completed(
                new ScenarioManifestStableCapture());

        assertEquals(List.of(new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        ".qaip/scenarios/nested/directory.scenario.json",
                        "DIRECTORY",
                        "UNSUPPORTED_DIRECTORY")),
                result.unsupportedMatchingEntries());
    }

    @Test
    void stableUnsupportedEntriesAreRetainedWithStableStructuralFields() {
        ScenarioManifestStableCapture capture = new ScenarioManifestStableCapture(
                ignored -> discoveryWithUnsupported(true),
                ScenarioManifestStableCapture.CaptureObserver.NONE);
        ScenarioManifestStableCaptureResult.Completed result = completed(capture);

        assertEquals(List.of(new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        ".qaip/scenarios/link.scenario.json",
                        "SYMBOLIC_LINK",
                        "UNSUPPORTED_SYMBOLIC_LINK")),
                result.unsupportedMatchingEntries());
    }

    @Test
    void humanDiagnosticMessageChangeDoesNotChangeUnsupportedIdentity() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        ScenarioManifestStableCapture capture = new ScenarioManifestStableCapture(
                ignored -> new ScenarioManifestDiscoveryResult(List.of(), List.of(
                        new ScenarioManifestDiscoveryResult.Diagnostic(
                                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_DIRECTORY,
                                ".qaip/scenarios/directory.scenario.json",
                                "human message " + calls.getAndIncrement()))),
                ScenarioManifestStableCapture.CaptureObserver.NONE);

        ScenarioManifestStableCaptureResult.Completed result = completed(capture);

        assertEquals(List.of(new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        ".qaip/scenarios/directory.scenario.json",
                        "DIRECTORY",
                        "UNSUPPORTED_DIRECTORY")),
                result.unsupportedMatchingEntries());
    }

    @Test
    void structuralDiscoveryFailureIsDistinctFromConcurrentMutation() throws Exception {
        Path anchor = repository.resolve(".qaip/scenarios");
        Files.createDirectories(anchor.getParent());
        Files.writeString(anchor, "not a directory");

        assertFailure(new ScenarioManifestStableCapture(),
                ScenarioManifestStableCaptureResult.Code.DISCOVERY_STRUCTURAL_FAILURE);
    }

    @Test
    void discoveryIoFailureIsDistinctProcessingFailure() {
        ScenarioManifestStableCapture capture = new ScenarioManifestStableCapture(
                ignored -> { throw new IOException("unavailable"); },
                ScenarioManifestStableCapture.CaptureObserver.NONE);

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.DISCOVERY_FAILED);
    }

    @Test
    void memberReadFailureIsDistinctDeterministicProcessingFailure() throws Exception {
        write("a.scenario.json", new byte[]{1});
        ScenarioManifestDiscovery discovery = new ScenarioManifestDiscovery();
        ScenarioManifestStableCapture capture = new ScenarioManifestStableCapture(
                discovery::discover,
                ScenarioManifestStableCapture.CaptureObserver.NONE,
                ignored -> { throw new IOException("unreadable"); });

        assertFailure(capture, ScenarioManifestStableCaptureResult.Code.MEMBER_READ_FAILED);
    }

    @Test
    void completedResultAndCapturedBytesAreImmutable() throws Exception {
        byte[] original = {10, 20, 30};
        write("a.scenario.json", original);
        ScenarioManifestStableCaptureResult.Completed result = completed(
                new ScenarioManifestStableCapture());

        original[0] = 99;
        byte[] exposed = result.members().getFirst().bytes();
        exposed[1] = 99;

        assertArrayEquals(new byte[]{10, 20, 30}, result.members().getFirst().bytes());
        assertThrows(UnsupportedOperationException.class, () -> result.members().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.unsupportedMatchingEntries().clear());
    }

    private ScenarioManifestStableCapture withObserver(
            ScenarioManifestStableCapture.CaptureObserver observer
    ) {
        ScenarioManifestDiscovery discovery = new ScenarioManifestDiscovery();
        return new ScenarioManifestStableCapture(discovery::discover, observer);
    }

    private ScenarioManifestStableCaptureResult.Completed completed(ScenarioManifestStableCapture capture) {
        return assertInstanceOf(ScenarioManifestStableCaptureResult.Completed.class,
                capture.capture(repository));
    }

    private void assertFailure(
            ScenarioManifestStableCapture capture,
            ScenarioManifestStableCaptureResult.Code expected
    ) {
        ScenarioManifestStableCaptureResult.Failed failed = assertInstanceOf(
                ScenarioManifestStableCaptureResult.Failed.class,
                capture.capture(repository));
        assertEquals(expected, failed.failure().code());
    }

    private static void assertMember(
            ScenarioManifestStableCaptureResult.CapturedMember member,
            byte[] expectedBytes
    ) {
        assertArrayEquals(expectedBytes, member.bytes());
        assertEquals(expectedBytes.length, member.rawByteLength());
        assertEquals(RawSourceMemberFingerprint.calculate(expectedBytes),
                member.rawMemberFingerprint());
    }

    private static ScenarioManifestDiscoveryResult discoveryWithUnsupported(boolean present) {
        List<ScenarioManifestDiscoveryResult.Diagnostic> diagnostics = present
                ? List.of(new ScenarioManifestDiscoveryResult.Diagnostic(
                        ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_SYMBOLIC_LINK,
                        ".qaip/scenarios/link.scenario.json",
                        "human-readable text is not compared"))
                : List.of();
        return new ScenarioManifestDiscoveryResult(List.of(), diagnostics);
    }

    private Path write(String relativePath, byte[] bytes) throws IOException {
        Path path = repository.resolve(".qaip/scenarios").resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
        return path;
    }
}
