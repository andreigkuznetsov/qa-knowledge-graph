package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class StaticTestRestTemplateInteractionResolver {
    private static final String TEST_REST_TEMPLATE =
            "org.springframework.boot.test.web.client.TestRestTemplate";
    private static final Map<String, IntegrationHttpMethod> HTTP_METHODS = Map.of(
            "getForEntity", IntegrationHttpMethod.GET,
            "getForObject", IntegrationHttpMethod.GET,
            "postForEntity", IntegrationHttpMethod.POST,
            "postForObject", IntegrationHttpMethod.POST,
            "put", IntegrationHttpMethod.PUT,
            "patchForObject", IntegrationHttpMethod.PATCH,
            "delete", IntegrationHttpMethod.DELETE);

    private final Map<String, SourceType> types;
    private final StaticTestEndpointResolver endpointResolver;

    private StaticTestRestTemplateInteractionResolver(
            Map<String, SourceType> types,
            StaticTestEndpointResolver endpointResolver) {
        this.types = Map.copyOf(types);
        this.endpointResolver = endpointResolver;
    }

    static StaticTestRestTemplateInteractionResolver from(
            Path repositoryRoot,
            List<Path> sourceFiles,
            JavaParser parser,
            StaticTestEndpointResolver endpointResolver
    ) throws IOException {
        Map<String, SourceType> types = new LinkedHashMap<>();
        for (Path sourceFile : sourceFiles) {
            var result = parser.parse(sourceFile);
            CompilationUnit unit = result.getResult().orElseThrow(() ->
                    new IOException("Cannot parse Java test source " + repositoryRoot.relativize(sourceFile)));
            if (!result.isSuccessful()) {
                throw new IOException("Cannot parse Java test source " + repositoryRoot.relativize(sourceFile)
                        + ": " + result.getProblems());
            }
            for (TypeDeclaration<?> type : unit.getTypes()) {
                String owner = qualifiedName(unit, type.getNameAsString());
                types.put(owner, new SourceType(unit, type));
            }
        }
        return new StaticTestRestTemplateInteractionResolver(types, endpointResolver);
    }

    List<HttpInteractionEvidence> resolve(
            CompilationUnit testUnit,
            MethodDeclaration testMethod,
            String testClass,
            String relativePath
    ) {
        List<HttpInteractionEvidence> interactions = new ArrayList<>();
        for (MethodCallExpr invocation : testMethod.findAll(MethodCallExpr.class)) {
            resolveInvocation(testUnit, testMethod, testClass, relativePath, invocation)
                    .ifPresent(interactions::add);
        }
        return interactions.stream().distinct().toList();
    }

    private Optional<HttpInteractionEvidence> resolveInvocation(
            CompilationUnit testUnit,
            MethodDeclaration testMethod,
            String testClass,
            String relativePath,
            MethodCallExpr invocation
    ) {
        HelperMethod directOwner = new HelperMethod(testClass, testUnit, testMethod);
        Optional<ResolvedCall> direct = resolveTestRestTemplateCall(directOwner, invocation);
        if (direct.isPresent()) {
            return interaction(direct.orElseThrow(), invocation, testClass, testMethod, relativePath,
                    testClass + '.' + testMethod.getNameAsString());
        }
        List<HelperMethod> helpers = helperCandidates(testUnit, testMethod, testClass, invocation);
        if (helpers.size() != 1) return Optional.empty();
        HelperMethod helper = helpers.getFirst();

        List<ResolvedCall> calls = helper.method().findAll(MethodCallExpr.class).stream()
                .map(call -> resolveTestRestTemplateCall(helper, call))
                .flatMap(Optional::stream)
                .toList();
        if (calls.size() != 1) return Optional.empty();

        return interaction(calls.getFirst(), invocation, testClass, testMethod, relativePath,
                helper.owner() + '.' + helper.method().getNameAsString());
    }

    private static Optional<HttpInteractionEvidence> interaction(
            ResolvedCall call,
            MethodCallExpr invocation,
            String testClass,
            MethodDeclaration testMethod,
            String relativePath,
            String owner
    ) {
        var position = invocation.getName().getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed helper invocation has no source location"));
        return Optional.of(new HttpInteractionEvidence(
                call.method(), normalizePath(call.endpoint()), invocation.toString(), testClass,
                testMethod.getNameAsString(), relativePath, position.line, position.column,
                owner + " -> " + call.expression()));
    }

    private Optional<ResolvedCall> resolveTestRestTemplateCall(HelperMethod helper, MethodCallExpr call) {
        IntegrationHttpMethod method = HTTP_METHODS.get(call.getNameAsString());
        if (method == null || call.getArguments().isEmpty() || call.getScope().isEmpty()) {
            return Optional.empty();
        }
        Optional<String> receiverType = expressionType(
                helper.unit(), helper.method(), helper.owner(), call.getScope().orElseThrow());
        if (receiverType.isEmpty() || !isType(helper.unit(), receiverType.get(), TEST_REST_TEMPLATE)) {
            return Optional.empty();
        }
        return endpointResolver.resolve(helper.unit(), call.getArgument(0))
                .map(endpoint -> new ResolvedCall(method, endpoint, call.toString()));
    }

    private List<HelperMethod> helperCandidates(
            CompilationUnit unit,
            MethodDeclaration method,
            String testClass,
            MethodCallExpr invocation
    ) {
        if (invocation.getScope().isEmpty()) {
            return methods(testClass, invocation.getNameAsString(), invocation.getArguments().size());
        }
        Optional<String> owner = expressionType(
                unit, method, testClass, invocation.getScope().orElseThrow());
        if (owner.isEmpty()) return List.of();
        List<String> owners = resolveSourceTypes(unit, owner.get());
        if (owners.size() != 1) return List.of();
        return methods(owners.getFirst(), invocation.getNameAsString(), invocation.getArguments().size());
    }

    private List<HelperMethod> methods(String owner, String name, int arity) {
        SourceType source = types.get(owner);
        if (source == null) return List.of();
        return source.type().getMethods().stream()
                .filter(method -> method.getNameAsString().equals(name))
                .filter(method -> method.getParameters().size() == arity)
                .map(method -> new HelperMethod(owner, source.unit(), method))
                .toList();
    }

    private Optional<String> expressionType(
            CompilationUnit unit,
            MethodDeclaration method,
            String owner,
            Expression expression
    ) {
        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();
            Optional<String> variable = localType(method, name)
                    .or(() -> fieldType(owner, name, new HashSet<>()));
            if (variable.isPresent()) {
                List<String> sourceTypes = resolveSourceTypes(unit, variable.get());
                return sourceTypes.size() == 1 ? Optional.of(sourceTypes.getFirst()) : variable;
            }
            List<String> sourceTypes = resolveSourceTypes(unit, name);
            return sourceTypes.size() == 1 ? Optional.of(sourceTypes.getFirst()) : Optional.empty();
        }
        if (expression.isFieldAccessExpr() && expression.asFieldAccessExpr().getScope().isThisExpr()) {
            return fieldType(owner, expression.asFieldAccessExpr().getNameAsString(), new HashSet<>());
        }
        return Optional.empty();
    }

    private static Optional<String> localType(MethodDeclaration method, String name) {
        Optional<String> parameter = method.getParameters().stream()
                .filter(value -> value.getNameAsString().equals(name))
                .map(value -> value.getType().asString())
                .findFirst();
        if (parameter.isPresent()) return parameter;
        return method.findAll(VariableDeclarator.class).stream()
                .filter(value -> value.getNameAsString().equals(name))
                .map(value -> value.getType().asString())
                .findFirst();
    }

    private Optional<String> fieldType(String owner, String name, Set<String> visited) {
        if (!visited.add(owner)) return Optional.empty();
        SourceType source = types.get(owner);
        if (source == null) return Optional.empty();
        Optional<String> direct = source.type().getFields().stream()
                .flatMap(field -> field.getVariables().stream())
                .filter(variable -> variable.getNameAsString().equals(name))
                .map(variable -> variable.getType().asString())
                .findFirst();
        if (direct.isPresent()) {
            List<String> sourceTypes = resolveSourceTypes(source.unit(), direct.get());
            return sourceTypes.size() == 1 ? Optional.of(sourceTypes.getFirst()) : direct;
        }
        List<String> inherited = superTypes(source);
        List<String> candidates = inherited.stream()
                .map(parent -> fieldType(parent, name, visited))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    private List<String> superTypes(SourceType source) {
        if (!(source.type() instanceof ClassOrInterfaceDeclaration declaration)) return List.of();
        return declaration.getExtendedTypes().stream()
                .flatMap(parent -> resolveSourceTypes(source.unit(), parent.getNameWithScope()).stream())
                .distinct()
                .toList();
    }

    private List<String> resolveSourceTypes(CompilationUnit unit, String typeName) {
        List<String> candidates = new ArrayList<>();
        if (types.containsKey(typeName)) candidates.add(typeName);
        unit.getImports().stream()
                .filter(value -> !value.isStatic())
                .forEach(value -> {
                    if (value.isAsterisk()) {
                        String candidate = value.getNameAsString() + '.' + typeName;
                        if (types.containsKey(candidate)) candidates.add(candidate);
                    } else if (simpleName(value.getNameAsString()).equals(typeName)
                            && types.containsKey(value.getNameAsString())) {
                        candidates.add(value.getNameAsString());
                    }
                });
        String samePackage = qualifiedName(unit, typeName);
        if (types.containsKey(samePackage)) candidates.add(samePackage);
        return candidates.stream().distinct().toList();
    }

    private static boolean isType(CompilationUnit unit, String declaredType, String expected) {
        if (declaredType.equals(expected)) return true;
        if (!declaredType.equals(simpleName(expected))) return false;
        return unit.getImports().stream().anyMatch(value -> !value.isStatic() && !value.isAsterisk()
                && value.getNameAsString().equals(expected));
    }

    private static String normalizePath(String path) {
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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

    private record SourceType(CompilationUnit unit, TypeDeclaration<?> type) { }

    private record HelperMethod(String owner, CompilationUnit unit, MethodDeclaration method) { }

    private record ResolvedCall(IntegrationHttpMethod method, String endpoint, String expression) { }
}
