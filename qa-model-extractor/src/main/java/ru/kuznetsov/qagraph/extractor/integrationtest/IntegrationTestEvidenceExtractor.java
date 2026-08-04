package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class IntegrationTestEvidenceExtractor {
    private static final String JUNIT_API = "org.junit.jupiter.api";
    private static final String JUNIT_PARAMS = "org.junit.jupiter.params";
    private static final String JUNIT_PROVIDERS = "org.junit.jupiter.params.provider";
    private static final String REST_ASSURED = "io.restassured.RestAssured";
    private static final Set<String> PARAMETER_SOURCES = Set.of(
            "ValueSource", "CsvSource", "CsvFileSource", "EnumSource", "MethodSource", "ArgumentsSource");
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "head", "post", "put", "patch", "delete", "options", "trace");
    private static final Comparator<TestImplementationEvidence> TEST_ORDER = Comparator
            .comparing(TestImplementationEvidence::testClass)
            .thenComparing(TestImplementationEvidence::testMethod)
            .thenComparing(TestImplementationEvidence::repositoryRelativePath)
            .thenComparingInt(TestImplementationEvidence::line)
            .thenComparingInt(TestImplementationEvidence::column);
    private static final Comparator<HttpInteractionEvidence> INTERACTION_ORDER = Comparator
            .comparing(HttpInteractionEvidence::owningTestClass)
            .thenComparing(HttpInteractionEvidence::owningTestMethod)
            .thenComparingInt(HttpInteractionEvidence::line)
            .thenComparingInt(HttpInteractionEvidence::column)
            .thenComparing(interaction -> interaction.httpMethod().name())
            .thenComparing(HttpInteractionEvidence::endpointPath);
    private static final Comparator<AssertionEvidence> ASSERTION_ORDER = Comparator
            .comparing(AssertionEvidence::owningTestClass)
            .thenComparing(AssertionEvidence::owningTestMethod)
            .thenComparingInt(AssertionEvidence::line)
            .thenComparingInt(AssertionEvidence::column)
            .thenComparing(assertion -> assertion.category().name())
            .thenComparing(AssertionEvidence::expression)
            .thenComparing(assertion -> helperOrderKey(assertion.helperInvocation()));

    private final JavaParser parser;

    public IntegrationTestEvidenceExtractor() {
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    public IntegrationTestEvidence extract(Path repositoryRoot) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path testRoot = normalizedRoot.resolve("src/test/java");
        if (!Files.isDirectory(testRoot)) {
            return new IntegrationTestEvidence(List.of(), List.of(), List.of());
        }

        List<Path> testFiles;
        try (Stream<Path> files = Files.walk(testRoot)) {
            testFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path ->
                            normalizedRoot.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }

        List<TestImplementationEvidence> tests = new ArrayList<>();
        List<HttpInteractionEvidence> interactions = new ArrayList<>();
        List<AssertionEvidence> assertions = new ArrayList<>();
        StaticTestEndpointResolver endpointResolver = StaticTestEndpointResolver.from(testFiles, parser);
        StaticTestRestTemplateInteractionResolver testRestTemplateResolver =
                StaticTestRestTemplateInteractionResolver.from(
                        normalizedRoot, testFiles, parser, endpointResolver);
        StaticTestHelperAssertionResolver helperResolver =
                StaticTestHelperAssertionResolver.from(normalizedRoot, testFiles, parser);
        for (Path testFile : testFiles) {
            extractFile(normalizedRoot, testFile, endpointResolver, testRestTemplateResolver, helperResolver,
                    tests, interactions, assertions);
        }
        tests.sort(TEST_ORDER);
        interactions.sort(INTERACTION_ORDER);
        assertions.sort(ASSERTION_ORDER);
        return new IntegrationTestEvidence(
                tests.stream().distinct().toList(),
                interactions.stream().distinct().toList(),
                assertions.stream().distinct().toList());
    }

    private void extractFile(
            Path repositoryRoot,
            Path testFile,
            StaticTestEndpointResolver endpointResolver,
            StaticTestRestTemplateInteractionResolver testRestTemplateResolver,
            StaticTestHelperAssertionResolver helperResolver,
            List<TestImplementationEvidence> tests,
            List<HttpInteractionEvidence> interactions,
            List<AssertionEvidence> assertions) throws IOException {
        ParseResult<CompilationUnit> result = parser.parse(testFile);
        CompilationUnit unit = result.getResult().orElseThrow(() ->
                new IOException("Cannot parse Java test source " + repositoryRoot.relativize(testFile)));
        if (!result.isSuccessful()) {
            throw new IOException("Cannot parse Java test source " + repositoryRoot.relativize(testFile)
                    + ": " + result.getProblems());
        }

        String relativePath = repositoryRoot.relativize(testFile).toString().replace('\\', '/');
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            Optional<AnnotationExpr> testAnnotation = supportedTestAnnotation(unit, method);
            boolean restAssured = containsRestAssuredCall(unit, method);
            boolean mockMvc = containsMockMvcCall(unit, method);
            List<HttpInteractionEvidence> testRestTemplateInteractions =
                    testRestTemplateResolver.resolve(unit, method, owningType(method), relativePath);
            boolean testRestTemplate = !testRestTemplateInteractions.isEmpty();
            boolean directAssertion = containsDirectAssertion(unit, method);
            if (testAnnotation.isEmpty()
                    || !restAssured && !mockMvc && !testRestTemplate && !directAssertion) continue;

            String testClass = owningType(method);
            String testMethod = method.getNameAsString();
            var testPosition = testAnnotation.get().getBegin().orElseThrow(() ->
                    new IllegalStateException("Parsed test annotation has no source location"));
            tests.add(new TestImplementationEvidence(
                    testClass,
                    testMethod,
                    displayName(unit, method),
                    relativePath,
                    testPosition.line,
                    testPosition.column,
                    testStyle(restAssured, mockMvc, testRestTemplate, directAssertion)));

            interactions.addAll(testRestTemplateInteractions);

            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                restAssuredInteraction(unit, call, endpointResolver, testClass, testMethod, relativePath)
                        .ifPresent(interactions::add);
                mockMvcInteraction(unit, method, call, endpointResolver,
                        testClass, testMethod, relativePath)
                        .ifPresent(interactions::add);
                assertions.addAll(helperResolver.resolve(
                        unit, call, testClass, testMethod, relativePath));
                if (!helperResolver.resolves(unit, call, testClass)) {
                    assertion(unit, method, call, testClass, testMethod, relativePath)
                            .ifPresent(assertions::add);
                }
            }
        }
    }

    private static Optional<AnnotationExpr> supportedTestAnnotation(
            CompilationUnit unit, MethodDeclaration method) {
        Optional<AnnotationExpr> test = annotation(unit, method, JUNIT_API, "Test");
        if (test.isPresent()) return test;
        Optional<AnnotationExpr> parameterized = annotation(unit, method, JUNIT_PARAMS, "ParameterizedTest");
        if (parameterized.isEmpty()) return Optional.empty();
        boolean hasSource = PARAMETER_SOURCES.stream().anyMatch(source ->
                annotation(unit, method, JUNIT_PROVIDERS, source).isPresent());
        return hasSource ? parameterized : Optional.empty();
    }

    private static String displayName(CompilationUnit unit, MethodDeclaration method) {
        return annotation(unit, method, JUNIT_API, "DisplayName")
                .filter(AnnotationExpr::isSingleMemberAnnotationExpr)
                .map(annotation -> annotation.asSingleMemberAnnotationExpr().getMemberValue())
                .filter(Expression::isStringLiteralExpr)
                .map(Expression::asStringLiteralExpr)
                .map(StringLiteralExpr::asString)
                .orElse(null);
    }

    private static Optional<HttpInteractionEvidence> restAssuredInteraction(
            CompilationUnit unit,
            MethodCallExpr call,
            StaticTestEndpointResolver endpointResolver,
            String testClass,
            String testMethod,
            String relativePath) {
        String methodName = call.getNameAsString().toLowerCase(Locale.ROOT);
        if (!HTTP_METHODS.contains(methodName) || !isRestAssuredHttpCall(unit, call)) return Optional.empty();
        if (call.getArguments().isEmpty()) return Optional.empty();
        Expression endpointExpression = call.getArgument(0);
        Optional<String> endpoint = endpointResolver.resolve(unit, endpointExpression);
        if (endpoint.isEmpty()) return Optional.empty();
        var position = call.getName().getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed HTTP call has no source location"));
        return Optional.of(new HttpInteractionEvidence(
                IntegrationHttpMethod.valueOf(methodName.toUpperCase(Locale.ROOT)),
                endpoint.get(),
                endpointExpression.toString(),
                testClass,
                testMethod,
                relativePath,
                position.line,
                position.column,
                null));
    }

    private static Optional<HttpInteractionEvidence> mockMvcInteraction(
            CompilationUnit unit,
            MethodDeclaration method,
            MethodCallExpr call,
            StaticTestEndpointResolver endpointResolver,
            String testClass,
            String testMethod,
            String relativePath) {
        if (!MockMvcEvidenceSupport.isMockMvcPerform(unit, method, call)) return Optional.empty();
        Optional<MethodCallExpr> builder = MockMvcEvidenceSupport.requestBuilder(unit, call);
        if (builder.isEmpty() || builder.get().getArguments().isEmpty()) return Optional.empty();
        Expression endpointExpression = builder.get().getArgument(0);
        Optional<String> endpoint = endpointResolver.resolve(unit, endpointExpression);
        if (endpoint.isEmpty()) return Optional.empty();
        var position = call.getName().getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed MockMvc perform call has no source location"));
        return Optional.of(new HttpInteractionEvidence(
                IntegrationHttpMethod.valueOf(builder.get().getNameAsString().toUpperCase(Locale.ROOT)),
                endpoint.get(),
                endpointExpression.toString(),
                testClass,
                testMethod,
                relativePath,
                position.line,
                position.column,
                call.getArgument(0).toString()));
    }

    private static Optional<AssertionEvidence> assertion(
            CompilationUnit unit,
            MethodDeclaration method,
            MethodCallExpr call,
            String testClass,
            String testMethod,
            String relativePath) {
        AssertionCategory category = null;
        AssertionEvidence.AssertionLibrary library = AssertionEvidence.AssertionLibrary.LEGACY;
        if (call.getNameAsString().equals("statusCode") && hasThenInScope(call)
                && containsRestAssuredRoot(unit, call)) {
            category = AssertionCategory.HTTP_STATUS;
            library = AssertionEvidence.AssertionLibrary.REST_ASSURED;
        } else if (call.getNameAsString().equals("body") && hasThenInScope(call)
                && containsRestAssuredRoot(unit, call)) {
            category = AssertionCategory.RESPONSE_BODY;
            library = AssertionEvidence.AssertionLibrary.REST_ASSURED;
        } else if (isDatabaseAssertionHelper(call)) {
            category = AssertionCategory.PERSISTENCE_DATABASE;
        } else if (containsMockMvcPerform(unit, method, call)) {
            category = MockMvcEvidenceSupport.assertionCategory(unit, call);
            if (category != null) library = AssertionEvidence.AssertionLibrary.MOCK_MVC;
        } else {
            var direct = DirectAssertionSupport.recognize(unit, call);
            if (direct.isPresent()) {
                var position = call.getName().getBegin().orElseThrow(() ->
                        new IllegalStateException("Parsed assertion has no source location"));
                return Optional.of(new AssertionEvidence(
                        directAssertionCategory(call),
                        direct.get().library(),
                        direct.get().kind(),
                        call.toString(),
                        testClass,
                        testMethod,
                        relativePath,
                        position.line,
                        position.column,
                        null));
            }
        }
        if (category == null) return Optional.empty();
        var position = call.getName().getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed assertion has no source location"));
        return Optional.of(new AssertionEvidence(
                category,
                library,
                call.getNameAsString(),
                call.toString(),
                testClass,
                testMethod,
                relativePath,
                position.line,
                position.column,
                null));
    }

    private static boolean containsRestAssuredCall(CompilationUnit unit, MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream().anyMatch(call ->
                HTTP_METHODS.contains(call.getNameAsString().toLowerCase(Locale.ROOT))
                        && isRestAssuredHttpCall(unit, call));
    }

    private static boolean isRestAssuredHttpCall(CompilationUnit unit, MethodCallExpr call) {
        if (isStaticRestAssuredImport(unit, call.getNameAsString()) && call.getScope().isEmpty()) return true;
        if (call.getScope().map(Object::toString).filter(name -> isRestAssuredScope(unit, name))
                .isPresent()) return true;
        return containsRestAssuredRoot(unit, call);
    }

    private static boolean containsRestAssuredRoot(CompilationUnit unit, MethodCallExpr call) {
        return call.findAll(MethodCallExpr.class).stream().anyMatch(candidate -> {
            String name = candidate.getNameAsString();
            if (!name.equals("given") && !name.equals("when")) return false;
            if (candidate.getScope().map(Object::toString)
                    .filter(scope -> isRestAssuredScope(unit, scope)).isPresent()) return true;
            return candidate.getScope().isEmpty() && isStaticRestAssuredImport(unit, name);
        });
    }

    private static boolean hasThenInScope(MethodCallExpr call) {
        return call.findAll(MethodCallExpr.class).stream()
                .anyMatch(candidate -> candidate.getNameAsString().equals("then"));
    }

    private static boolean isDatabaseAssertionHelper(MethodCallExpr call) {
        if (!call.getNameAsString().toLowerCase(Locale.ROOT).startsWith("assert")) return false;
        String scope = call.getScope().map(Object::toString).orElse("").toLowerCase(Locale.ROOT);
        return scope.contains("db") || scope.contains("database")
                || scope.contains("persistence") || scope.contains("repository");
    }

    private static boolean isStaticRestAssuredImport(CompilationUnit unit, String member) {
        return unit.getImports().stream().anyMatch(declaration -> declaration.isStatic()
                && (declaration.isAsterisk() && declaration.getNameAsString().equals(REST_ASSURED)
                || !declaration.isAsterisk()
                && declaration.getNameAsString().equals(REST_ASSURED + '.' + member)));
    }

    private static boolean isRestAssuredScope(CompilationUnit unit, String name) {
        if (name.equals(REST_ASSURED)) return true;
        if (!name.equals("RestAssured")) return false;
        return unit.getImports().stream().anyMatch(declaration -> !declaration.isStatic()
                && !declaration.isAsterisk()
                && declaration.getNameAsString().equals(REST_ASSURED));
    }

    private static Optional<AnnotationExpr> annotation(
            CompilationUnit unit,
            NodeWithAnnotations<?> node,
            String annotationPackage,
            String annotationName) {
        return node.getAnnotations().stream()
                .filter(candidate -> simpleName(candidate.getNameAsString()).equals(annotationName))
                .filter(candidate -> isImportedAnnotation(unit, candidate, annotationPackage, annotationName))
                .findFirst();
    }

    private static boolean isImportedAnnotation(
            CompilationUnit unit,
            AnnotationExpr annotation,
            String annotationPackage,
            String annotationName) {
        String qualifiedName = annotationPackage + '.' + annotationName;
        if (annotation.getNameAsString().contains(".")) {
            return annotation.getNameAsString().equals(qualifiedName);
        }
        return unit.getImports().stream().anyMatch(declaration ->
                importsAnnotation(declaration, annotationPackage, qualifiedName));
    }

    private static boolean importsAnnotation(
            ImportDeclaration declaration, String annotationPackage, String qualifiedName) {
        if (declaration.isStatic()) return false;
        if (declaration.isAsterisk()) return declaration.getNameAsString().equals(annotationPackage);
        return declaration.getNameAsString().equals(qualifiedName);
    }

    private static String owningType(MethodDeclaration method) {
        Optional<com.github.javaparser.ast.Node> current = method.getParentNode();
        while (current.isPresent()) {
            if (current.get() instanceof TypeDeclaration<?> type) {
                return type.getFullyQualifiedName().orElseGet(type::getNameAsString);
            }
            current = current.get().getParentNode();
        }
        throw new IllegalStateException("Test method has no owning Java type");
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private static AssertionCategory directAssertionCategory(MethodCallExpr assertion) {
        Set<String> calls = assertion.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (calls.contains("getStatusCode") || calls.contains("statusCode")) {
            return AssertionCategory.HTTP_STATUS;
        }
        if (calls.contains("getBody") || calls.contains("jsonPath") || calls.contains("asString")) {
            return AssertionCategory.RESPONSE_BODY;
        }
        return AssertionCategory.GENERAL_ASSERTION;
    }

    private static boolean containsMockMvcCall(CompilationUnit unit, MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream().anyMatch(call ->
                MockMvcEvidenceSupport.isMockMvcPerform(unit, method, call));
    }

    private static boolean containsDirectAssertion(CompilationUnit unit, MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> DirectAssertionSupport.recognize(unit, call).isPresent());
    }

    private static boolean containsMockMvcPerform(
            CompilationUnit unit, MethodDeclaration method, MethodCallExpr call) {
        return call.findAll(MethodCallExpr.class).stream().anyMatch(candidate ->
                MockMvcEvidenceSupport.isMockMvcPerform(unit, method, candidate));
    }

    private static String helperOrderKey(AssertionEvidence.HelperInvocationEvidence helper) {
        if (helper == null) return "";
        return helper.helperClass() + '\u0000' + helper.helperMethod() + '\u0000'
                + helper.repositoryRelativePath() + '\u0000' + helper.line() + '\u0000' + helper.column()
                + '\u0000' + helper.invocationExpression();
    }

    private static IntegrationTestStyle testStyle(
            boolean restAssured, boolean mockMvc, boolean testRestTemplate, boolean directAssertion) {
        if ((restAssured ? 1 : 0) + (mockMvc ? 1 : 0) + (testRestTemplate ? 1 : 0) > 1) {
            return IntegrationTestStyle.MIXED;
        }
        if (mockMvc) return IntegrationTestStyle.MOCK_MVC;
        if (restAssured) return IntegrationTestStyle.REST_ASSURED;
        if (testRestTemplate) return IntegrationTestStyle.TEST_REST_TEMPLATE;
        if (directAssertion) return IntegrationTestStyle.DIRECT_ASSERTION;
        throw new IllegalArgumentException("A supported test style is required");
    }
}
