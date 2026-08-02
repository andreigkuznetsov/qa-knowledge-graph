package ru.kuznetsov.qagraph.extractor.rest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class SpringMvcRestOperationScanner {
    private static final String SPRING_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation";
    private static final Set<String> SUPPORTED_MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");
    private static final Comparator<RestOperationEvidence> STABLE_ORDER = Comparator
            .comparing(RestOperationEvidence::endpointPath)
            .thenComparing(operation -> operation.httpMethod().name())
            .thenComparing(RestOperationEvidence::javaPackage)
            .thenComparing(RestOperationEvidence::controllerClass)
            .thenComparing(RestOperationEvidence::controllerMethod)
            .thenComparing(operation -> operation.sourceLocation().repositoryRelativePath())
            .thenComparingInt(operation -> operation.sourceLocation().line())
            .thenComparingInt(operation -> operation.sourceLocation().column());

    private final JavaParser parser;

    public SpringMvcRestOperationScanner() {
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    public List<RestOperationEvidence> scan(Path repositoryRoot) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = normalizedRoot.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) return List.of();

        List<Path> sourceFiles;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> normalizedRoot.relativize(path).toString()))
                    .toList();
        }

        List<RestOperationEvidence> operations = new ArrayList<>();
        for (Path sourceFile : sourceFiles) {
            operations.addAll(scanFile(normalizedRoot, sourceFile));
        }
        operations.sort(STABLE_ORDER);
        return List.copyOf(operations);
    }

    private List<RestOperationEvidence> scanFile(Path repositoryRoot, Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = parser.parse(sourceFile);
        CompilationUnit unit = result.getResult().orElseThrow(() ->
                new IOException("Cannot parse Java source " + repositoryRoot.relativize(sourceFile)));
        if (!result.isSuccessful()) {
            throw new IOException("Cannot parse Java source " + repositoryRoot.relativize(sourceFile)
                    + ": " + result.getProblems());
        }

        String javaPackage = unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString()).orElse("");
        String relativePath = repositoryRoot.relativize(sourceFile).toString().replace('\\', '/');
        List<RestOperationEvidence> operations = new ArrayList<>();

        for (ClassOrInterfaceDeclaration controller : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            if (controller.isInterface() || !hasSpringAnnotation(unit, controller.getAnnotations(), "RestController")) {
                continue;
            }
            List<String> basePaths = mappingPaths(unit, controller.getAnnotations(), "RequestMapping");
            if (basePaths.isEmpty()) basePaths = List.of("");

            for (MethodDeclaration method : controller.getMethods()) {
                for (Mapping mapping : methodMappings(unit, method)) {
                    SourceLocation location = sourceLocation(relativePath, mapping.annotation());
                    for (String basePath : basePaths) {
                        for (String methodPath : mapping.paths()) {
                            for (RestHttpMethod httpMethod : mapping.httpMethods()) {
                                operations.add(new RestOperationEvidence(
                                        httpMethod,
                                        combinePaths(basePath, methodPath),
                                        controller.getNameAsString(),
                                        method.getNameAsString(),
                                        javaPackage,
                                        location));
                            }
                        }
                    }
                }
            }
        }
        return operations;
    }

    private static List<Mapping> methodMappings(CompilationUnit unit, MethodDeclaration method) {
        List<Mapping> mappings = new ArrayList<>();
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String simpleName = simpleName(annotation.getNameAsString());
            if (!SUPPORTED_MAPPING_ANNOTATIONS.contains(simpleName)
                    || !isSpringAnnotation(unit, annotation, simpleName)) {
                continue;
            }
            List<String> paths = paths(annotation);
            if (paths.isEmpty()) paths = List.of("");
            EnumSet<RestHttpMethod> methods = httpMethods(simpleName, annotation);
            if (!methods.isEmpty()) mappings.add(new Mapping(annotation, paths, methods));
        }
        return mappings;
    }

    private static EnumSet<RestHttpMethod> httpMethods(String annotationName, AnnotationExpr annotation) {
        return switch (annotationName) {
            case "GetMapping" -> EnumSet.of(RestHttpMethod.GET);
            case "PostMapping" -> EnumSet.of(RestHttpMethod.POST);
            case "PutMapping" -> EnumSet.of(RestHttpMethod.PUT);
            case "DeleteMapping" -> EnumSet.of(RestHttpMethod.DELETE);
            case "PatchMapping" -> EnumSet.of(RestHttpMethod.PATCH);
            case "RequestMapping" -> requestMappingMethods(annotation);
            default -> EnumSet.noneOf(RestHttpMethod.class);
        };
    }

    private static EnumSet<RestHttpMethod> requestMappingMethods(AnnotationExpr annotation) {
        Optional<Expression> methodValue = memberValue(annotation, "method");
        if (methodValue.isEmpty()) return EnumSet.noneOf(RestHttpMethod.class);
        EnumSet<RestHttpMethod> methods = EnumSet.noneOf(RestHttpMethod.class);
        for (Expression expression : flatten(methodValue.get())) {
            String value = expression.toString();
            int separator = value.lastIndexOf('.');
            String methodName = (separator >= 0 ? value.substring(separator + 1) : value).toUpperCase(Locale.ROOT);
            try {
                methods.add(RestHttpMethod.valueOf(methodName));
            } catch (IllegalArgumentException ignored) {
                // Unsupported HTTP methods are deliberately not evidence for this slice.
            }
        }
        return methods;
    }

    private static List<String> mappingPaths(CompilationUnit unit, List<AnnotationExpr> annotations,
                                             String annotationName) {
        for (AnnotationExpr annotation : annotations) {
            if (isSpringAnnotation(unit, annotation, annotationName)) return paths(annotation);
        }
        return List.of();
    }

    private static List<String> paths(AnnotationExpr annotation) {
        Optional<Expression> pathValue = memberValue(annotation, "path")
                .or(() -> memberValue(annotation, "value"))
                .or(() -> annotation.isSingleMemberAnnotationExpr()
                        ? Optional.of(annotation.asSingleMemberAnnotationExpr().getMemberValue())
                        : Optional.empty());
        if (pathValue.isEmpty()) return List.of();
        return flatten(pathValue.get()).stream()
                .filter(Expression::isStringLiteralExpr)
                .map(Expression::asStringLiteralExpr)
                .map(StringLiteralExpr::asString)
                .distinct()
                .sorted()
                .toList();
    }

    private static Optional<Expression> memberValue(AnnotationExpr annotation, String name) {
        if (!annotation.isNormalAnnotationExpr()) return Optional.empty();
        return annotation.asNormalAnnotationExpr().getPairs().stream()
                .filter(pair -> pair.getNameAsString().equals(name))
                .map(MemberValuePair::getValue)
                .findFirst();
    }

    private static List<Expression> flatten(Expression expression) {
        if (expression.isArrayInitializerExpr()) {
            ArrayInitializerExpr array = expression.asArrayInitializerExpr();
            return List.copyOf(array.getValues());
        }
        return List.of(expression);
    }

    private static boolean hasSpringAnnotation(CompilationUnit unit, List<AnnotationExpr> annotations,
                                               String annotationName) {
        return annotations.stream().anyMatch(annotation -> isSpringAnnotation(unit, annotation, annotationName));
    }

    private static boolean isSpringAnnotation(CompilationUnit unit, AnnotationExpr annotation,
                                              String annotationName) {
        if (!simpleName(annotation.getNameAsString()).equals(annotationName)) return false;
        String qualifiedName = SPRING_ANNOTATION_PACKAGE + '.' + annotationName;
        if (annotation.getNameAsString().equals(qualifiedName)) return true;
        return unit.getImports().stream().anyMatch(importDeclaration ->
                importsAnnotation(importDeclaration, annotationName, qualifiedName));
    }

    private static boolean importsAnnotation(ImportDeclaration declaration, String annotationName,
                                             String qualifiedName) {
        if (declaration.isStatic()) return false;
        if (declaration.isAsterisk()) {
            return declaration.getNameAsString().equals(SPRING_ANNOTATION_PACKAGE);
        }
        return declaration.getNameAsString().equals(qualifiedName);
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private static SourceLocation sourceLocation(String relativePath, Node node) {
        return node.getBegin()
                .map(position -> new SourceLocation(relativePath, position.line, position.column))
                .orElseThrow(() -> new IllegalStateException("Parsed annotation has no source location"));
    }

    private static String combinePaths(String basePath, String methodPath) {
        String combined = ("/" + basePath + "/" + methodPath).replaceAll("/{2,}", "/");
        if (combined.length() > 1 && combined.endsWith("/")) {
            return combined.substring(0, combined.length() - 1);
        }
        return combined;
    }

    private record Mapping(AnnotationExpr annotation, List<String> paths,
                           EnumSet<RestHttpMethod> httpMethods) {
    }
}
