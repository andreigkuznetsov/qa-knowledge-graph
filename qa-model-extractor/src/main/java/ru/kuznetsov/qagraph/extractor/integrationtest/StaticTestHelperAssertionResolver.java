package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class StaticTestHelperAssertionResolver {
    private static final String JUNIT_ASSERTIONS = "org.junit.jupiter.api.Assertions";
    private static final Set<String> JUNIT_METHODS = Set.of(
            "assertEquals", "assertNotEquals", "assertTrue", "assertFalse", "assertNull",
            "assertNotNull", "assertSame", "assertNotSame", "assertArrayEquals", "assertIterableEquals",
            "assertLinesMatch", "assertThrows", "assertDoesNotThrow", "fail");

    private final Map<String, List<HelperMethod>> methodsByOwner;

    private StaticTestHelperAssertionResolver(Map<String, List<HelperMethod>> methodsByOwner) {
        this.methodsByOwner = Map.copyOf(methodsByOwner);
    }

    static StaticTestHelperAssertionResolver from(
            Path repositoryRoot, List<Path> sourceFiles, JavaParser parser) throws IOException {
        Map<String, List<HelperMethod>> methods = new LinkedHashMap<>();
        for (Path sourceFile : sourceFiles) {
            var result = parser.parse(sourceFile);
            CompilationUnit unit = result.getResult().orElseThrow(() ->
                    new IOException("Cannot parse Java test source " + repositoryRoot.relativize(sourceFile)));
            if (!result.isSuccessful()) {
                throw new IOException("Cannot parse Java test source " + repositoryRoot.relativize(sourceFile)
                        + ": " + result.getProblems());
            }
            String relativePath = repositoryRoot.relativize(sourceFile).toString().replace('\\', '/');
            for (TypeDeclaration<?> type : unit.getTypes()) {
                String owner = qualifiedName(unit, type.getNameAsString());
                for (MethodDeclaration method : type.getMethods()) {
                    methods.computeIfAbsent(owner, ignored -> new ArrayList<>())
                            .add(new HelperMethod(owner, method, unit, relativePath));
                }
            }
        }
        methods.replaceAll((ignored, values) -> List.copyOf(values));
        return new StaticTestHelperAssertionResolver(methods);
    }

    List<AssertionEvidence> resolve(
            CompilationUnit testUnit,
            MethodCallExpr invocation,
            String testClass,
            String testMethod,
            String invocationPath) {
        List<HelperMethod> candidates = resolveCandidates(testUnit, invocation, testClass);
        if (candidates.size() != 1) return List.of();
        HelperMethod helper = candidates.getFirst();
        if (isRecursive(helper)) return List.of();

        var invocationPosition = invocation.getName().getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed helper invocation has no source location"));
        var context = new AssertionEvidence.HelperInvocationEvidence(
                helper.owner(), helper.method().getNameAsString(), invocation.toString(), invocationPath,
                invocationPosition.line, invocationPosition.column);
        List<AssertionEvidence> evidence = new ArrayList<>();
        for (MethodCallExpr assertion : helper.method().findAll(MethodCallExpr.class)) {
            AssertionCategory category = classify(helper, assertion);
            if (category == null) continue;
            var position = assertion.getName().getBegin().orElseThrow(() ->
                    new IllegalStateException("Parsed helper assertion has no source location"));
            evidence.add(new AssertionEvidence(
                    category,
                    assertion.toString(),
                    testClass,
                    testMethod,
                    helper.repositoryRelativePath(),
                    position.line,
                    position.column,
                    context));
        }
        return List.copyOf(evidence);
    }

    boolean resolves(CompilationUnit unit, MethodCallExpr invocation, String testClass) {
        return resolveCandidates(unit, invocation, testClass).size() == 1;
    }

    private List<HelperMethod> resolveCandidates(
            CompilationUnit unit, MethodCallExpr invocation, String testClass) {
        String methodName = invocation.getNameAsString();
        int arity = invocation.getArguments().size();
        List<HelperMethod> candidates = new ArrayList<>();
        if (invocation.getScope().isEmpty()) {
            addMethods(candidates, testClass, methodName, arity, false);
            for (ImportDeclaration declaration : unit.getImports()) {
                if (!declaration.isStatic()) continue;
                if (declaration.isAsterisk()) {
                    addMethods(candidates, declaration.getNameAsString(), methodName, arity, true);
                } else if (simpleName(declaration.getNameAsString()).equals(methodName)) {
                    String owner = declaration.getNameAsString().substring(
                            0, declaration.getNameAsString().length() - methodName.length() - 1);
                    addMethods(candidates, owner, methodName, arity, true);
                }
            }
        } else if (invocation.getScope().orElseThrow().isNameExpr()) {
            String typeName = invocation.getScope().orElseThrow().asNameExpr().getNameAsString();
            for (String owner : resolveTypes(unit, typeName)) {
                addMethods(candidates, owner, methodName, arity, true);
            }
        }
        return candidates.stream().distinct().toList();
    }

    private void addMethods(
            List<HelperMethod> candidates, String owner, String name, int arity, boolean requireStatic) {
        methodsByOwner.getOrDefault(owner, List.of()).stream()
                .filter(helper -> helper.method().getNameAsString().equals(name))
                .filter(helper -> helper.method().getParameters().size() == arity)
                .filter(helper -> !requireStatic || helper.method().isStatic())
                .forEach(candidates::add);
    }

    private List<String> resolveTypes(CompilationUnit unit, String typeName) {
        List<String> candidates = new ArrayList<>();
        if (typeName.contains(".") && methodsByOwner.containsKey(typeName)) candidates.add(typeName);
        unit.getImports().stream()
                .filter(declaration -> !declaration.isStatic() && !declaration.isAsterisk())
                .map(ImportDeclaration::getNameAsString)
                .filter(name -> simpleName(name).equals(typeName))
                .filter(methodsByOwner::containsKey)
                .forEach(candidates::add);
        String samePackage = qualifiedName(unit, typeName);
        if (methodsByOwner.containsKey(samePackage)) candidates.add(samePackage);
        return candidates;
    }

    private boolean isRecursive(HelperMethod helper) {
        return helper.method().findAll(MethodCallExpr.class).stream().anyMatch(call ->
                call.getNameAsString().equals(helper.method().getNameAsString())
                        && call.getArguments().size() == helper.method().getParameters().size()
                        && (call.getScope().isEmpty()
                        || call.getScope().map(Object::toString).orElse("").equals("this")
                        || call.getScope().map(Object::toString).orElse("").equals(simpleName(helper.owner()))));
    }

    private static AssertionCategory classify(HelperMethod helper, MethodCallExpr assertion) {
        if (MockMvcEvidenceSupport.hasResultActionsParameter(helper.unit(), helper.method())) {
            AssertionCategory mockMvc = MockMvcEvidenceSupport.assertionCategory(helper.unit(), assertion);
            if (mockMvc != null) return mockMvc;
        }
        if (assertion.getNameAsString().equals("statusCode") && responseRelated(helper, assertion)) {
            return AssertionCategory.HTTP_STATUS;
        }
        if (assertion.getNameAsString().equals("body") && responseRelated(helper, assertion)) {
            return AssertionCategory.RESPONSE_BODY;
        }
        if (!isJunitAssertion(helper.unit(), assertion)) return null;
        if (assertion.getArguments().stream().anyMatch(StaticTestHelperAssertionResolver::containsStatusAccess)) {
            return AssertionCategory.HTTP_STATUS;
        }
        if (isPersistenceHelper(helper)) return AssertionCategory.PERSISTENCE_DATABASE;
        if (!hasResponseParameter(helper.method())) return null;
        return AssertionCategory.RESPONSE_BODY;
    }

    private static boolean isJunitAssertion(CompilationUnit unit, MethodCallExpr assertion) {
        String name = assertion.getNameAsString();
        if (!JUNIT_METHODS.contains(name)) return false;
        if (assertion.getScope().map(Object::toString).filter(scope ->
                scope.equals("Assertions") || scope.equals(JUNIT_ASSERTIONS)).isPresent()) return true;
        if (assertion.getScope().isPresent()) return false;
        return unit.getImports().stream().anyMatch(declaration -> declaration.isStatic()
                && (declaration.isAsterisk() && declaration.getNameAsString().equals(JUNIT_ASSERTIONS)
                || declaration.getNameAsString().equals(JUNIT_ASSERTIONS + '.' + name)));
    }

    private static boolean containsStatusAccess(Expression expression) {
        return expression.findAll(MethodCallExpr.class).stream().anyMatch(call -> {
            String name = call.getNameAsString();
            return name.equals("getStatusCode") || name.equals("statusCode");
        });
    }

    private static boolean hasResponseParameter(MethodDeclaration method) {
        return method.getParameters().stream().map(Parameter::getTypeAsString).anyMatch(type ->
                type.equals("Response") || type.equals("io.restassured.response.Response"));
    }

    private static boolean responseRelated(HelperMethod helper, MethodCallExpr assertion) {
        return hasResponseParameter(helper.method())
                && assertion.getScope().isPresent();
    }

    private static boolean isPersistenceHelper(HelperMethod helper) {
        String identity = (helper.owner() + '.' + helper.method().getNameAsString()).toLowerCase(Locale.ROOT);
        if (identity.contains("db") || identity.contains("database")
                || identity.contains("persistence") || identity.contains("repository")) return true;
        return helper.method().getParameters().stream().map(Parameter::getTypeAsString)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("repository"));
    }

    private static String qualifiedName(CompilationUnit unit, String typeName) {
        return unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + typeName)
                .orElse(typeName);
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private record HelperMethod(
            String owner,
            MethodDeclaration method,
            CompilationUnit unit,
            String repositoryRelativePath) {
    }
}
