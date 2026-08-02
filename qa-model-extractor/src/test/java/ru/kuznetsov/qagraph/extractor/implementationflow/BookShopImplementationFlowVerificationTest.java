package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookShopImplementationFlowVerificationTest {
    @Test
    void registrationProducesControllerServiceRepositoryFlowWithoutHardcodedTargets() throws Exception {
        String configured = System.getenv("BOOKSHOP_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "BOOKSHOP_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/main/java")),
                "BookShop production sources are unavailable");

        var operation = new SpringMvcRestOperationScanner().scan(repository).stream()
                .filter(value -> value.httpMethod() == RestHttpMethod.POST)
                .filter(value -> value.endpointPath().equals("/auth/register"))
                .findFirst().orElseThrow();
        var flow = new DirectImplementationFlowExtractor().extract(repository, operation).orElseThrow();
        var graph = new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(),
                new IntegrationTestEvidence(List.of(), List.of(), List.of()), List.of(flow)));

        assertEquals(operation.javaPackage() + '.' + operation.controllerClass(), flow.controllerClass());
        assertEquals(operation.controllerMethod(), flow.controllerMethod());
        assertTrue(!flow.serviceClass().isBlank() && !flow.serviceMethod().isBlank());
        assertTrue(!flow.repositoryClass().isBlank() && !flow.repositoryMethod().isBlank());
        assertEquals(3, graph.technicalImplementations().size());
        assertEquals(List.of("CONTROLLER", "SERVICE", "REPOSITORY"), graph.technicalImplementations().stream()
                .map(value -> value.technicalImplementation().details().getOrDefault("flowStage", "CONTROLLER"))
                .toList());
        assertEquals(2, graph.relationships().stream()
                .filter(value -> value.type() == RelationshipType.USES).count());
        System.out.printf("BOOKSHOP_M6_4 controller=%s#%s service=%s#%s repository=%s#%s%n",
                flow.controllerClass(), flow.controllerMethod(), flow.serviceClass(), flow.serviceMethod(),
                flow.repositoryClass(), flow.repositoryMethod());
    }
}
