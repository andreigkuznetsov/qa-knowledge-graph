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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectAssertionLibraryEvidenceExtractorTest {
    private final IntegrationTestEvidenceExtractor extractor = new IntegrationTestEvidenceExtractor();

    @Test
    void extractsSupportedJunitAssertJAndHamcrestSubsets() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        assertEquals(33, assertions.size());
        assertEquals(15, count(assertions, AssertionEvidence.AssertionLibrary.JUNIT_5));
        assertEquals(12, count(assertions, AssertionEvidence.AssertionLibrary.ASSERTJ));
        assertEquals(6, count(assertions, AssertionEvidence.AssertionLibrary.HAMCREST));
        assertTrue(assertions.stream().anyMatch(value -> value.assertionKind().equals("assertThrows")));
        assertTrue(assertions.stream().anyMatch(value -> value.assertionKind().equals("containsEntry")));
        assertTrue(assertions.stream().anyMatch(value -> value.assertionKind().equals("not")));
        assertEquals(32, assertions.stream().filter(value ->
                value.category() == AssertionCategory.GENERAL_ASSERTION).count());
        assertEquals(1, assertions.stream().filter(value ->
                value.category() == AssertionCategory.HTTP_STATUS).count());
    }

    @Test
    void preservesMultilineExpressionsLocationsAndHelperOwnership() throws Exception {
        List<AssertionEvidence> assertions = extractor.extract(fixtureRepository()).assertions();

        AssertionEvidence multiline = assertions.stream()
                .filter(value -> value.assertionKind().equals("isEqualTo"))
                .filter(value -> value.expression().contains("org.assertj.core.api.Assertions"))
                .findFirst().orElseThrow();
        assertTrue(multiline.expression().contains("isNotNull().isEqualTo"));
        assertTrue(multiline.repositoryRelativePath().startsWith("src/test/java/"));
        assertTrue(multiline.line() > 0 && multiline.column() > 0);

        List<AssertionEvidence> helper = assertions.stream()
                .filter(value -> value.helperInvocation() != null).toList();
        assertEquals(3, helper.size());
        assertTrue(helper.stream().anyMatch(value ->
                value.owningTestMethod().equals("helperAssertion")
                        && value.helperInvocation().helperMethod().equals("verifyValue")));
        assertTrue(helper.stream().anyMatch(value ->
                value.owningTestMethod().equals("externalHelperAssertion")
                        && value.helperInvocation().helperMethod().equals("verifyExternal")));
        assertNotNull(helper.getFirst().helperInvocation().repositoryRelativePath());
    }

    @Test
    void omitsUnsupportedForeignAndNonJunitPatterns() throws Exception {
        IntegrationTestEvidence evidence = extractor.extract(fixtureRepository());

        assertTrue(evidence.assertions().stream().noneMatch(value ->
                value.expression().contains("unsupportedFluentMethod")
                        || value.expression().contains("foreign entry")
                        || value.expression().contains("customMatcher")));
        assertTrue(evidence.tests().stream().noneMatch(value ->
                value.testMethod().equals("nonJunitMethodIgnored")));
        assertTrue(evidence.tests().stream().anyMatch(value ->
                value.testMethod().equals("assertionWithoutRestInteraction")
                        && value.testStyle() == IntegrationTestStyle.DIRECT_ASSERTION));
        assertTrue(evidence.httpInteractions().stream().noneMatch(value ->
                value.owningTestMethod().equals("assertionWithoutRestInteraction")));
    }

    @Test
    void isDeterministicImmutableAndDuplicateFree() throws Exception {
        IntegrationTestEvidence first = extractor.extract(fixtureRepository());
        IntegrationTestEvidence second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(first.assertions().size(), first.assertions().stream().distinct().count());
        assertEquals(first.assertions().stream().sorted(java.util.Comparator
                        .comparing(AssertionEvidence::owningTestClass)
                        .thenComparing(AssertionEvidence::owningTestMethod)
                        .thenComparingInt(AssertionEvidence::line)
                        .thenComparingInt(AssertionEvidence::column)
                        .thenComparing(value -> value.category().name())
                        .thenComparing(AssertionEvidence::expression)
                        .thenComparing(value -> value.helperInvocation() == null ? "" :
                                value.helperInvocation().helperClass())).toList(),
                first.assertions());
        assertThrows(UnsupportedOperationException.class, () -> first.assertions().clear());
    }

    @Test
    void existingGraphAssemblyProducesChecksAndHasCheckRelationships() throws Exception {
        Path root = fixtureRepository();
        var operation = new SpringMvcRestOperationScanner().scan(root).stream()
                .filter(value -> value.httpMethod() == RestHttpMethod.POST)
                .filter(value -> value.endpointPath().equals("/items"))
                .findFirst().orElseThrow();
        var graph = new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), extractor.extract(root)));

        assertEquals(5, graph.testImplementations().size());
        assertEquals(32, graph.checks().size());
        assertEquals(32, graph.relationships().stream()
                .filter(value -> value.type() == RelationshipType.HAS_CHECK).count());
        assertEquals(5, graph.relationships().stream()
                .filter(value -> value.type() == RelationshipType.USES).count());
    }

    private static long count(
            List<AssertionEvidence> assertions, AssertionEvidence.AssertionLibrary library) {
        return assertions.stream().filter(value -> value.assertionLibrary() == library).count();
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(DirectAssertionLibraryEvidenceExtractorTest.class.getResource(
                "/fixtures/direct-assertion-repository").toURI());
    }
}
