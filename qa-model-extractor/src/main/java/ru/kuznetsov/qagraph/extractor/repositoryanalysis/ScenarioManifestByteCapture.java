package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ScenarioManifestByteCapture {
    public ScenarioManifestCaptureResult capture(ScenarioManifestDiscoveryResult discovery) {
        Objects.requireNonNull(discovery, "discovery");
        List<ScenarioManifestCaptureResult.CapturedMember> captured = new ArrayList<>();

        for (ScenarioManifestDiscoveryResult.Member member : discovery.members()) {
            try {
                byte[] bytes = readExactBytesWithoutFollowingLinks(member);
                captured.add(new ScenarioManifestCaptureResult.CapturedMember(
                        member.path(), member.repositoryRelativePath(), bytes));
            } catch (IOException | SecurityException exception) {
                return new ScenarioManifestCaptureResult.Failed(new ScenarioManifestCaptureResult.Failure(
                        ScenarioManifestCaptureResult.Code.MEMBER_READ_FAILED,
                        member.repositoryRelativePath(),
                        "Cannot read discovered scenario manifest member: "
                                + member.repositoryRelativePath()));
            }
        }

        return new ScenarioManifestCaptureResult.Completed(captured);
    }

    private static byte[] readExactBytesWithoutFollowingLinks(ScenarioManifestDiscoveryResult.Member member)
            throws IOException {
        try (SeekableByteChannel channel = java.nio.file.Files.newByteChannel(
                member.path(), Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (channel.read(buffer) >= 0) {
                buffer.flip();
                output.write(buffer.array(), buffer.position(), buffer.remaining());
                buffer.clear();
            }
            return output.toByteArray();
        }
    }
}
