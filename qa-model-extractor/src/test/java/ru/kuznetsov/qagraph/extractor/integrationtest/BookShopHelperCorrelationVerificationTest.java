package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.extractor.rest.binding.ControllerRequestModelBindingExtractor;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidenceExtractor;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookShopHelperCorrelationVerificationTest {
    @Test
    void helperAssertionsAddRegistrationChecksThroughExistingAssembler() throws Exception {
        String configured = System.getenv("BOOKSHOP_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "BOOKSHOP_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/test/java")),
                "BookShop test sources are unavailable");

        var operation = new SpringMvcRestOperationScanner().scan(repository).stream()
                .filter(value -> value.httpMethod() == RestHttpMethod.POST)
                .filter(value -> value.endpointPath().equals("/auth/register"))
                .findFirst().orElseThrow();
        var validations = new BeanValidationEvidenceExtractor().extract(repository);
        var bindings = new ControllerRequestModelBindingExtractor().extract(repository, operation);
        IntegrationTestEvidence afterEvidence = new IntegrationTestEvidenceExtractor().extract(repository);
        IntegrationTestEvidence beforeEvidence = new IntegrationTestEvidence(
                afterEvidence.tests(),
                afterEvidence.httpInteractions(),
                afterEvidence.assertions().stream()
                        .filter(value -> value.helperInvocation() == null)
                        .toList());
        var assembler = new OperationEvidenceGraphAssembler();
        var before = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, bindings, validations, beforeEvidence));
        var after = assembler.assemble(new OperationEvidenceAssemblyRequest(
                operation, bindings, validations, afterEvidence));

        long correlatedHelperAssertions = afterEvidence.assertions().stream()
                .filter(value -> value.helperInvocation() != null)
                .filter(value -> afterEvidence.httpInteractions().stream().anyMatch(interaction ->
                        interaction.owningTestClass().equals(value.owningTestClass())
                                && interaction.owningTestMethod().equals(value.owningTestMethod())
                                && interaction.httpMethod() == IntegrationHttpMethod.POST
                                && interaction.endpointPath().equals("/auth/register")))
                .count();
        System.out.printf(
                "BOOKSHOP_M6_2 beforeTests=%d beforeChecks=%d afterTests=%d afterChecks=%d helperAssertions=%d%n",
                before.testImplementations().size(), before.checks().size(),
                after.testImplementations().size(), after.checks().size(), correlatedHelperAssertions);

        assertTrue(correlatedHelperAssertions > 0);
        assertTrue(after.testImplementations().size() > 0);
        assertTrue(after.checks().size() > 0);
        assertTrue(after.checks().size() > before.checks().size());
        assertTrue(after.relationships().stream().anyMatch(value -> value.type() == RelationshipType.USES));
        assertTrue(after.relationships().stream().anyMatch(value -> value.type() == RelationshipType.HAS_CHECK));
    }
}
