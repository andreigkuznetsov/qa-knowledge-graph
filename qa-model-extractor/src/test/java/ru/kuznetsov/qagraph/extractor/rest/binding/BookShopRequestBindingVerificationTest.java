package ru.kuznetsov.qagraph.extractor.rest.binding;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidenceExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookShopRequestBindingVerificationTest {
    @Test
    void registrationBindingAddsValidationRulesWithoutManualModelTypes() throws Exception {
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
        var bindings = new ControllerRequestModelBindingExtractor().extract(repository, operation);
        var validations = new BeanValidationEvidenceExtractor().extract(repository);
        var assembler = new OperationEvidenceGraphAssembler();
        var withoutBindings = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), validations,
                new IntegrationTestEvidence(List.of(), List.of(), List.of())));
        var withBindings = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, bindings, validations,
                new IntegrationTestEvidence(List.of(), List.of(), List.of())));

        assertEquals(1, bindings.size());
        assertEquals("bookShop.model.request.RegisterRequest", bindings.getFirst().resolvedModelType());
        assertEquals(RequestBindingKind.REQUEST_BODY, bindings.getFirst().bindingKind());
        assertEquals(Set.of(ValidationActivation.VALID), bindings.getFirst().validationActivations());
        assertTrue(withoutBindings.businessRules().isEmpty());
        assertTrue(withBindings.businessRules().size() > 0);
        assertTrue(withBindings.businessRules().stream().allMatch(rule ->
                rule.name().contains("bookShop.model.request.RegisterRequest")));
        System.out.printf("BOOKSHOP_M6_3 bindings=%d beforeRules=%d afterRules=%d%n",
                bindings.size(), withoutBindings.businessRules().size(), withBindings.businessRules().size());
    }
}
