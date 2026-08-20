package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityNormalizerArchitectureTest {
    @Test void extractorNormalizerIsProjectionOnlyAndModuleDirectionIsOneWay() throws Exception {
        Path root = root();
        String source = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioSourceDeclarationNormalizer.java"));
        assertTrue(source.contains("ScenarioAuthorityNormalizerV1"));
        for (String forbidden : new String[]{"JsonNode", ".path(\"", "authoredTexts", "addSteps(",
                "ScenarioSemanticFingerprintEncoder", "StepSemanticFingerprintEncoder"})
            assertFalse(source.contains(forbidden), forbidden);
        String validationBuild = Files.readString(root.resolve("qa-model-validation-core/build.gradle"));
        assertFalse(validationBuild.contains("qa-evidence-governance-core"));
        try (var files = Files.walk(root.resolve("qa-model-extractor/src/main/java"))) {
            assertTrue(files.filter(path -> path.toString().endsWith(".java")).noneMatch(path -> {
                try { return Files.readString(path).startsWith("package ru.kuznetsov.qaip.evidencegovernance.source;"); }
                catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }));
        }
    }
    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
        return java.util.Objects.requireNonNull(current);
    }
}
