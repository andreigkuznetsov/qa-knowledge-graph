package ru.kuznetsov.qagraph.validationcore.scenarioauthority;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthoritySchemaOwnershipTest {
    @Test void validationCoreIsTheOnlySchemaAndValidatorOwner() throws Exception {
        Path root = Path.of(System.getProperty("qaip.repositoryRoot"));
        String resourceName = "qaip-scenario-authority-manifest-v1.schema.json";
        try (var paths = Files.walk(root)) {
            var resources = paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(resourceName))
                    .filter(p -> p.toString().contains("src\\main\\resources")
                            || p.toString().contains("src/main/resources"))
                    .toList();
            assertEquals(List.of(root.resolve(
                    "qa-model-validation-core/src/main/resources/schemas/" + resourceName)), resources);
        }
        Path extractor = root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/qagraph/extractor/"
                + "repositoryanalysis/ScenarioManifestSchemaValidator.java");
        String source = Files.readString(extractor);
        assertTrue(source.contains("ScenarioAuthorityManifestSchemaValidatorV1"));
        assertFalse(source.contains("JsonSchemaFactory"));
        assertFalse(source.contains("getResourceAsStream"));
        String build = Files.readString(root.resolve("qa-model-validation-core/build.gradle"));
        assertFalse(build.contains("qa-model-extractor"));
        assertFalse(build.contains("qa-evidence-governance-core"));
    }
}
