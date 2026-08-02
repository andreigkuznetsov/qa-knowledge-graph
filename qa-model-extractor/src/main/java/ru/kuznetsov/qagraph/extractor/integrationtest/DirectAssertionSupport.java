package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Optional;
import java.util.Set;

final class DirectAssertionSupport {
    private static final String JUNIT = "org.junit.jupiter.api.Assertions";
    private static final String ASSERTJ = "org.assertj.core.api.Assertions";
    private static final String HAMCREST = "org.hamcrest.MatcherAssert";
    private static final Set<String> JUNIT_METHODS = Set.of(
            "assertEquals", "assertNotEquals", "assertTrue", "assertFalse", "assertNull",
            "assertNotNull", "assertThrows", "assertDoesNotThrow", "assertIterableEquals",
            "assertArrayEquals");
    private static final Set<String> ASSERTJ_TERMINALS = Set.of(
            "isEqualTo", "isNotEqualTo", "isNull", "isNotNull", "isTrue", "isFalse",
            "contains", "containsExactly", "containsEntry", "hasSize", "isEmpty", "isNotEmpty",
            "matches", "satisfies");
    private static final Set<String> HAMCREST_MATCHERS = Set.of(
            "is", "equalTo", "not", "nullValue", "notNullValue", "containsString", "hasItem", "hasSize");

    private DirectAssertionSupport() {
    }

    static Optional<Descriptor> recognize(CompilationUnit unit, MethodCallExpr call) {
        String name = call.getNameAsString();
        if (JUNIT_METHODS.contains(name) && entryPoint(unit, call, JUNIT, name)) {
            return Optional.of(new Descriptor(AssertionEvidence.AssertionLibrary.JUNIT_5, name));
        }
        if (ASSERTJ_TERMINALS.contains(name) && hasAssertJRoot(unit, call)) {
            return Optional.of(new Descriptor(AssertionEvidence.AssertionLibrary.ASSERTJ, name));
        }
        if (name.equals("assertThat") && entryPoint(unit, call, HAMCREST, name)
                && call.getArguments().size() >= 2 && supportedMatcher(unit, call.getArgument(1))) {
            return Optional.of(new Descriptor(
                    AssertionEvidence.AssertionLibrary.HAMCREST,
                    call.getArgument(1).asMethodCallExpr().getNameAsString()));
        }
        return Optional.empty();
    }

    private static boolean hasAssertJRoot(CompilationUnit unit, MethodCallExpr terminal) {
        return terminal.getScope().stream()
                .flatMap(scope -> scope.findAll(MethodCallExpr.class).stream())
                .anyMatch(call -> call.getNameAsString().equals("assertThat")
                        && entryPoint(unit, call, ASSERTJ, "assertThat"));
    }

    private static boolean supportedMatcher(
            CompilationUnit unit, com.github.javaparser.ast.expr.Expression expression) {
        if (!expression.isMethodCallExpr()) return false;
        MethodCallExpr matcher = expression.asMethodCallExpr();
        if (!HAMCREST_MATCHERS.contains(matcher.getNameAsString())) return false;
        if (!hamcrestMatcherEntryPoint(unit, matcher)) return false;
        return matcher.getArguments().stream()
                .filter(com.github.javaparser.ast.expr.Expression::isMethodCallExpr)
                .map(com.github.javaparser.ast.expr.Expression::asMethodCallExpr)
                .allMatch(nested -> HAMCREST_MATCHERS.contains(nested.getNameAsString())
                        && hamcrestMatcherEntryPoint(unit, nested));
    }

    private static boolean hamcrestMatcherEntryPoint(CompilationUnit unit, MethodCallExpr matcher) {
        return entryPoint(unit, matcher, "org.hamcrest.Matchers", matcher.getNameAsString())
                || entryPoint(unit, matcher, "org.hamcrest.CoreMatchers", matcher.getNameAsString());
    }

    private static boolean entryPoint(
            CompilationUnit unit, MethodCallExpr call, String owner, String member) {
        if (call.getScope().isEmpty()) return hasStaticImport(unit, owner, member);
        String scope = call.getScope().orElseThrow().toString();
        if (scope.equals(owner)) return true;
        String simpleOwner = owner.substring(owner.lastIndexOf('.') + 1);
        return scope.equals(simpleOwner) && unit.getImports().stream().anyMatch(declaration ->
                !declaration.isStatic() && !declaration.isAsterisk()
                        && declaration.getNameAsString().equals(owner));
    }

    private static boolean hasStaticImport(CompilationUnit unit, String owner, String member) {
        return unit.getImports().stream().anyMatch(declaration -> declaration.isStatic()
                && (declaration.isAsterisk() && declaration.getNameAsString().equals(owner)
                || !declaration.isAsterisk()
                && declaration.getNameAsString().equals(owner + '.' + member)));
    }

    record Descriptor(AssertionEvidence.AssertionLibrary library, String kind) {
    }
}
