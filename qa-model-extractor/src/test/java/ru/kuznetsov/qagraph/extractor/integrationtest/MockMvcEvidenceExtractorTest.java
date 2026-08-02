package ru.kuznetsov.qagraph.extractor.integrationtest;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockMvcEvidenceExtractorTest {
    private final IntegrationTestEvidenceExtractor extractor = new IntegrationTestEvidenceExtractor();

    @Test
    void extractsLiteralConstantEnumAndQualifiedMockMvcRequests() throws Exception {
        IntegrationTestEvidence evidence = extractor.extract(fixtureRepository());

        assertEquals(5, evidence.httpInteractions().size());
        assertEquals(List.of("directGetWithMultipleAssertions", "externalStaticHelper",
                        "qualifiedPostWithConstantAndPersistenceCheck", "sameClassHelper",
                        "unsupportedMatcherIgnored"),
                evidence.httpInteractions().stream().map(HttpInteractionEvidence::owningTestMethod).toList());
        HttpInteractionEvidence get = evidence.httpInteractions().getFirst();
        assertEquals(IntegrationHttpMethod.GET, get.httpMethod());
        assertEquals("/api/items/{id}", get.endpointPath());
        assertEquals("ApiPath.ITEM.getPath()", get.sourceExpression());
        assertTrue(get.invocationDetails().contains("queryParam")
                && get.invocationDetails().contains("header"));
        HttpInteractionEvidence post = evidence.httpInteractions().stream()
                .filter(value -> value.owningTestMethod().equals("qualifiedPostWithConstantAndPersistenceCheck"))
                .findFirst().orElseThrow();
        assertEquals("CREATE_PATH", post.sourceExpression());
        assertTrue(post.invocationDetails().contains("content")
                && post.invocationDetails().contains("header"));
    }

    @Test
    void extractsStatusBodyHeaderContentPersistenceAndHelperAssertions() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        assertEquals(10, assertions.size());
        assertEquals(4, assertions.stream().filter(value ->
                value.category() == AssertionCategory.HTTP_STATUS).count());
        assertEquals(5, assertions.stream().filter(value ->
                value.category() == AssertionCategory.RESPONSE_BODY).count());
        assertEquals(1, assertions.stream().filter(value ->
                value.category() == AssertionCategory.PERSISTENCE_DATABASE).count());
        assertEquals(3, assertions.stream().filter(value -> value.helperInvocation() != null).count());
        assertTrue(assertions.stream().anyMatch(value -> value.expression().contains("jsonPath")));
        assertTrue(assertions.stream().anyMatch(value -> value.expression().contains("content")));
        assertTrue(assertions.stream().anyMatch(value -> value.expression().contains("header")));
    }

    @Test
    void omitsDynamicInteractionUnsupportedMatcherAndNonMockMvcClient() throws Exception {
        IntegrationTestEvidence evidence = extractor.extract(fixtureRepository());

        assertTrue(evidence.httpInteractions().stream().noneMatch(value ->
                value.owningTestMethod().equals("dynamicEndpointIgnored")));
        assertTrue(evidence.assertions().stream().noneMatch(value ->
                value.expression().contains("customMatcher")));
        assertTrue(evidence.tests().stream().noneMatch(value ->
                value.testMethod().equals("nonMockMvcClientIgnored")
                        || value.testMethod().equals("foreignClientIgnored")));
    }

    @Test
    void isStableImmutableDuplicateFreeAndPreservesMockMvcLocationsAndStyle() throws Exception {
        IntegrationTestEvidence first = extractor.extract(fixtureRepository());
        IntegrationTestEvidence second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(first.tests().size(), first.tests().stream().distinct().count());
        assertEquals(first.httpInteractions().size(), first.httpInteractions().stream().distinct().count());
        assertEquals(first.assertions().size(), first.assertions().stream().distinct().count());
        assertTrue(first.tests().stream().allMatch(value -> value.testStyle() == IntegrationTestStyle.MOCK_MVC));
        assertTrue(first.httpInteractions().stream().allMatch(value ->
                value.repositoryRelativePath().startsWith("src/test/java/")
                        && value.line() > 0 && value.column() > 0));
        assertNotNull(first.assertions().stream().filter(value -> value.helperInvocation() != null)
                .findFirst().orElseThrow().helperInvocation());
        assertThrows(UnsupportedOperationException.class, () -> first.httpInteractions().clear());
    }

    @Test
    void existingAssemblerProducesTestChecksUsesAndHasCheck() throws Exception {
        Path root = fixtureRepository();
        var operation = new SpringMvcRestOperationScanner().scan(root).stream()
                .filter(value -> value.httpMethod() == RestHttpMethod.POST)
                .filter(value -> value.endpointPath().equals("/api/items"))
                .findFirst().orElseThrow();
        IntegrationTestEvidence evidence = extractor.extract(root);

        var graph = new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), evidence));

        assertEquals(4, graph.testImplementations().size());
        assertEquals(5, graph.checks().size());
        assertEquals(10, graph.relationships().size());
        assertEquals(Set.of(RelationshipType.IMPLEMENTED_BY, RelationshipType.USES,
                        RelationshipType.HAS_CHECK),
                graph.relationships().stream().map(value -> value.type())
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(graph.testImplementations().stream().allMatch(value ->
                value.description().contains("MockMvc")));
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(MockMvcEvidenceExtractorTest.class.getResource(
                "/fixtures/mockmvc-reference-repository").toURI());
    }
}
