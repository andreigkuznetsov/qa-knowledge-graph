package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRuntimePortTestRestTemplateExtractorTest {
    @TempDir
    Path repository;

    @BeforeEach
    void createRepository() throws Exception {
        Path source = repository.resolve("src/test/java/example/LocalHttpTest.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;
                import org.junit.jupiter.api.Test;
                import org.springframework.boot.test.web.client.TestRestTemplate;
                import org.springframework.http.HttpMethod;
                class LocalHttpTest {
                    private static final String API_ORDERS = "/api/orders";
                    private TestRestTemplate restTemplate;
                    private int port;
                    private String dynamicPath;

                    @Test void localhost() {
                        restTemplate.postForEntity("http://localhost:" + port + "/api/orders", null, String.class);
                    }
                    @Test void loopback() {
                        restTemplate.postForEntity("http://127.0.0.1:" + port + "/api/orders", null, String.class);
                    }
                    @Test void pathConstant() {
                        restTemplate.postForEntity("http://localhost:" + port + API_ORDERS, null, String.class);
                    }
                    @Test void externalIgnored() {
                        restTemplate.postForEntity("http://orders.company.internal:" + port + "/api/orders",
                                null, String.class);
                    }
                    @Test void dynamicPathIgnored() {
                        restTemplate.postForEntity("http://localhost:" + port + dynamicPath, null, String.class);
                    }
                    @Test void queryIgnored() {
                        restTemplate.postForEntity("http://localhost:" + port + "/api/orders?full=true",
                                null, String.class);
                    }
                    @Test void fragmentIgnored() {
                        restTemplate.postForEntity("http://localhost:" + port + "/api/orders#created",
                                null, String.class);
                    }
                    @Test void unresolvedMethodIgnored(HttpMethod method) {
                        restTemplate.exchange("http://localhost:" + port + "/api/orders",
                                method, null, String.class);
                    }
                }
                """);
    }

    @Test
    void resolves_only_bounded_local_runtime_port_urls() throws Exception {
        IntegrationTestEvidence evidence = new IntegrationTestEvidenceExtractor().extract(repository);

        assertEquals(List.of("localhost", "loopback", "pathConstant"),
                evidence.httpInteractions().stream().map(HttpInteractionEvidence::owningTestMethod).toList());
        assertTrue(evidence.httpInteractions().stream()
                .allMatch(interaction -> interaction.httpMethod() == IntegrationHttpMethod.POST));
        assertTrue(evidence.httpInteractions().stream()
                .allMatch(interaction -> interaction.endpointPath().equals("/api/orders")));
        assertTrue(evidence.httpInteractions().stream().allMatch(interaction ->
                interaction.line() > 0 && interaction.column() > 0));
    }

    @Test
    void safely_rejects_external_dynamic_path_query_and_unresolved_method_forms() throws Exception {
        IntegrationTestEvidence evidence = new IntegrationTestEvidenceExtractor().extract(repository);

        assertTrue(evidence.httpInteractions().stream().noneMatch(interaction -> Set.of(
                        "externalIgnored", "dynamicPathIgnored", "queryIgnored", "fragmentIgnored",
                        "unresolvedMethodIgnored")
                .contains(interaction.owningTestMethod())));
    }
}
