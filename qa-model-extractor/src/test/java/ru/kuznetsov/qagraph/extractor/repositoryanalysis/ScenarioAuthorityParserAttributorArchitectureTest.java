package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityParserAttributorArchitectureTest {
    @Test void extractorDelegatesAndContainsNoNormativeParserOrAttributionLogic() throws Exception {
        Path root = repositoryRoot();
        String parser = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioManifestJsonParser.java"));
        assertTrue(parser.contains("ScenarioAuthorityExactJsonParserV1"));
        for (String forbidden : new String[]{"JsonFactory", "ObjectMapper", "StandardCharsets", "newDecoder",
                "STRICT_DUPLICATE_DETECTION", "USE_BIG_INTEGER_FOR_INTS", "USE_BIG_DECIMAL_FOR_FLOATS"})
            assertFalse(parser.contains(forbidden), forbidden);

        String processor = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioLogicalSourceMemberProcessor.java"));
        assertTrue(processor.contains("ScenarioAuthorityAttributorV1"));
        for (String forbidden : new String[]{"Pattern.compile", "MAX_AUTHORITY_LENGTH", "AUTHORITY.matcher",
                "document.has(\"authority\")", "authorityNode.isTextual"})
            assertFalse(processor.contains(forbidden), forbidden);

        String validationCore = Files.readString(root.resolve("qa-model-validation-core/build.gradle"));
        assertFalse(validationCore.contains("qa-evidence-governance-core"));
        try (var files = Files.walk(root.resolve("qa-model-validation-core/src/main"))) {
            assertTrue(files.filter(Files::isRegularFile).noneMatch(path -> {
                try {
                    String text = Files.readString(path);
                    return text.contains("ScenarioAuthorityExactJsonParserV1")
                            || text.contains("ScenarioAuthorityAttributorV1");
                } catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
            }));
        }
        try (var files = Files.walk(root.resolve("qa-model-extractor/src/main/java"))) {
            assertTrue(files.filter(path -> path.toString().endsWith(".java")).noneMatch(path -> {
                try {
                    return Files.readString(path).startsWith(
                            "package ru.kuznetsov.qaip.evidencegovernance.source;");
                } catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
            }));
        }
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
        return java.util.Objects.requireNonNull(current);
    }
}
