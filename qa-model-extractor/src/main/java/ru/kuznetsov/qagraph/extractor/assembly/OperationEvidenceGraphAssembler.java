package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionCategory;
import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.HttpInteractionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.TestImplementationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.extractor.rest.mapping.RestOperationEvidenceMapper;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class OperationEvidenceGraphAssembler {
    private final RestOperationEvidenceMapper operationMapper = new RestOperationEvidenceMapper();

    public EvidenceGraphProjection assemble(OperationEvidenceAssemblyRequest request) {
        Objects.requireNonNull(request, "request");
        BusinessOperationProjection operation = operationMapper.map(request.operation());
        EvidenceGraphProjection.TechnicalImplementationProjection implementation =
                technicalImplementation(request.operation(), operation);

        Map<String, EvidenceGraphProjection.BusinessRuleProjection> rules = new TreeMap<>();
        for (BeanValidationEvidence evidence : request.validationEvidence()) {
            if (!request.boundRequestModelTypes().contains(evidence.owningJavaType())) continue;
            var projection = businessRule(evidence);
            rules.putIfAbsent(projection.id(), projection);
        }

        Map<String, EvidenceGraphProjection.TestImplementationProjection> tests = new TreeMap<>();
        Map<String, EvidenceGraphProjection.CheckProjection> checks = new TreeMap<>();
        List<EvidenceGraphProjection.RelationshipProjection> relationships = new ArrayList<>();
        relationships.add(relationship(
                operation.id(), RelationshipType.IMPLEMENTED_BY, implementation.id()));
        for (var rule : rules.values()) {
            relationships.add(relationship(operation.id(), RelationshipType.GOVERNED_BY, rule.id()));
        }

        addTestsAndChecks(request, implementation, tests, checks, relationships);
        relationships.sort(Comparator.comparing(EvidenceGraphProjection.RelationshipProjection::id));

        EvidenceGraphProjection graph = new EvidenceGraphProjection(
                operation,
                List.copyOf(rules.values()),
                List.of(implementation),
                List.copyOf(tests.values()),
                List.copyOf(checks.values()),
                relationships);
        verifyGraph(graph);
        return graph;
    }

    private static void addTestsAndChecks(
            OperationEvidenceAssemblyRequest request,
            EvidenceGraphProjection.TechnicalImplementationProjection implementation,
            Map<String, EvidenceGraphProjection.TestImplementationProjection> tests,
            Map<String, EvidenceGraphProjection.CheckProjection> checks,
            List<EvidenceGraphProjection.RelationshipProjection> relationships) {
        IntegrationTestEvidence testEvidence = request.integrationTestEvidence();
        for (TestImplementationEvidence test : testEvidence.tests()) {
            List<HttpInteractionEvidence> interactions = testEvidence.httpInteractions().stream()
                    .filter(interaction -> sameTest(interaction, test))
                    .toList();
            if (interactions.size() != 1 || !matchesOperation(interactions.getFirst(), request.operation())) {
                continue;
            }

            var testProjection = testImplementation(test);
            if (tests.putIfAbsent(testProjection.id(), testProjection) != null) continue;
            relationships.add(relationship(
                    testProjection.id(), RelationshipType.USES, implementation.id()));

            for (AssertionEvidence assertion : testEvidence.assertions()) {
                if (!sameTest(assertion, test)) continue;
                var check = check(assertion, testProjection.id());
                if (checks.putIfAbsent(check.id(), check) == null) {
                    relationships.add(relationship(
                            testProjection.id(), RelationshipType.HAS_CHECK, check.id()));
                }
            }
        }
    }

    private static EvidenceGraphProjection.BusinessRuleProjection businessRule(
            BeanValidationEvidence evidence) {
        String canonical = "VALIDATION|" + evidence.owningJavaType() + '|' + evidence.memberName()
                + '|' + evidence.annotationType() + '|' + evidence.declaredAttributes()
                + '|' + sourceLocation(evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String identity = sha256(canonical);
        String annotation = simpleName(evidence.annotationType());
        String target = evidence.owningJavaType() + '.' + evidence.memberName();
        String rendered = renderAnnotation(evidence);
        return new EvidenceGraphProjection.BusinessRuleProjection(
                "BR-VALIDATION-" + identity,
                NodeType.BUSINESS_RULE,
                "@" + annotation + " on " + target,
                "Declared Bean Validation constraint " + rendered + " on " + target + ".",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.owningJavaType()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        sourceLocation(evidence.repositoryRelativePath(), evidence.line(), evidence.column())
                                + "#" + target,
                        "Bean Validation annotation " + rendered)),
                new EvidenceGraphProjection.RuleProjection(
                        "VALIDATION-" + identity,
                        EvidenceGraphProjection.RuleType.VALIDATION_RULE,
                        rendered + " is declared on " + target + ".",
                        null));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection technicalImplementation(
            RestOperationEvidence evidence, BusinessOperationProjection operation) {
        String controller = qualifiedController(evidence);
        SourceLocation location = evidence.sourceLocation();
        String locationValue = location == null
                ? controller + '.' + evidence.controllerMethod()
                : sourceLocation(location.repositoryRelativePath(), location.line(), location.column())
                + "#" + controller + '.' + evidence.controllerMethod();
        String identity = sha256("CONTROLLER|" + locationValue);
        var operationSource = operation.sourceReferences().getFirst();
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-REST-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                controller + '.' + evidence.controllerMethod(),
                "Spring MVC controller method implementing " + operation.name() + ".",
                List.of(sourceReference(
                        operationSource.sourceId(),
                        EvidenceGraphProjection.LocationType.OTHER,
                        locationValue,
                        "Controller method " + controller + '.' + evidence.controllerMethod())),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.API,
                        evidence.javaPackage().isBlank() ? evidence.controllerClass() : evidence.javaPackage(),
                        Map.of(
                                "controllerClass", controller,
                                "controllerMethod", evidence.controllerMethod(),
                                "endpoint", operation.name())));
    }

    private static EvidenceGraphProjection.TestImplementationProjection testImplementation(
            TestImplementationEvidence evidence) {
        String location = sourceLocation(
                evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String canonical = "TEST|" + evidence.testClass() + '#' + evidence.testMethod() + '|' + location;
        String identity = sha256(canonical);
        String name = evidence.displayName() == null
                ? evidence.testClass() + '.' + evidence.testMethod()
                : evidence.displayName();
        return new EvidenceGraphProjection.TestImplementationProjection(
                "TEST-AUTO-" + identity,
                NodeType.TEST_IMPLEMENTATION,
                name,
                "Automated JUnit 5 REST Assured test method "
                        + evidence.testClass() + '.' + evidence.testMethod() + ".",
                List.of(sourceReference(
                        "SRC-TEST-" + sha256(evidence.testClass()),
                        EvidenceGraphProjection.LocationType.TEST_CASE,
                        location + "#" + evidence.testClass() + '.' + evidence.testMethod(),
                        "JUnit 5 test method " + evidence.testClass() + '.' + evidence.testMethod())),
                new EvidenceGraphProjection.TestProjection(
                        "TEST-" + identity,
                        EvidenceGraphProjection.ExecutionType.AUTOMATED,
                        List.of(),
                        List.of()));
    }

    private static EvidenceGraphProjection.CheckProjection check(
            AssertionEvidence evidence, String testId) {
        String location = sourceLocation(
                evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String canonical = "CHECK|" + testId + '|' + evidence.category() + '|'
                + evidence.expression() + '|' + location;
        String identity = sha256(canonical);
        EvidenceGraphProjection.CheckType checkType = evidence.category() == AssertionCategory.PERSISTENCE_DATABASE
                ? EvidenceGraphProjection.CheckType.SQL
                : EvidenceGraphProjection.CheckType.API;
        return new EvidenceGraphProjection.CheckProjection(
                "CHECK-AUTO-" + identity,
                NodeType.CHECK,
                evidence.category() + " assertion",
                "Source assertion from " + evidence.owningTestClass()
                        + '.' + evidence.owningTestMethod() + ".",
                List.of(sourceReference(
                        "SRC-TEST-" + sha256(evidence.owningTestClass()),
                        EvidenceGraphProjection.LocationType.TEST_CASE,
                        location + "#" + evidence.owningTestClass() + '.' + evidence.owningTestMethod(),
                        "Assertion expression " + evidence.expression())),
                new EvidenceGraphProjection.CheckContentProjection(
                        checkType,
                        evidence.expression(),
                        Map.of("category", evidence.category().name())));
    }

    private static EvidenceGraphProjection.RelationshipProjection relationship(
            String from, RelationshipType type, String to) {
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(from + '|' + type.name() + '|' + to), from, type, to);
    }

    private static EvidenceGraphProjection.SourceReferenceProjection sourceReference(
            String sourceId,
            EvidenceGraphProjection.LocationType locationType,
            String location,
            String text) {
        return new EvidenceGraphProjection.SourceReferenceProjection(
                sourceId,
                new EvidenceGraphProjection.SourceLocationProjection(locationType, location),
                text,
                1.0,
                EvidenceGraphProjection.EvidenceType.OBSERVED);
    }

    private static boolean matchesOperation(
            HttpInteractionEvidence interaction, RestOperationEvidence operation) {
        return interaction.httpMethod().name().equals(operation.httpMethod().name())
                && normalizePath(interaction.endpointPath()).equals(normalizePath(operation.endpointPath()));
    }

    private static boolean sameTest(
            HttpInteractionEvidence interaction, TestImplementationEvidence test) {
        return interaction.owningTestClass().equals(test.testClass())
                && interaction.owningTestMethod().equals(test.testMethod());
    }

    private static boolean sameTest(AssertionEvidence assertion, TestImplementationEvidence test) {
        return assertion.owningTestClass().equals(test.testClass())
                && assertion.owningTestMethod().equals(test.testMethod());
    }

    private static String renderAnnotation(BeanValidationEvidence evidence) {
        if (evidence.declaredAttributes().isEmpty()) return "@" + simpleName(evidence.annotationType());
        String attributes = evidence.declaredAttributes().entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return "@" + simpleName(evidence.annotationType()) + "(" + attributes + ")";
    }

    private static String qualifiedController(RestOperationEvidence evidence) {
        return evidence.javaPackage().isBlank()
                ? evidence.controllerClass()
                : evidence.javaPackage() + '.' + evidence.controllerClass();
    }

    private static String sourceLocation(String path, int line, int column) {
        return path + ':' + line + ':' + column;
    }

    private static String normalizePath(String path) {
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void verifyGraph(EvidenceGraphProjection graph) {
        Set<String> nodeIds = new HashSet<>();
        addUnique(nodeIds, graph.businessOperation().id());
        graph.businessRules().forEach(node -> addUnique(nodeIds, node.id()));
        graph.technicalImplementations().forEach(node -> addUnique(nodeIds, node.id()));
        graph.testImplementations().forEach(node -> addUnique(nodeIds, node.id()));
        graph.checks().forEach(node -> addUnique(nodeIds, node.id()));

        Set<String> relationshipIds = new HashSet<>();
        Set<String> triples = new HashSet<>();
        for (var relationship : graph.relationships()) {
            if (!nodeIds.contains(relationship.from()) || !nodeIds.contains(relationship.to())) {
                throw new IllegalStateException("Relationship has an unknown endpoint");
            }
            addUnique(relationshipIds, relationship.id());
            addUnique(triples, relationship.from() + '|' + relationship.type() + '|' + relationship.to());
        }
    }

    private static void addUnique(Set<String> values, String value) {
        if (!values.add(value)) throw new IllegalStateException("Duplicate graph identity " + value);
    }
}
