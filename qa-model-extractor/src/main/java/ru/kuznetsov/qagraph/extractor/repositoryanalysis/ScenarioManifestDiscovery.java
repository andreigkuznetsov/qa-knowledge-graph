package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ScenarioManifestDiscovery {
    public static final String DISCOVERY_ANCHOR = ".qaip/scenarios";
    public static final String MANIFEST_SUFFIX = ".scenario.json";

    private static final LinkOption[] NO_FOLLOW_LINKS = {LinkOption.NOFOLLOW_LINKS};
    private static final Comparator<String> CODE_POINT_ORDER = ScenarioManifestDiscovery::compareCodePoints;

    public ScenarioManifestDiscoveryResult discover(Path repositoryRoot) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path anchor = root.resolve(DISCOVERY_ANCHOR).normalize();
        requireContained(root, anchor, "discovery anchor");

        if (Files.notExists(anchor, NO_FOLLOW_LINKS)) {
            return new ScenarioManifestDiscoveryResult(List.of(), List.of());
        }
        if (!Files.isDirectory(anchor, NO_FOLLOW_LINKS)) {
            return new ScenarioManifestDiscoveryResult(List.of(), List.of(new ScenarioManifestDiscoveryResult.Diagnostic(
                    ScenarioManifestDiscoveryResult.Code.NON_DIRECTORY_DISCOVERY_ANCHOR,
                    repositoryRelativePath(root, anchor),
                    "Scenario manifest discovery anchor is not a directory: "
                            + repositoryRelativePath(root, anchor))));
        }

        List<ScenarioManifestDiscoveryResult.Member> members = new ArrayList<>();
        List<ScenarioManifestDiscoveryResult.Diagnostic> diagnostics = new ArrayList<>();
        try (Stream<Path> entries = Files.walk(anchor)) {
            for (Path entry : entries.toList()) {
                Path normalized = entry.toAbsolutePath().normalize();
                requireContained(root, normalized, "discovery entry");
                requireContained(anchor, normalized, "discovery entry");
                if (normalized.equals(anchor) || !matchesSuffix(normalized)) continue;

                String relativePath = repositoryRelativePath(root, normalized);
                if (Files.isRegularFile(normalized, NO_FOLLOW_LINKS)) {
                    members.add(new ScenarioManifestDiscoveryResult.Member(normalized, relativePath));
                } else if (Files.isSymbolicLink(normalized)) {
                    diagnostics.add(unsupported(
                            ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_SYMBOLIC_LINK,
                            relativePath,
                            "Matching scenario manifest symbolic link is unsupported: "));
                } else if (Files.isDirectory(normalized, NO_FOLLOW_LINKS)) {
                    diagnostics.add(unsupported(
                            ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_DIRECTORY,
                            relativePath,
                            "Matching scenario manifest directory is unsupported: "));
                } else {
                    diagnostics.add(unsupported(
                            ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_OTHER_NON_REGULAR_ENTRY,
                            relativePath,
                            "Matching scenario manifest non-regular entry is unsupported: "));
                }
            }
        }

        members.sort(Comparator.comparing(
                ScenarioManifestDiscoveryResult.Member::repositoryRelativePath, CODE_POINT_ORDER));
        diagnostics.sort(Comparator
                .comparing(ScenarioManifestDiscoveryResult.Diagnostic::repositoryRelativePath, CODE_POINT_ORDER)
                .thenComparing(value -> value.code().name()));
        rejectDuplicatePaths(members);
        return new ScenarioManifestDiscoveryResult(members, diagnostics);
    }

    private static ScenarioManifestDiscoveryResult.Diagnostic unsupported(
            ScenarioManifestDiscoveryResult.Code code,
            String relativePath,
            String messagePrefix
    ) {
        return new ScenarioManifestDiscoveryResult.Diagnostic(
                code, relativePath, messagePrefix + relativePath);
    }

    private static boolean matchesSuffix(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(MANIFEST_SUFFIX);
    }

    private static String repositoryRelativePath(Path root, Path path) {
        requireContained(root, path, "repository entry");
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static void requireContained(Path container, Path candidate, String role) {
        if (!candidate.startsWith(container)) {
            throw new IllegalArgumentException(role + " escapes " + container + ": " + candidate);
        }
    }

    private static void rejectDuplicatePaths(List<ScenarioManifestDiscoveryResult.Member> members) {
        for (int index = 1; index < members.size(); index++) {
            String previous = members.get(index - 1).repositoryRelativePath();
            String current = members.get(index).repositoryRelativePath();
            if (previous.equals(current)) {
                throw new IllegalStateException("Duplicate normalized scenario manifest path: " + current);
            }
        }
    }

    private static int compareCodePoints(String left, String right) {
        var leftPoints = left.codePoints().iterator();
        var rightPoints = right.codePoints().iterator();
        while (leftPoints.hasNext() && rightPoints.hasNext()) {
            int comparison = Integer.compare(leftPoints.nextInt(), rightPoints.nextInt());
            if (comparison != 0) return comparison;
        }
        return Boolean.compare(leftPoints.hasNext(), rightPoints.hasNext());
    }
}
