package ru.kuznetsov.qaip.core.application.query.operationtests;

import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared Runtime resolver for qualified tests and checks in one project snapshot. */
public final class DefaultOperationTestsResolver implements OperationTestsResolver {
    private static final String TEST_IMPLEMENTATION = "TEST_IMPLEMENTATION";
    private static final String TEST_EVIDENCE_PREFIX = "JUnit 5 test method ";

    private final OperationListProjector qualificationProjector;

    public DefaultOperationTestsResolver(OperationListProjector qualificationProjector) {
        this.qualificationProjector = Objects.requireNonNull(qualificationProjector, "qualificationProjector");
    }

    @Override
    public OperationTestsQueryResult resolve(Project project, String projectId, String operationId) {
        Project snapshot = Objects.requireNonNull(project, "project");
        String requestedProjectId = OperationTestsProjectNotFound.requireId(projectId, "projectId");
        String requestedOperationId = OperationTestsProjectNotFound.requireId(operationId, "operationId");
        Set<String> qualifiedIds = qualificationProjector.qualifiedTestIds(snapshot, requestedOperationId);
        if (qualifiedIds.isEmpty()) {
            return new OperationTestsNoneQualified(requestedProjectId, requestedOperationId);
        }
        List<QualifiedOperationTest> tests = snapshot.nodes().stream()
                .filter(node -> TEST_IMPLEMENTATION.equals(node.type()))
                .filter(node -> qualifiedIds.contains(node.id()))
                .map(node -> test(snapshot, node))
                .toList();
        OperationVerificationSemantics.Summary verification =
                OperationVerificationSemantics.summarize(tests);
        return new OperationTestsFound(requestedProjectId, requestedOperationId,
                verification.verificationStatus(), verification.tests());
    }

    private QualifiedOperationTest test(Project project, Node node) {
        TestDeclaration declaration = declaration(node);
        Set<String> checkIds = qualificationProjector.qualifiedCheckIds(project, node.id());
        List<QualifiedOperationCheck> checks = project.nodes().stream()
                .filter(candidate -> "CHECK".equals(candidate.type()))
                .filter(candidate -> checkIds.contains(candidate.id()))
                .map(DefaultOperationTestsResolver::check)
                .toList();
        return new QualifiedOperationTest(node.id(), node.name(), declaration.testClass(),
                declaration.testMethod(), checks.size(), checks);
    }

    private static QualifiedOperationCheck check(Node node) {
        Object content = node.attributes().get("check");
        if (!(content instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("qualified check content is unavailable: " + node.id());
        }
        Object type = values.get("checkType");
        if (!(type instanceof String value)) {
            throw new IllegalArgumentException("qualified check type is unavailable: " + node.id());
        }
        return new QualifiedOperationCheck(node.id(), node.name(), OperationCheckType.valueOf(value));
    }

    private static TestDeclaration declaration(Node node) {
        for (Map<String, Object> reference : node.sourceReferences()) {
            Object text = reference.get("text");
            if (!(text instanceof String value) || !value.startsWith(TEST_EVIDENCE_PREFIX)) continue;
            String declaration = value.substring(TEST_EVIDENCE_PREFIX.length());
            if (declaration.endsWith(".")) declaration = declaration.substring(0, declaration.length() - 1);
            TestDeclaration parsed = splitDeclaration(declaration);
            if (parsed != null) return parsed;
        }
        TestDeclaration parsed = splitDeclaration(node.name());
        if (parsed != null) return parsed;
        throw new IllegalArgumentException("qualified test declaration is unavailable: " + node.id());
    }

    private static TestDeclaration splitDeclaration(String value) {
        if (value == null) return null;
        int separator = value.lastIndexOf('.');
        if (separator <= 0 || separator == value.length() - 1) return null;
        return new TestDeclaration(value.substring(0, separator), value.substring(separator + 1));
    }

    private record TestDeclaration(String testClass, String testMethod) { }
}
