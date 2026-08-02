package ru.kuznetsov.qagraph.extractor.rest.binding;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidenceExtractor;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerRequestModelBindingExtractorTest {
    private final SpringMvcRestOperationScanner scanner = new SpringMvcRestOperationScanner();
    private final ControllerRequestModelBindingExtractor extractor =
            new ControllerRequestModelBindingExtractor();

    @Test
    void extractsExplicitImplicitRecordAndContainerBindings() throws Exception {
        List<RequestModelBindingEvidence> bindings = allBindings();

        assertEquals(List.of(
                        "array|REQUEST_BODY|CreateRequest[]|example.model.CreateRequest",
                        "create|REQUEST_BODY|CreateRequest|example.model.CreateRequest",
                        "filter|MODEL_ATTRIBUTE|FilterRequest|example.model.FilterRequest",
                        "fullyQualified|REQUEST_BODY|example.model.CreateRequest|example.model.CreateRequest",
                        "implicit|IMPLICIT|ImplicitRequest|example.model.ImplicitRequest",
                        "list|REQUEST_BODY|List<CreateRequest>|example.model.CreateRequest",
                        "record|REQUEST_BODY|RecordRequest|example.model.RecordRequest"),
                bindings.stream().map(binding -> binding.controllerMethod() + '|'
                        + binding.bindingKind() + '|' + binding.declaredParameterType() + '|'
                        + binding.resolvedModelType()).sorted().toList());
    }

    @Test
    void preservesValidationActivationAndParameterLocation() throws Exception {
        List<RequestModelBindingEvidence> bindings = allBindings();
        RequestModelBindingEvidence create = find(bindings, "create");
        RequestModelBindingEvidence filter = find(bindings, "filter");

        assertEquals(Set.of(ValidationActivation.VALID), create.validationActivations());
        assertEquals(Set.of(ValidationActivation.VALIDATED), filter.validationActivations());
        assertEquals("request", create.parameterName());
        assertEquals("example.api.RequestBindingController", create.controllerClass());
        assertEquals("src/main/java/example/api/RequestBindingController.java",
                create.repositoryRelativePath());
        assertTrue(create.line() > 0);
        assertTrue(create.column() > 0);
    }

    @Test
    void excludesTransportInfrastructureUnresolvedAndAmbiguousParameters() throws Exception {
        List<RequestModelBindingEvidence> bindings = allBindings();

        assertTrue(bindings.stream().noneMatch(binding -> Set.of(
                "id", "query", "header", "cookie", "servletRequest", "principal", "request")
                .contains(binding.parameterName()) && binding.controllerMethod().equals("create")
                && !binding.resolvedModelType().equals("example.model.CreateRequest")));
        assertTrue(bindings.stream().noneMatch(binding -> binding.controllerMethod().equals("missing")));
        assertTrue(bindings.stream().noneMatch(binding -> binding.controllerMethod().equals("ambiguous")));
    }

    @Test
    void isDeterministicImmutableDuplicateFreeAndDoesNotMutateSourceOrInput() throws Exception {
        Path root = fixtureRepository();
        RestOperationEvidence operation = operation("POST", "/api/class");
        RestOperationEvidence unchanged = operation;
        Path source = root.resolve("src/main/java/example/api/RequestBindingController.java");
        byte[] before = Files.readAllBytes(source);

        List<RequestModelBindingEvidence> first = extractor.extract(root, operation);
        List<RequestModelBindingEvidence> second = extractor.extract(root, operation);

        assertEquals(first, second);
        assertEquals(first.size(), first.stream().distinct().count());
        assertEquals(unchanged, operation);
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(source)));
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
    }

    @Test
    void feedsExistingValidationAndAssemblyWithoutManualTypeSet() throws Exception {
        Path root = fixtureRepository();
        RestOperationEvidence operation = operation("POST", "/api/class");
        var bindings = extractor.extract(root, operation);
        var validations = new BeanValidationEvidenceExtractor().extract(root);

        var graph = new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, bindings, validations, new IntegrationTestEvidence(List.of(), List.of(), List.of())));

        assertEquals(1, graph.businessRules().size());
        assertTrue(graph.businessRules().getFirst().name().contains("example.model.CreateRequest.name"));
        assertTrue(graph.relationships().stream().anyMatch(relationship ->
                relationship.type() == RelationshipType.GOVERNED_BY));
    }

    @Test
    void rejectsNullInputs() throws Exception {
        assertThrows(NullPointerException.class, () -> extractor.extract(null, operation("POST", "/api/class")));
        assertThrows(NullPointerException.class, () -> extractor.extract(fixtureRepository(), null));
    }

    private List<RequestModelBindingEvidence> allBindings() throws Exception {
        List<RequestModelBindingEvidence> result = new java.util.ArrayList<>();
        for (RestOperationEvidence operation : scanner.scan(fixtureRepository())) {
            result.addAll(extractor.extract(fixtureRepository(), operation));
        }
        return result.stream().distinct().sorted(java.util.Comparator
                .comparing(RequestModelBindingEvidence::controllerMethod)
                .thenComparing(RequestModelBindingEvidence::parameterName)).toList();
    }

    private RestOperationEvidence operation(String method, String path) throws Exception {
        return scanner.scan(fixtureRepository()).stream()
                .filter(value -> value.httpMethod().name().equals(method))
                .filter(value -> value.endpointPath().equals(path))
                .findFirst().orElseThrow();
    }

    private static RequestModelBindingEvidence find(
            List<RequestModelBindingEvidence> values, String method) {
        return values.stream().filter(value -> value.controllerMethod().equals(method))
                .findFirst().orElseThrow();
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(ControllerRequestModelBindingExtractorTest.class.getResource(
                "/fixtures/request-binding-repository").toURI());
    }
}
