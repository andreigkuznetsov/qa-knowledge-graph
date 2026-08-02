package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.Test;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationTestEvidenceExtractorTest {
    private final IntegrationTestEvidenceExtractor extractor = new IntegrationTestEvidenceExtractor();

    @Test
    void extractsJUnitFiveRestAssuredTestsAndExplicitDisplayNames() throws Exception {
        List<TestImplementationEvidence> tests = extractor.extract(fixtureRepository()).tests();

        assertEquals(List.of("dynamicPathIgnored", "registerRoles", "registerSuccessfully"),
                tests.stream().map(TestImplementationEvidence::testMethod).toList());
        assertEquals("register supported roles", tests.get(1).displayName());
        assertEquals("register user through API", tests.get(2).displayName());
    }

    @Test
    void extractsOnlyStaticallyDeterminedRestAssuredInteractions() throws Exception {
        List<HttpInteractionEvidence> interactions = extractor.extract(fixtureRepository()).httpInteractions();

        assertEquals(2, interactions.size());
        assertEquals(List.of(IntegrationHttpMethod.POST, IntegrationHttpMethod.POST),
                interactions.stream().map(HttpInteractionEvidence::httpMethod).toList());
        assertEquals(List.of("/auth/register", "/auth/register"),
                interactions.stream().map(HttpInteractionEvidence::endpointPath).toList());
        assertEquals(List.of("registerRoles", "registerSuccessfully"),
                interactions.stream().map(HttpInteractionEvidence::owningTestMethod).toList());
        assertTrue(interactions.stream().noneMatch(interaction ->
                interaction.owningTestMethod().equals("dynamicPathIgnored")));
    }

    @Test
    void extractsSeparateStatusBodyAndDatabaseAssertions() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        assertEquals(4, assertions.size());
        assertEquals(List.of(
                AssertionCategory.HTTP_STATUS,
                AssertionCategory.HTTP_STATUS,
                AssertionCategory.RESPONSE_BODY,
                AssertionCategory.PERSISTENCE_DATABASE),
                assertions.stream().map(AssertionEvidence::category).toList());
        assertEquals("DatabaseAssertions.assertPersisted(userRepository, \"generated-id\")",
                assertions.get(3).expression());
    }

    @Test
    void ignoresNonJUnitAndUnsupportedTestStyles() throws Exception {
        IntegrationTestEvidence evidence = extractor.extract(fixtureRepository());

        assertTrue(evidence.tests().stream().noneMatch(test ->
                test.testMethod().equals("helperMethodIsNotATest")));
        assertTrue(evidence.tests().stream().noneMatch(test ->
                test.testMethod().equals("mockMvcStyleIsUnsupported")));
        assertTrue(evidence.tests().stream().noneMatch(test ->
                test.testMethod().equals("sameNamedClientIsUnsupported")));
        assertTrue(evidence.tests().stream().noneMatch(test ->
                test.testMethod().equals("junitFourIsUnsupported")));
    }

    @Test
    void returnsImmutableStableEvidenceWithStableLocationsAndNoDuplicates() throws Exception {
        IntegrationTestEvidence first = extractor.extract(fixtureRepository());
        IntegrationTestEvidence second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(3, first.tests().stream().distinct().count());
        assertEquals(2, first.httpInteractions().stream().distinct().count());
        assertEquals(4, first.assertions().stream().distinct().count());
        assertThrowsUnsupportedMutation(first);

        TestImplementationEvidence success = first.tests().get(2);
        assertEquals("example.api.RegistrationApiIT", success.testClass());
        assertEquals("src/test/java/example/api/RegistrationApiIT.java", success.repositoryRelativePath());
        assertEquals(12, success.line());
        assertEquals(5, success.column());

        HttpInteractionEvidence interaction = first.httpInteractions().get(1);
        assertEquals(18, interaction.line());
        assertEquals(18, interaction.column());
    }

    private static void assertThrowsUnsupportedMutation(IntegrationTestEvidence evidence) {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> evidence.tests().add(evidence.tests().getFirst()));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> evidence.httpInteractions().clear());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> evidence.assertions().clear());
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(IntegrationTestEvidenceExtractorTest.class.getResource(
                "/fixtures/integration-test-repository").toURI());
    }
}
