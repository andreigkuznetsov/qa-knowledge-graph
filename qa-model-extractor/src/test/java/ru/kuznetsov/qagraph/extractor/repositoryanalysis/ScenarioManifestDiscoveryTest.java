package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioManifestDiscoveryTest {
    private final ScenarioManifestDiscovery discovery = new ScenarioManifestDiscovery();

    @TempDir
    Path repository;

    @Test
    void missingAnchorProducesEmptyMembership() throws Exception {
        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertTrue(result.members().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void nonDirectoryAnchorProducesStructuralDiagnostic() throws Exception {
        Path anchor = repository.resolve(".qaip/scenarios");
        Files.createDirectories(anchor.getParent());
        Files.writeString(anchor, "not a directory");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertTrue(result.members().isEmpty());
        assertEquals(List.of(ScenarioManifestDiscoveryResult.Code.NON_DIRECTORY_DISCOVERY_ANCHOR),
                result.diagnostics().stream().map(ScenarioManifestDiscoveryResult.Diagnostic::code).toList());
        assertEquals(".qaip/scenarios", result.diagnostics().getFirst().repositoryRelativePath());
    }

    @Test
    void recursivelyDiscoversOnlyExactCaseSensitiveSuffixAndRegularFiles() throws Exception {
        write(".qaip/scenarios/root.scenario.json");
        write(".qaip/scenarios/domain/nested.scenario.json");
        write(".qaip/scenarios/plain.json");
        write(".qaip/scenarios/wrong.SCENARIO.JSON");
        write(".qaip/scenarios/backup.scenario.json.bak");
        Files.createDirectories(repository.resolve(".qaip/scenarios/ordinary-directory"));
        write("outside.scenario.json");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(
                        ".qaip/scenarios/domain/nested.scenario.json",
                        ".qaip/scenarios/root.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                        .toList());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void matchingDirectoryIsUnsupportedWhileOrdinaryDirectoryIsTraversalOnly() throws Exception {
        Files.createDirectories(repository.resolve(".qaip/scenarios/matching.scenario.json"));
        Files.createDirectories(repository.resolve(".qaip/scenarios/ordinary-directory"));
        write(".qaip/scenarios/ordinary-directory/member.scenario.json");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(".qaip/scenarios/ordinary-directory/member.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                        .toList());
        assertUnsupported(result.diagnostics().getFirst(),
                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_DIRECTORY,
                "DIRECTORY",
                ".qaip/scenarios/matching.scenario.json");
    }

    @Test
    void matchingDirectoryRemainsUnsupportedAndTraversalContinuesBelowIt() throws Exception {
        Files.createDirectories(repository.resolve(".qaip/scenarios/nested/container.scenario.json"));
        write(".qaip/scenarios/nested/container.scenario.json/member.scenario.json");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(
                        ".qaip/scenarios/nested/container.scenario.json/member.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                        .toList());
        assertUnsupported(result.diagnostics().getFirst(),
                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_DIRECTORY,
                "DIRECTORY",
                ".qaip/scenarios/nested/container.scenario.json");
    }

    @Test
    void unsupportedEntriesUseDeterministicCodePointPathOrdering() throws Exception {
        Files.createDirectories(repository.resolve(".qaip/scenarios/z.scenario.json"));
        Files.createDirectories(repository.resolve(".qaip/scenarios/a.scenario.json"));
        Files.createDirectories(repository.resolve(".qaip/scenarios/nested/m.scenario.json"));

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(
                        ".qaip/scenarios/a.scenario.json",
                        ".qaip/scenarios/nested/m.scenario.json",
                        ".qaip/scenarios/z.scenario.json"),
                result.diagnostics().stream()
                        .map(ScenarioManifestDiscoveryResult.Diagnostic::repositoryRelativePath)
                        .toList());
    }

    @Test
    void unsupportedEntryKindIdentifiersAreFrozen() {
        assertEquals("SYMBOLIC_LINK",
                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_SYMBOLIC_LINK.unsupportedEntryKind());
        assertEquals("DIRECTORY",
                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_DIRECTORY.unsupportedEntryKind());
        assertEquals("OTHER_NON_REGULAR",
                ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_OTHER_NON_REGULAR_ENTRY.unsupportedEntryKind());
    }

    @Test
    void ordersNormalizedPathsByUnicodeCodePoint() throws Exception {
        write(".qaip/scenarios/\uE000.scenario.json");
        write(".qaip/scenarios/\uD800\uDC00.scenario.json");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(
                        ".qaip/scenarios/\uE000.scenario.json",
                        ".qaip/scenarios/\uD800\uDC00.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                        .toList());
    }

    @Test
    void matchingSymbolicLinkIsNotFollowedAndProducesDiagnostic() throws Exception {
        Path external = repository.resolve("external.json");
        Files.writeString(external, "external");
        Path link = repository.resolve(".qaip/scenarios/linked.scenario.json");
        Files.createDirectories(link.getParent());
        try {
            Files.createSymbolicLink(link, external);
        } catch (UnsupportedOperationException | IOException exception) {
            Assumptions.abort("Symbolic links are unavailable: " + exception.getMessage());
        }

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertTrue(result.members().isEmpty());
        assertEquals(List.of(ScenarioManifestDiscoveryResult.Code.UNSUPPORTED_SYMBOLIC_LINK),
                result.diagnostics().stream().map(ScenarioManifestDiscoveryResult.Diagnostic::code).toList());
        assertEquals(".qaip/scenarios/linked.scenario.json",
                result.diagnostics().getFirst().repositoryRelativePath());
        assertEquals("SYMBOLIC_LINK",
                result.diagnostics().getFirst().code().unsupportedEntryKind());
    }

    @Test
    void gitIgnoreDoesNotExcludeMatchingFilesystemMember() throws Exception {
        write(".gitignore", ".qaip/\n");
        write(".qaip/scenarios/ignored.scenario.json", "ignored but discovered");

        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertEquals(List.of(".qaip/scenarios/ignored.scenario.json"),
                result.members().stream()
                        .map(ScenarioManifestDiscoveryResult.Member::repositoryRelativePath)
                        .toList());
    }

    @Test
    void requiresExplicitRepositoryRoot() {
        assertThrows(NullPointerException.class, () -> discovery.discover(null));
    }

    @Test
    void resultCollectionsAreImmutable() throws Exception {
        write(".qaip/scenarios/one.scenario.json");
        ScenarioManifestDiscoveryResult result = discovery.discover(repository);

        assertThrows(UnsupportedOperationException.class, () -> result.members().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    }

    private void write(String relativePath) throws IOException {
        write(relativePath, "not parsed");
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void assertUnsupported(
            ScenarioManifestDiscoveryResult.Diagnostic diagnostic,
            ScenarioManifestDiscoveryResult.Code code,
            String entryKind,
            String path
    ) {
        assertEquals(code, diagnostic.code());
        assertEquals(entryKind, diagnostic.code().unsupportedEntryKind());
        assertEquals(path, diagnostic.repositoryRelativePath());
    }
}
