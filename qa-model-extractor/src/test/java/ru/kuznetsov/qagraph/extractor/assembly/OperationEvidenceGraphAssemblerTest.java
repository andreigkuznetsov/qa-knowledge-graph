package ru.kuznetsov.qagraph.extractor.assembly;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionCategory;
import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.HttpInteractionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationHttpMethod;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.TestImplementationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;
import ru.kuznetsov.qagraph.extractor.rest.binding.RequestBindingKind;
import ru.kuznetsov.qagraph.extractor.rest.binding.RequestModelBindingEvidence;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationEvidenceGraphAssemblerTest {
    private static final String REGISTER_OPERATION_ID =
            "BO-REST-D73C690C07E2E205BAAB91B1FA6C54081AC5695C69AED9E5A57D1192CE269363";

    private final OperationEvidenceGraphAssembler assembler = new OperationEvidenceGraphAssembler();

    @Test
    void assemblesOneOperationRulesImplementationTestAndChecks() {
        EvidenceGraphProjection graph = assembler.assemble(request());

        assertEquals(REGISTER_OPERATION_ID, graph.businessOperation().id());
        assertEquals(NodeType.BUSINESS_OPERATION, graph.businessOperation().type());
        assertEquals(2, graph.businessRules().size());
        assertEquals(1, graph.technicalImplementations().size());
        assertEquals(1, graph.testImplementations().size());
        assertEquals(2, graph.checks().size());
        assertEquals(6, graph.relationships().size());

        assertEquals(Set.of(RelationshipType.GOVERNED_BY, RelationshipType.IMPLEMENTED_BY,
                        RelationshipType.USES, RelationshipType.HAS_CHECK),
                graph.relationships().stream()
                        .map(EvidenceGraphProjection.RelationshipProjection::type)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(EvidenceGraphProjection.CheckType.API, EvidenceGraphProjection.CheckType.SQL),
                graph.checks().stream().map(check -> check.check().checkType())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void ignoresUnrelatedAndAmbiguousEvidence() {
        EvidenceGraphProjection graph = assembler.assemble(request());

        assertTrue(graph.businessRules().stream().allMatch(rule ->
                rule.name().contains("example.request.RegisterRequest")));
        assertTrue(graph.testImplementations().stream().allMatch(test ->
                test.description().contains("RegistrationApiIT.registerSuccessfully")));
        assertTrue(graph.checks().stream().noneMatch(check ->
                check.description().contains("OtherApiIT") || check.description().contains("ambiguousTest")));
        assertFalse(graph.relationships().stream().anyMatch(relationship ->
                relationship.type() == RelationshipType.SPECIFIED_BY
                        || relationship.type() == RelationshipType.VALIDATES
                        || relationship.type() == RelationshipType.COVERS));
    }

    @Test
    void usesStableUniqueIdsOrderingAndKnownEndpoints() {
        EvidenceGraphProjection graph = assembler.assemble(request());
        List<String> nodeIds = nodeIds(graph);
        List<String> relationshipIds = graph.relationships().stream()
                .map(EvidenceGraphProjection.RelationshipProjection::id).toList();
        List<String> triples = graph.relationships().stream()
                .map(relationship -> relationship.from() + '|' + relationship.type() + '|' + relationship.to())
                .toList();

        assertEquals(nodeIds.size(), new HashSet<>(nodeIds).size());
        assertEquals(relationshipIds.size(), new HashSet<>(relationshipIds).size());
        assertEquals(triples.size(), new HashSet<>(triples).size());
        assertEquals(relationshipIds.stream().sorted().toList(), relationshipIds);
        assertTrue(graph.relationships().stream().allMatch(relationship ->
                nodeIds.contains(relationship.from()) && nodeIds.contains(relationship.to())));
        assertTrue(graph.businessRules().stream().allMatch(rule -> rule.id().startsWith("BR-VALIDATION-")));
        assertTrue(graph.testImplementations().stream().allMatch(test -> test.id().startsWith("TEST-AUTO-")));
        assertTrue(graph.checks().stream().allMatch(check -> check.id().startsWith("CHECK-AUTO-")));
    }

    @Test
    void repeatedAssemblyIsEqualAndInputsRemainUnchanged() {
        OperationEvidenceAssemblyRequest input = request();
        OperationEvidenceAssemblyRequest unchanged = new OperationEvidenceAssemblyRequest(
                input.operation(), input.requestModelBindings(), input.validationEvidence(),
                input.integrationTestEvidence());

        EvidenceGraphProjection first = assembler.assemble(input);
        EvidenceGraphProjection second = assembler.assemble(input);

        assertEquals(first, second);
        assertEquals(unchanged, input);
    }

    @Test
    void qualifiesTwoTestsForOneExactMethodAndPath() {
        OperationEvidenceAssemblyRequest input = request();
        TestImplementationEvidence second = test("example.api.SecondRegistrationApiIT", "register", 10);
        IntegrationTestEvidence original = input.integrationTestEvidence();
        IntegrationTestEvidence evidence = new IntegrationTestEvidence(
                List.of(original.tests().getFirst(), second),
                List.of(original.httpInteractions().getFirst(),
                        interaction(second, IntegrationHttpMethod.POST, "/auth/register", 14)),
                original.assertions());

        EvidenceGraphProjection graph = assembler.assemble(new OperationEvidenceAssemblyRequest(
                input.operation(), input.requestModelBindings(), input.validationEvidence(), evidence));

        assertEquals(2, graph.testImplementations().size());
        assertEquals(2, graph.relationships().stream()
                .filter(relationship -> relationship.type() == RelationshipType.USES).count());
    }

    @Test
    void duplicateEquivalentInteractionEvidenceDoesNotDuplicateOrDisqualifyTest() {
        OperationEvidenceAssemblyRequest input = request();
        IntegrationTestEvidence original = input.integrationTestEvidence();
        TestImplementationEvidence success = original.tests().getFirst();
        IntegrationTestEvidence evidence = new IntegrationTestEvidence(
                List.of(success),
                List.of(original.httpInteractions().getFirst(),
                        interaction(success, IntegrationHttpMethod.POST, "/auth/register/", 15)),
                original.assertions());

        EvidenceGraphProjection first = assembler.assemble(new OperationEvidenceAssemblyRequest(
                input.operation(), input.requestModelBindings(), input.validationEvidence(), evidence));
        EvidenceGraphProjection second = assembler.assemble(new OperationEvidenceAssemblyRequest(
                input.operation(), input.requestModelBindings(), input.validationEvidence(), evidence));

        assertEquals(1, first.testImplementations().size());
        assertEquals(first, second);
    }

    @Test
    void rejectsWrongMethodAndWrongPathIndependently() {
        OperationEvidenceAssemblyRequest input = request();
        TestImplementationEvidence test = input.integrationTestEvidence().tests().getFirst();

        for (HttpInteractionEvidence interaction : List.of(
                interaction(test, IntegrationHttpMethod.GET, "/auth/register", 14),
                interaction(test, IntegrationHttpMethod.POST, "/auth/other", 14))) {
            IntegrationTestEvidence evidence = new IntegrationTestEvidence(
                    List.of(test), List.of(interaction), input.integrationTestEvidence().assertions());
            EvidenceGraphProjection graph = assembler.assemble(new OperationEvidenceAssemblyRequest(
                    input.operation(), input.requestModelBindings(), input.validationEvidence(), evidence));
            assertTrue(graph.testImplementations().isEmpty());
            assertTrue(graph.checks().isEmpty());
        }
    }

    @Test
    void ambiguousOperationMatchRemovesInteractionBeforeAssembly() {
        OperationEvidenceAssemblyRequest input = request();
        RestOperationEvidence duplicate = new RestOperationEvidence(
                RestHttpMethod.POST, "/auth/register/", "OtherController", "register",
                "example.other", new SourceLocation("src/main/java/example/other/OtherController.java", 10, 5));

        IntegrationTestEvidence filtered = OperationTestQualification.withoutAmbiguousOperationMatches(
                List.of(input.operation(), duplicate), input.integrationTestEvidence());
        EvidenceGraphProjection graph = assembler.assemble(new OperationEvidenceAssemblyRequest(
                input.operation(), input.requestModelBindings(), input.validationEvidence(), filtered));

        assertTrue(graph.testImplementations().isEmpty());
        assertTrue(graph.checks().isEmpty());
    }

    @Test
    void omitsValidationEvidenceWithoutExplicitRequestModelBinding() {
        OperationEvidenceAssemblyRequest input = request();
        OperationEvidenceAssemblyRequest unbound = new OperationEvidenceAssemblyRequest(
                input.operation(), List.of(), input.validationEvidence(), input.integrationTestEvidence());

        EvidenceGraphProjection graph = assembler.assemble(unbound);

        assertTrue(graph.businessRules().isEmpty());
        assertTrue(graph.relationships().stream().noneMatch(relationship ->
                relationship.type() == RelationshipType.GOVERNED_BY));
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(NullPointerException.class, () -> assembler.assemble(null));
    }

    private static OperationEvidenceAssemblyRequest request() {
        RestOperationEvidence operation = new RestOperationEvidence(
                RestHttpMethod.POST,
                "/auth/register",
                "AuthController",
                "register",
                "example.api",
                new SourceLocation("src/main/java/example/api/AuthController.java", 20, 5));

        List<BeanValidationEvidence> validations = List.of(
                validation("example.request.RegisterRequest", "password",
                        "jakarta.validation.constraints.Size", Map.of("min", "6"), 12),
                validation("example.request.RegisterRequest", "username",
                        "jakarta.validation.constraints.NotBlank", Map.of(), 9),
                validation("example.request.UnrelatedRequest", "value",
                        "jakarta.validation.constraints.NotNull", Map.of(), 7));

        TestImplementationEvidence success = test("example.api.RegistrationApiIT", "registerSuccessfully", 10);
        TestImplementationEvidence unrelated = test("example.api.OtherApiIT", "getBook", 10);
        TestImplementationEvidence ambiguous = test("example.api.RegistrationApiIT", "ambiguousTest", 40);
        List<TestImplementationEvidence> tests = List.of(success, unrelated, ambiguous);

        List<HttpInteractionEvidence> interactions = List.of(
                interaction(success, IntegrationHttpMethod.POST, "/auth/register", 14),
                interaction(unrelated, IntegrationHttpMethod.GET, "/books/1", 14),
                interaction(ambiguous, IntegrationHttpMethod.POST, "/auth/register", 44),
                interaction(ambiguous, IntegrationHttpMethod.GET, "/other", 45));

        List<AssertionEvidence> assertions = List.of(
                assertion(success, AssertionCategory.HTTP_STATUS, "statusCode(200)", 16),
                assertion(success, AssertionCategory.PERSISTENCE_DATABASE,
                        "DatabaseAssertions.assertPersisted(repository, id)", 17),
                assertion(unrelated, AssertionCategory.RESPONSE_BODY, "body(\"id\", equalTo(1))", 16),
                assertion(ambiguous, AssertionCategory.HTTP_STATUS, "statusCode(200)", 47));

        return new OperationEvidenceAssemblyRequest(
                operation,
                List.of(binding("example.request.RegisterRequest")),
                validations,
                new IntegrationTestEvidence(tests, interactions, assertions));
    }

    private static RequestModelBindingEvidence binding(String modelType) {
        return new RequestModelBindingEvidence(
                "example.api.AuthController",
                "register",
                "request",
                "RegisterRequest",
                modelType,
                RequestBindingKind.REQUEST_BODY,
                Set.of(),
                "src/main/java/example/api/AuthController.java",
                20,
                55);
    }

    private static BeanValidationEvidence validation(
            String owner, String member, String annotation, Map<String, String> attributes, int line) {
        return new BeanValidationEvidence(
                owner,
                member,
                annotation,
                attributes,
                null,
                "src/main/java/" + owner.replace('.', '/') + ".java",
                line,
                5);
    }

    private static TestImplementationEvidence test(String owner, String method, int line) {
        return new TestImplementationEvidence(
                owner,
                method,
                null,
                "src/test/java/" + owner.replace('.', '/') + ".java",
                line,
                5);
    }

    private static HttpInteractionEvidence interaction(
            TestImplementationEvidence test, IntegrationHttpMethod method, String path, int line) {
        return new HttpInteractionEvidence(
                method,
                path,
                '"' + path + '"',
                test.testClass(),
                test.testMethod(),
                test.repositoryRelativePath(),
                line,
                9);
    }

    private static AssertionEvidence assertion(
            TestImplementationEvidence test, AssertionCategory category, String expression, int line) {
        return new AssertionEvidence(
                category,
                expression,
                test.testClass(),
                test.testMethod(),
                test.repositoryRelativePath(),
                line,
                9);
    }

    private static List<String> nodeIds(EvidenceGraphProjection graph) {
        List<String> ids = new ArrayList<>();
        ids.add(graph.businessOperation().id());
        graph.businessRules().forEach(node -> ids.add(node.id()));
        graph.technicalImplementations().forEach(node -> ids.add(node.id()));
        graph.testImplementations().forEach(node -> ids.add(node.id()));
        graph.checks().forEach(node -> ids.add(node.id()));
        return ids;
    }
}
