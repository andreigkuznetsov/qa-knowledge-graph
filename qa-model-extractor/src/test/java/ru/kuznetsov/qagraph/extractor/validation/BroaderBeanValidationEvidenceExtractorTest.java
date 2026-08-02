package ru.kuznetsov.qagraph.extractor.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.extractor.rest.binding.ControllerRequestModelBindingExtractor;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroaderBeanValidationEvidenceExtractorTest {
    private static final Set<String> NEW_ANNOTATIONS = Set.of(
            "Null", "AssertTrue", "AssertFalse", "Positive", "PositiveOrZero", "Negative",
            "NegativeOrZero", "DecimalMin", "DecimalMax", "Digits", "Past", "PastOrPresent",
            "Future", "FutureOrPresent");
    private final BeanValidationEvidenceExtractor extractor = new BeanValidationEvidenceExtractor();

    @Test
    void extractsEveryNewConstraintFromJakartaAndJavaxPackages() throws Exception {
        List<BeanValidationEvidence> evidence = extractor.extract(fixtureRepository());

        for (String annotation : NEW_ANNOTATIONS) {
            assertEquals(Set.of("jakarta.validation.constraints", "javax.validation.constraints"),
                    evidence.stream().filter(value -> value.annotationType().endsWith('.' + annotation))
                            .map(value -> value.annotationType().substring(
                                    0, value.annotationType().lastIndexOf('.')))
                            .collect(java.util.stream.Collectors.toSet()), annotation);
        }
    }

    @Test
    void extractsFieldsRecordComponentsMethodParametersAndMultipleConstraintsSeparately() throws Exception {
        List<BeanValidationEvidence> evidence = extractor.extract(fixtureRepository());

        assertEquals(30, evidence.size());
        assertEquals(2, evidence.stream()
                .filter(value -> value.owningJavaType().equals("example.request.JakartaBroadRequest"))
                .filter(value -> value.memberName().equals("positiveOrZero"))
                .count());
        BeanValidationEvidence methodParameter = find(evidence,
                "example.request.JakartaBroadRequest", "adjustment", "Positive");
        assertEquals("adjustment must be positive", methodParameter.explicitMessage());
        assertTrue(evidence.stream().anyMatch(value ->
                value.owningJavaType().equals("example.request.JavaxBroadRecord")
                        && value.memberName().equals("futureOrPresent")));
    }

    @Test
    void preservesOnlyExplicitAttributesInDeterministicOrder() throws Exception {
        List<BeanValidationEvidence> evidence = extractor.extract(fixtureRepository());
        BeanValidationEvidence minimum = find(evidence,
                "example.request.JakartaBroadRequest", "decimalMin", "DecimalMin");
        BeanValidationEvidence digits = find(evidence,
                "example.request.JakartaBroadRequest", "digits", "Digits");
        BeanValidationEvidence past = find(evidence,
                "example.request.JakartaBroadRequest", "past", "Past");

        assertEquals(Map.of("inclusive", "false", "message", "\"above minimum\"", "value", "\"0.10\""),
                minimum.declaredAttributes());
        assertEquals("above minimum", minimum.explicitMessage());
        assertEquals(List.of("fraction", "groups", "integer", "message", "payload"),
                digits.declaredAttributes().keySet().stream().toList());
        assertEquals("{ Create.class, Update.class }", digits.declaredAttributes().get("groups"));
        assertEquals("Severity.class", digits.declaredAttributes().get("payload"));
        assertNull(past.explicitMessage());
        assertTrue(past.declaredAttributes().isEmpty());
    }

    @Test
    void ignoresForeignHibernateAndCustomAnnotations() throws Exception {
        assertTrue(extractor.extract(fixtureRepository()).stream().noneMatch(value ->
                value.owningJavaType().equals("example.request.UnsupportedValidationRequest")));
    }

    @Test
    void isDeterministicImmutableDuplicateFreeAndLocationStable() throws Exception {
        List<BeanValidationEvidence> first = extractor.extract(fixtureRepository());
        List<BeanValidationEvidence> second = extractor.extract(fixtureRepository());

        assertEquals(first, second);
        assertEquals(first.size(), first.stream().distinct().count());
        assertTrue(first.stream().allMatch(value ->
                value.repositoryRelativePath().startsWith("src/main/java/")
                        && value.line() > 0 && value.column() > 0));
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
    }

    @Test
    void existingBindingAndAssemblyProjectEveryBoundOccurrenceAsOneRule() throws Exception {
        Path root = fixtureRepository();
        var operation = new SpringMvcRestOperationScanner().scan(root).stream()
                .filter(value -> value.endpointPath().equals("/broad"))
                .findFirst().orElseThrow();
        var bindings = new ControllerRequestModelBindingExtractor().extract(root, operation);
        var validations = extractor.extract(root);

        var graph = new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, bindings, validations,
                new IntegrationTestEvidence(List.of(), List.of(), List.of())));

        assertEquals(1, bindings.size());
        assertEquals("example.request.JakartaBroadRequest", bindings.getFirst().resolvedModelType());
        assertEquals(16, graph.businessRules().size());
        assertEquals(16, graph.relationships().stream()
                .filter(value -> value.type() == RelationshipType.GOVERNED_BY).count());
    }

    private static BeanValidationEvidence find(
            List<BeanValidationEvidence> evidence, String owner, String member, String annotation) {
        return evidence.stream()
                .filter(value -> value.owningJavaType().equals(owner))
                .filter(value -> value.memberName().equals(member))
                .filter(value -> value.annotationType().endsWith('.' + annotation))
                .findFirst().orElseThrow();
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(BroaderBeanValidationEvidenceExtractorTest.class.getResource(
                "/fixtures/broader-validation-repository").toURI());
    }
}
