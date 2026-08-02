package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookShopStaticEndpointVerificationTest {
    @Test
    void resolvesRegistrationEnumEndpointInRealBookShopTests() throws Exception {
        String configured = System.getenv("BOOKSHOP_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "BOOKSHOP_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/test/java")),
                "BookShop test sources are unavailable");

        IntegrationTestEvidence evidence = new IntegrationTestEvidenceExtractor().extract(repository);

        assertTrue(evidence.httpInteractions().stream().anyMatch(interaction ->
                interaction.httpMethod() == IntegrationHttpMethod.POST
                        && interaction.endpointPath().equals("/auth/register")
                        && interaction.sourceExpression().equals("AUTH_REGISTER.getPath()")
                        && interaction.owningTestClass().endsWith("AuthRegisterValidationIT")));
    }
}
