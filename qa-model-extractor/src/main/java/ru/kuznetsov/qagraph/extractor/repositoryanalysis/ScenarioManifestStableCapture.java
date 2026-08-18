package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Performs one mutation-stabilized ADR-013 capture attempt without retrying or qualifying it. */
public final class ScenarioManifestStableCapture {
    public static final String MUTATION_DETECTION_VERSION =
            RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION;

    private static final String DISCOVERY_ANCHOR = ScenarioManifestDiscovery.DISCOVERY_ANCHOR;
    private static final LinkOption[] NO_FOLLOW_LINKS = {LinkOption.NOFOLLOW_LINKS};

    private final DiscoveryStep discovery;
    private final CaptureObserver observer;
    private final MemberChannelOpener memberChannelOpener;

    public ScenarioManifestStableCapture() {
        this(new ScenarioManifestDiscovery()::discover, CaptureObserver.NONE,
                ScenarioManifestByteCapture::openWithoutFollowingLinks);
    }

    ScenarioManifestStableCapture(DiscoveryStep discovery, CaptureObserver observer) {
        this(discovery, observer, ScenarioManifestByteCapture::openWithoutFollowingLinks);
    }

    ScenarioManifestStableCapture(
            DiscoveryStep discovery,
            CaptureObserver observer,
            MemberChannelOpener memberChannelOpener
    ) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.observer = Objects.requireNonNull(observer, "observer");
        this.memberChannelOpener = Objects.requireNonNull(memberChannelOpener, "memberChannelOpener");
    }

    public ScenarioManifestStableCaptureResult capture(Path repositoryRoot) {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path root = repositoryRoot.toAbsolutePath().normalize();

        ScenarioManifestDiscoveryResult first;
        try {
            first = Objects.requireNonNull(discovery.discover(root), "D1 discovery result");
        } catch (IOException | SecurityException exception) {
            return failed(ScenarioManifestStableCaptureResult.Code.DISCOVERY_FAILED,
                    DISCOVERY_ANCHOR, "Scenario manifest D1 discovery failed");
        }
        if (hasStructuralFailure(first)) {
            return failed(ScenarioManifestStableCaptureResult.Code.DISCOVERY_STRUCTURAL_FAILURE,
                    DISCOVERY_ANCHOR, "Scenario manifest discovery anchor is structurally invalid");
        }

        try {
            observer.afterDiscoveryD1(first);
        } catch (IOException exception) {
            return failed(ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                    DISCOVERY_ANCHOR, "Scenario repository changed after D1 discovery");
        }

        List<ScenarioManifestStableCaptureResult.CapturedMember> members = new ArrayList<>();
        for (ScenarioManifestDiscoveryResult.Member member : first.members()) {
            CaptureMemberResult captured = captureMember(member);
            if (captured.failure() != null) return new ScenarioManifestStableCaptureResult.Failed(captured.failure());
            members.add(captured.member());
        }

        try {
            observer.beforeDiscoveryD2();
        } catch (IOException exception) {
            return failed(ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                    DISCOVERY_ANCHOR, "Scenario repository changed before D2 discovery");
        }

        ScenarioManifestDiscoveryResult second;
        try {
            second = Objects.requireNonNull(discovery.discover(root), "D2 discovery result");
        } catch (IOException | SecurityException exception) {
            return failed(ScenarioManifestStableCaptureResult.Code.DISCOVERY_FAILED,
                    DISCOVERY_ANCHOR, "Scenario manifest D2 discovery failed");
        }
        if (hasStructuralFailure(second) || !sameDiscoveryMembership(first, second)) {
            return failed(ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                    DISCOVERY_ANCHOR,
                    "Scenario repository membership or unsupported entries changed between D1 and D2");
        }

        return new ScenarioManifestStableCaptureResult.Completed(
                members, unsupportedEntries(first), MUTATION_DETECTION_VERSION);
    }

    private CaptureMemberResult captureMember(ScenarioManifestDiscoveryResult.Member member) {
        try (SeekableByteChannel channel = memberChannelOpener.open(member.path())) {
            FileState before = fileState(member.path());
            if (!before.regularFile()) return mutated(member, "member is no longer a regular file");

            byte[] bytes = ScenarioManifestByteCapture.readExactBytes(channel);
            observer.afterMemberBytesRead(member, bytes.clone());
            FileState after = fileState(member.path());
            if (!before.equals(after) || bytes.length != before.size() || bytes.length != after.size()) {
                return mutated(member, "member attributes changed while bytes were captured");
            }

            return new CaptureMemberResult(new ScenarioManifestStableCaptureResult.CapturedMember(
                    member.repositoryRelativePath(),
                    bytes,
                    bytes.length,
                    RawSourceMemberFingerprint.calculate(bytes)), null);
        } catch (NoSuchFileException exception) {
            return mutated(member, "member was removed or replaced after D1 discovery");
        } catch (IOException | SecurityException exception) {
            if (!Files.isRegularFile(member.path(), NO_FOLLOW_LINKS)) {
                return mutated(member, "member type changed after D1 discovery");
            }
            return new CaptureMemberResult(null, new ScenarioManifestStableCaptureResult.Failure(
                    ScenarioManifestStableCaptureResult.Code.MEMBER_READ_FAILED,
                    member.repositoryRelativePath(),
                    "Cannot read discovered scenario manifest member: " + member.repositoryRelativePath()));
        }
    }

    private static FileState fileState(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new FileState(attributes.isRegularFile(), attributes.fileKey(),
                attributes.size(), attributes.lastModifiedTime());
    }

    private static boolean hasStructuralFailure(ScenarioManifestDiscoveryResult result) {
        return result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == ScenarioManifestDiscoveryResult.Code.NON_DIRECTORY_DISCOVERY_ANCHOR);
    }

    private static boolean sameDiscoveryMembership(
            ScenarioManifestDiscoveryResult first,
            ScenarioManifestDiscoveryResult second
    ) {
        return memberPaths(first).equals(memberPaths(second))
                && unsupportedEntries(first).equals(unsupportedEntries(second));
    }

    private static List<String> memberPaths(ScenarioManifestDiscoveryResult result) {
        return result.members().stream()
                .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                .toList();
    }

    private static List<ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry> unsupportedEntries(
            ScenarioManifestDiscoveryResult result
    ) {
        return result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.code()
                        == ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_SYMBOLIC_LINK)
                .map(diagnostic -> new ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry(
                        diagnostic.repositoryRelativePath(),
                        "SYMBOLIC_LINK",
                        diagnostic.code().name()))
                .toList();
    }

    private static CaptureMemberResult mutated(
            ScenarioManifestDiscoveryResult.Member member,
            String reason
    ) {
        return new CaptureMemberResult(null, new ScenarioManifestStableCaptureResult.Failure(
                ScenarioManifestStableCaptureResult.Code.CONCURRENT_SOURCE_MUTATION,
                member.repositoryRelativePath(), reason + ": " + member.repositoryRelativePath()));
    }

    private static ScenarioManifestStableCaptureResult.Failed failed(
            ScenarioManifestStableCaptureResult.Code code,
            String path,
            String message
    ) {
        return new ScenarioManifestStableCaptureResult.Failed(
                new ScenarioManifestStableCaptureResult.Failure(code, path, message));
    }

    private record FileState(boolean regularFile, Object fileKey, long size, FileTime lastModifiedTime) {
    }

    private record CaptureMemberResult(
            ScenarioManifestStableCaptureResult.CapturedMember member,
            ScenarioManifestStableCaptureResult.Failure failure
    ) {
    }

    @FunctionalInterface
    interface DiscoveryStep {
        ScenarioManifestDiscoveryResult discover(Path repositoryRoot) throws IOException;
    }

    @FunctionalInterface
    interface MemberChannelOpener {
        SeekableByteChannel open(Path member) throws IOException;
    }

    interface CaptureObserver {
        CaptureObserver NONE = new CaptureObserver() { };

        default void afterDiscoveryD1(ScenarioManifestDiscoveryResult first) throws IOException {
        }

        default void afterMemberBytesRead(
                ScenarioManifestDiscoveryResult.Member member,
                byte[] bytes
        ) throws IOException {
        }

        default void beforeDiscoveryD2() throws IOException {
        }
    }
}
