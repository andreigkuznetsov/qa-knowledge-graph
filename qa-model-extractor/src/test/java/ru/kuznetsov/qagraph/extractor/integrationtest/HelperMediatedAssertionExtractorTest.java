package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperMediatedAssertionExtractorTest {
    private final IntegrationTestEvidenceExtractor extractor = new IntegrationTestEvidenceExtractor();

    @Test
    void extractsSameClassAndExternalStaticHelperAssertions() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        assertEquals(5, assertions.size());
        assertEquals(List.of(
                        "externalStaticHelper", "helperWithMultipleCategories", "helperWithMultipleCategories",
                        "helperWithMultipleCategories", "sameClassHelper"),
                assertions.stream().map(AssertionEvidence::owningTestMethod).toList());
        assertEquals(List.of(
                        AssertionCategory.RESPONSE_BODY,
                        AssertionCategory.HTTP_STATUS,
                        AssertionCategory.RESPONSE_BODY,
                        AssertionCategory.PERSISTENCE_DATABASE,
                        AssertionCategory.HTTP_STATUS),
                assertions.stream().map(AssertionEvidence::category).toList());
        assertEquals("example.api.HelperMediatedIT",
                assertions.getLast().helperInvocation().helperClass());
        assertEquals("example.support.ResponseAssertions",
                assertions.getFirst().helperInvocation().helperClass());
    }

    @Test
    void preservesParametersInvocationAndBothSourceLocations() throws Exception {
        AssertionEvidence status = extractor.extract(fixtureRepository()).assertions().stream()
                .filter(value -> value.owningTestMethod().equals("sameClassHelper"))
                .findFirst().orElseThrow();

        assertEquals("assertEquals(expectedStatus, response.getStatusCode())", status.expression());
        assertEquals("verifyStatus(response, expectedStatus)",
                status.helperInvocation().invocationExpression());
        assertEquals("verifyStatus", status.helperInvocation().helperMethod());
        assertEquals("src/test/java/example/api/HelperMediatedIT.java", status.repositoryRelativePath());
        assertEquals("src/test/java/example/api/HelperMediatedIT.java",
                status.helperInvocation().repositoryRelativePath());
        assertTrue(status.line() > status.helperInvocation().line());
        assertTrue(status.column() > 0);
        assertTrue(status.helperInvocation().column() > 0);
    }

    @Test
    void omitsUnsupportedChainsRecursiveAndAmbiguousHelpers() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        Set<String> omitted = Set.of("helperChainIgnored", "recursiveHelperIgnored", "ambiguousHelperIgnored");
        assertTrue(assertions.stream().noneMatch(value -> omitted.contains(value.owningTestMethod())));
        assertTrue(assertions.stream().noneMatch(value -> value.expression().contains("unsupportedAssertion")));
    }

    @Test
    void isDeterministicImmutableAndDuplicateFree() throws Exception {
        IntegrationTestEvidence first = extractor.extract(fixtureRepository());
        IntegrationTestEvidence second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(first.assertions().size(), first.assertions().stream().distinct().count());
        assertEquals(6, first.tests().size());
        assertEquals(6, first.httpInteractions().size());
        assertNotNull(first.assertions().getFirst().helperInvocation());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> first.assertions().clear());
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(HelperMediatedAssertionExtractorTest.class.getResource(
                "/fixtures/helper-integration-test-repository").toURI());
    }
}
