package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryEvidenceDiscoveryTest {
    private final RepositoryEvidenceDiscovery discovery = new RepositoryEvidenceDiscovery();

    @TempDir
    Path repository;

    @Test
    void discovers_and_assembles_one_operation() throws Exception {
        writeMain("example/OrdersController.java", controllerWithFlow());

        var result = discover();
        EvidenceGraphProjection operation = result.operationEvidence().getFirst();

        assertEquals(List.of("POST /orders"), operationNames(result));
        assertEquals(1, operation.businessRules().size());
        assertEquals(3, operation.technicalImplementations().size());
        assertTrue(operation.testImplementations().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void discovers_every_operation_in_one_controller() throws Exception {
        writeMain("example/MultiController.java", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class MultiController {
                    @PostMapping("/z-last") String create() { return "created"; }
                    @GetMapping("/a-first") String find() { return "found"; }
                }
                """);

        assertEquals(List.of("GET /a-first", "POST /z-last"), operationNames(discover()));
    }

    @Test
    void discovers_operations_across_multiple_controllers() throws Exception {
        writeMain("example/AlphaController.java", simpleController("AlphaController", "alpha", "/alpha"));
        writeMain("other/BetaController.java", simpleController("BetaController", "beta", "/beta")
                .replace("package example;", "package other;"));

        assertEquals(List.of("GET /alpha", "GET /beta"), operationNames(discover()));
    }

    @Test
    void returns_empty_immutable_output_when_no_supported_operations_exist() throws Exception {
        writeMain("example/PlainService.java", """
                package example;
                class PlainService { String value() { return "value"; } }
                """);

        var result = discover();

        assertTrue(result.operationEvidence().isEmpty());
        assertTrue(result.warnings().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> result.operationEvidence().add(null));
        assertThrows(UnsupportedOperationException.class,
                () -> result.warnings().add("warning"));
    }

    @Test
    void main_only_repository_has_operation_evidence_without_tests() throws Exception {
        writeMain("example/OrdersController.java", simpleController("OrdersController", "orders", "/orders"));

        var operation = discover().operationEvidence().getFirst();

        assertTrue(operation.testImplementations().isEmpty());
        assertTrue(operation.checks().isEmpty());
    }

    @Test
    void main_plus_tests_correlates_rest_assured_and_direct_assertion_evidence() throws Exception {
        writeMain("example/OrdersController.java", simpleController("OrdersController", "orders", "/orders"));
        writeTest("example/OrdersTest.java", """
                package example;
                import org.junit.jupiter.api.Test;
                import static io.restassured.RestAssured.given;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                class OrdersTest {
                    @Test void readsOrders() {
                        given().when().get("/orders").then().statusCode(200);
                        assertTrue(true);
                    }
                }
                """);

        var operation = discover().operationEvidence().getFirst();

        assertEquals(1, operation.testImplementations().size());
        assertEquals(2, operation.checks().size());
    }

    @Test
    void repeated_discovery_is_equal_and_ordered_deterministically() throws Exception {
        writeMain("example/ZController.java", simpleController("ZController", "z", "/z"));
        writeMain("example/AController.java", simpleController("AController", "a", "/a"));

        var first = discover();
        var second = discover();

        assertEquals(first, second);
        assertEquals(List.of("GET /a", "GET /z"), operationNames(first));
    }

    @Test
    void source_locations_are_repository_relative() throws Exception {
        writeMain("example/OrdersController.java", simpleController("OrdersController", "orders", "/orders"));

        var operation = discover().operationEvidence().getFirst();
        var locations = new ArrayList<String>();
        operation.businessOperation().sourceReferences().forEach(reference ->
                locations.add(reference.location().value()));
        operation.technicalImplementations().forEach(node -> node.sourceReferences().forEach(reference ->
                locations.add(reference.location().value())));

        assertFalse(locations.isEmpty());
        assertTrue(locations.stream().allMatch(value -> value.startsWith("src/main/java/")));
        assertTrue(locations.stream().noneMatch(value -> value.contains(repository.toAbsolutePath().toString())));
    }

    @Test
    void unsupported_mappings_and_dynamic_test_paths_remain_omitted() throws Exception {
        writeMain("example/MixedController.java", """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class MixedController {
                    @GetMapping("/supported") String supported() { return "supported"; }
                    @ReadMapping("/custom") String custom() { return "custom"; }
                }
                @interface ReadMapping { String value(); }
                """);
        writeTest("example/MixedTest.java", """
                package example;
                import org.junit.jupiter.api.Test;
                import static io.restassured.RestAssured.given;
                class MixedTest {
                    @Test void dynamicPathIsUnsupported() {
                        String path = "/supported";
                        given().when().get(path).then().statusCode(200);
                    }
                }
                """);

        var result = discover();

        assertEquals(List.of("GET /supported"), operationNames(result));
        assertTrue(result.operationEvidence().getFirst().testImplementations().isEmpty());
        assertTrue(result.operationEvidence().getFirst().checks().isEmpty());
    }

    private RepositoryEvidenceDiscoveryResult discover() throws IOException {
        return discovery.discover(new RepositoryAnalysisRequest(repository, "Focused repository"));
    }

    private static List<String> operationNames(RepositoryEvidenceDiscoveryResult result) {
        return result.operationEvidence().stream()
                .map(graph -> graph.businessOperation().name())
                .toList();
    }

    private void writeMain(String relativePath, String source) throws IOException {
        write("src/main/java/" + relativePath, source);
    }

    private void writeTest(String relativePath, String source) throws IOException {
        write("src/test/java/" + relativePath, source);
    }

    private void write(String relativePath, String source) throws IOException {
        Path target = repository.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, source);
    }

    private static String simpleController(String className, String methodName, String path) {
        return """
                package example;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class %s {
                    @GetMapping("%s") String %s() { return "value"; }
                }
                """.formatted(className, path, methodName);
    }

    private static String controllerWithFlow() {
        return """
                package example;
                import jakarta.validation.constraints.NotBlank;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Repository;
                import org.springframework.stereotype.Service;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class OrdersController {
                    @Autowired OrderService service;
                    @PostMapping("/orders") String create(@RequestBody CreateOrderRequest request) {
                        return service.create();
                    }
                }
                class CreateOrderRequest { @NotBlank String customer; }
                @Service
                class OrderService {
                    @Autowired OrderRepository repository;
                    String create() { return repository.save(); }
                }
                @Repository
                class OrderRepository { String save() { return "saved"; } }
                """;
    }
}
