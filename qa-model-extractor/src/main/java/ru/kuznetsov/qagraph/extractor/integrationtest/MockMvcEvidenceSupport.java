package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Optional;
import java.util.Set;

final class MockMvcEvidenceSupport {
    static final String REQUEST_BUILDERS =
            "org.springframework.test.web.servlet.request.MockMvcRequestBuilders";
    private static final String RESULT_MATCHERS =
            "org.springframework.test.web.servlet.result.MockMvcResultMatchers";
    private static final String MOCK_MVC = "org.springframework.test.web.servlet.MockMvc";
    private static final Set<String> HTTP_BUILDERS = Set.of("get", "post", "put", "patch", "delete");

    private MockMvcEvidenceSupport() {
    }

    static boolean isMockMvcPerform(
            CompilationUnit unit, MethodDeclaration method, MethodCallExpr call) {
        if (!call.getNameAsString().equals("perform") || call.getScope().isEmpty()) return false;
        String scope = call.getScope().orElseThrow().toString();
        String fieldName = scope.equals("this.mockMvc") ? "mockMvc" : scope;
        if (!fieldName.equals("mockMvc")) return false;
        Optional<TypeDeclaration<?>> owner = owningType(method);
        if (owner.isEmpty()) return false;
        boolean mockMvcField = owner.get().getFields().stream().anyMatch(field ->
                field.getVariables().stream().anyMatch(variable -> variable.getNameAsString().equals(fieldName))
                        && isDeclaredType(unit, field.getElementType().asString(), MOCK_MVC));
        return mockMvcField;
    }

    private static Optional<TypeDeclaration<?>> owningType(MethodDeclaration method) {
        Optional<Node> current = method.getParentNode();
        while (current.isPresent()) {
            if (current.get() instanceof TypeDeclaration<?> type) return Optional.of(type);
            current = current.get().getParentNode();
        }
        return Optional.empty();
    }

    static Optional<MethodCallExpr> requestBuilder(CompilationUnit unit, MethodCallExpr perform) {
        if (perform.getArguments().isEmpty()) return Optional.empty();
        var candidates = perform.getArgument(0).findAll(MethodCallExpr.class).stream()
                .filter(call -> HTTP_BUILDERS.contains(call.getNameAsString()))
                .filter(call -> isRequestBuilder(unit, call))
                .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    static AssertionCategory assertionCategory(CompilationUnit unit, MethodCallExpr andExpect) {
        if (!andExpect.getNameAsString().equals("andExpect") || andExpect.getArguments().size() != 1) return null;
        Expression matcher = andExpect.getArgument(0);
        if (containsMatcherRoot(unit, matcher, "status")) return AssertionCategory.HTTP_STATUS;
        if (containsMatcherRoot(unit, matcher, "jsonPath")
                || containsMatcherRoot(unit, matcher, "content")
                || containsMatcherRoot(unit, matcher, "header")) {
            return AssertionCategory.RESPONSE_BODY;
        }
        return null;
    }

    static boolean hasResultActionsParameter(CompilationUnit unit, MethodDeclaration method) {
        return method.getParameters().stream().map(Parameter::getTypeAsString)
                .anyMatch(type -> isDeclaredType(
                        unit, type, "org.springframework.test.web.servlet.ResultActions"));
    }

    private static boolean containsMatcherRoot(
            CompilationUnit unit, Expression expression, String rootName) {
        return expression.findAll(MethodCallExpr.class).stream().anyMatch(call ->
                call.getNameAsString().equals(rootName) && isResultMatcherFactory(unit, call));
    }

    private static boolean isRequestBuilder(CompilationUnit unit, MethodCallExpr call) {
        if (call.getScope().isEmpty()) return hasStaticImport(unit, REQUEST_BUILDERS, call.getNameAsString());
        String scope = call.getScope().orElseThrow().toString();
        return scope.equals(REQUEST_BUILDERS)
                || scope.equals("MockMvcRequestBuilders") && importsType(unit, REQUEST_BUILDERS);
    }

    private static boolean isResultMatcherFactory(CompilationUnit unit, MethodCallExpr call) {
        if (call.getScope().isEmpty()) return hasStaticImport(unit, RESULT_MATCHERS, call.getNameAsString());
        String scope = call.getScope().orElseThrow().toString();
        return scope.equals(RESULT_MATCHERS)
                || scope.equals("MockMvcResultMatchers") && importsType(unit, RESULT_MATCHERS);
    }

    private static boolean hasStaticImport(CompilationUnit unit, String owner, String member) {
        return unit.getImports().stream().anyMatch(declaration -> declaration.isStatic()
                && (declaration.isAsterisk() && declaration.getNameAsString().equals(owner)
                || !declaration.isAsterisk()
                && declaration.getNameAsString().equals(owner + '.' + member)));
    }

    private static boolean importsType(CompilationUnit unit, String qualifiedName) {
        return unit.getImports().stream().anyMatch(declaration -> !declaration.isStatic()
                && (!declaration.isAsterisk() && declaration.getNameAsString().equals(qualifiedName)
                || declaration.isAsterisk()
                && declaration.getNameAsString().equals(packageName(qualifiedName))));
    }

    private static boolean isDeclaredType(
            CompilationUnit unit, String declaredType, String qualifiedType) {
        return declaredType.equals(qualifiedType)
                || simpleName(declaredType).equals(simpleName(qualifiedType))
                && importsType(unit, qualifiedType);
    }

    private static String packageName(String value) {
        return value.substring(0, value.lastIndexOf('.'));
    }

    private static String simpleName(String value) {
        int separator = value.lastIndexOf('.');
        return separator < 0 ? value : value.substring(separator + 1);
    }
}
