package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioManifestByteCaptureTest {
    private final ScenarioManifestDiscovery discovery = new ScenarioManifestDiscovery();
    private final ScenarioManifestByteCapture capture = new ScenarioManifestByteCapture();

    @TempDir
    Path repository;

    @Test
    void preservesExactMemberBytesWithoutTextDecoding() throws Exception {
        byte[] expected = {(byte) 0xFF, 0x00, (byte) 0xC3, 0x28, 0x0A};
        write("binary.scenario.json", expected);

        ScenarioManifestCaptureResult.Completed result = completed();

        assertEquals(".qaip/scenarios/binary.scenario.json",
                result.members().getFirst().repositoryRelativePath());
        assertArrayEquals(expected, result.members().getFirst().bytes());
    }

    @Test
    void preservesDeterministicDiscoveryOrdering() throws Exception {
        write("z.scenario.json", new byte[]{3});
        write("nested/a.scenario.json", new byte[]{1});
        write("m.scenario.json", new byte[]{2});

        ScenarioManifestCaptureResult.Completed result = completed();

        assertEquals(List.of(
                        ".qaip/scenarios/m.scenario.json",
                        ".qaip/scenarios/nested/a.scenario.json",
                        ".qaip/scenarios/z.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestCaptureResult.CapturedMember::repositoryRelativePath)
                        .toList());
        assertEquals(List.of(2, 1, 3), result.members().stream()
                .map(member -> Byte.toUnsignedInt(member.bytes()[0]))
                .toList());
    }

    @Test
    void emptyDiscoveryProducesCompletedEmptyCapture() throws Exception {
        ScenarioManifestCaptureResult.Completed result = completed();

        assertEquals(List.of(), result.members());
    }

    @Test
    void capturedMembersAndBytesAreImmutable() throws Exception {
        byte[] original = {10, 20, 30};
        write("immutable.scenario.json", original);
        ScenarioManifestCaptureResult.Completed result = completed();

        original[0] = 99;
        byte[] exposed = result.members().getFirst().bytes();
        exposed[1] = 99;

        assertArrayEquals(new byte[]{10, 20, 30}, result.members().getFirst().bytes());
        assertThrows(UnsupportedOperationException.class, () -> result.members().clear());
    }

    @Test
    void readFailureReturnsFailureWithoutPartialSuccessfulCapture() throws Exception {
        write("a-readable.scenario.json", new byte[]{1});
        Path unavailable = write("b-unavailable.scenario.json", new byte[]{2});
        ScenarioManifestDiscoveryResult discovered = discovery.discover(repository);
        Files.delete(unavailable);

        ScenarioManifestCaptureResult.Failed failed = assertInstanceOf(
                ScenarioManifestCaptureResult.Failed.class, capture.capture(discovered));

        assertEquals(ScenarioManifestCaptureResult.Code.MEMBER_READ_FAILED, failed.failure().code());
        assertEquals(".qaip/scenarios/b-unavailable.scenario.json",
                failed.failure().repositoryRelativePath());
        assertEquals("Cannot read discovered scenario manifest member: "
                        + ".qaip/scenarios/b-unavailable.scenario.json",
                failed.failure().message());
    }

    @Test
    void requiresDiscoveryResult() {
        assertThrows(NullPointerException.class, () -> capture.capture(null));
    }

    private ScenarioManifestCaptureResult.Completed completed() throws Exception {
        return assertInstanceOf(ScenarioManifestCaptureResult.Completed.class,
                capture.capture(discovery.discover(repository)));
    }

    private Path write(String relativePath, byte[] bytes) throws Exception {
        Path path = repository.resolve(".qaip/scenarios").resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
        return path;
    }
}
