package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisRequest;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryEvidenceDiscovery;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRestTemplateHelperInteractionExtractorTest {
    @TempDir
    Path repository;

    @BeforeEach
    void createRepository() throws Exception {
        write("src/test/java/example/ApiPaths.java", """
                package example;
                final class ApiPaths {
                    static final String CONSTANT = "/api/constant";
                }
                """);
        write("src/test/java/example/NotificationClient.java", """
                package example;
                import org.springframework.boot.test.web.client.TestRestTemplate;
                final class NotificationClient {
                    private final TestRestTemplate restTemplate;
                    NotificationClient(TestRestTemplate restTemplate) { this.restTemplate = restTemplate; }
                    void literal() { restTemplate.postForEntity("api//literal/", null, String.class); }
                    void constant() { restTemplate.postForEntity(ApiPaths.CONSTANT, null, String.class); }
                    void dynamic(String path) { restTemplate.postForEntity(path, null, String.class); }
                    void chained() { delegate(); }
                    void delegate() { restTemplate.postForEntity(ApiPaths.CONSTANT, null, String.class); }
                }
                """);
        write("src/test/java/example/BaseTest.java", """
                package example;
                abstract class BaseTest { NotificationClient client; }
                """);
        write("src/test/java/example/HelperHttpTest.java", """
                package example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                class HelperHttpTest extends BaseTest {
                    @Test void literalCall() { client.literal(); assertTrue(true); }
                    @Test void constantCall() { client.constant(); assertTrue(true); }
                    @Test void firstSharedCall() { client.constant(); assertTrue(true); }
                    @Test void secondSharedCall() { client.constant(); assertTrue(true); }
                    @Test void dynamicIgnored() { client.dynamic(System.getProperty("path")); assertTrue(true); }
                    @Test void unresolvedIgnored() { client.missing(); assertTrue(true); }
                    @Test void helperChainIgnored() { client.chained(); assertTrue(true); }
                }
                """);
    }

    @Test
    void extracts_one_level_helper_calls_with_literal_and_static_constant() throws Exception {
        IntegrationTestEvidence evidence = new IntegrationTestEvidenceExtractor().extract(repository);

        assertEquals(List.of("constantCall", "firstSharedCall", "literalCall", "secondSharedCall"),
                evidence.httpInteractions().stream().map(HttpInteractionEvidence::owningTestMethod).toList());
        assertEquals(List.of("/api/constant", "/api/constant", "/api/literal", "/api/constant"),
                evidence.httpInteractions().stream().map(HttpInteractionEvidence::endpointPath).toList());
        assertTrue(evidence.httpInteractions().stream()
                .allMatch(interaction -> interaction.httpMethod() == IntegrationHttpMethod.POST));
        assertTrue(evidence.httpInteractions().stream()
                .allMatch(interaction -> interaction.owningTestClass().equals("example.HelperHttpTest")));
        assertTrue(evidence.tests().stream()
                .filter(test -> Set.of("constantCall", "firstSharedCall", "literalCall", "secondSharedCall")
                        .contains(test.testMethod()))
                .allMatch(test -> test.testStyle() == IntegrationTestStyle.TEST_REST_TEMPLATE));
    }

    @Test
    void safely_ignores_dynamic_unresolved_and_second_helper_level() throws Exception {
        IntegrationTestEvidence evidence = new IntegrationTestEvidenceExtractor().extract(repository);

        assertTrue(evidence.httpInteractions().stream().noneMatch(interaction -> Set.of(
                        "dynamicIgnored", "unresolvedIgnored", "helperChainIgnored")
                .contains(interaction.owningTestMethod())));
    }

    @Test
    void operation_assembly_creates_tests_uses_and_checks() throws Exception {
        write("src/main/java/example/NotificationController.java", """
                package example;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class NotificationController {
                    @PostMapping("/api/constant") void create() { }
                }
                """);

        var discovered = new RepositoryEvidenceDiscovery().discover(
                new RepositoryAnalysisRequest(repository, "Helper HTTP repository"));
        var operation = discovered.operationEvidence().getFirst();

        assertEquals(3, operation.testImplementations().size());
        assertEquals(3, operation.relationships().stream()
                .filter(relationship -> relationship.type() == RelationshipType.USES)
                .filter(relationship -> relationship.from().startsWith("TEST-AUTO-"))
                .count());
        assertEquals(3, operation.relationships().stream()
                .filter(relationship -> relationship.type() == RelationshipType.HAS_CHECK)
                .count());
    }

    private void write(String relativePath, String source) throws Exception {
        Path target = repository.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, source);
    }
}
