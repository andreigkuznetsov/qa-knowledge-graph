package ru.kuznetsov.qagraph.extractor.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class BeanValidationEvidenceExtractor {
    private static final Set<String> VALIDATION_PACKAGES = Set.of(
            "jakarta.validation.constraints", "javax.validation.constraints");
    private static final Set<String> SUPPORTED_ANNOTATIONS = Set.of(
            "NotNull", "NotBlank", "NotEmpty", "Size", "Min", "Max", "Pattern", "Email");
    private static final Comparator<BeanValidationEvidence> STABLE_ORDER = Comparator
            .comparing(BeanValidationEvidence::owningJavaType)
            .thenComparing(BeanValidationEvidence::memberName)
            .thenComparing(BeanValidationEvidence::annotationType)
            .thenComparing(BeanValidationEvidence::repositoryRelativePath)
            .thenComparingInt(BeanValidationEvidence::line)
            .thenComparingInt(BeanValidationEvidence::column)
            .thenComparing(evidence -> evidence.declaredAttributes().toString());

    private final JavaParser parser;

    public BeanValidationEvidenceExtractor() {
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    public List<BeanValidationEvidence> extract(Path repositoryRoot) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = normalizedRoot.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) return List.of();

        List<Path> sourceFiles;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path ->
                            normalizedRoot.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }

        List<BeanValidationEvidence> evidence = new ArrayList<>();
        for (Path sourceFile : sourceFiles) {
            evidence.addAll(extractFile(normalizedRoot, sourceFile));
        }
        evidence.sort(STABLE_ORDER);
        return List.copyOf(evidence);
    }

    private List<BeanValidationEvidence> extractFile(Path repositoryRoot, Path sourceFile) throws IOException {
        ParseResult<CompilationUnit> result = parser.parse(sourceFile);
        CompilationUnit unit = result.getResult().orElseThrow(() ->
                new IOException("Cannot parse Java source " + repositoryRoot.relativize(sourceFile)));
        if (!result.isSuccessful()) {
            throw new IOException("Cannot parse Java source " + repositoryRoot.relativize(sourceFile)
                    + ": " + result.getProblems());
        }

        String relativePath = repositoryRoot.relativize(sourceFile).toString().replace('\\', '/');
        List<BeanValidationEvidence> evidence = new ArrayList<>();

        for (FieldDeclaration field : unit.findAll(FieldDeclaration.class)) {
            String owningType = owningType(field);
            for (var variable : field.getVariables()) {
                evidence.addAll(extractAnnotations(
                        unit, field, owningType, variable.getNameAsString(), relativePath));
            }
        }

        for (RecordDeclaration record : unit.findAll(RecordDeclaration.class)) {
            String owningType = qualifiedName(record);
            for (var component : record.getParameters()) {
                evidence.addAll(extractAnnotations(
                        unit, component, owningType, component.getNameAsString(), relativePath));
            }
        }
        return evidence;
    }

    private static List<BeanValidationEvidence> extractAnnotations(
            CompilationUnit unit,
            NodeWithAnnotations<?> annotatedNode,
            String owningType,
            String memberName,
            String relativePath) {
        List<BeanValidationEvidence> evidence = new ArrayList<>();
        for (AnnotationExpr annotation : annotatedNode.getAnnotations()) {
            Optional<String> annotationType = supportedAnnotationType(unit, annotation);
            if (annotationType.isEmpty()) continue;
            var position = annotation.getBegin().orElseThrow(() ->
                    new IllegalStateException("Parsed annotation has no source location"));
            Map<String, String> attributes = declaredAttributes(annotation);
            evidence.add(new BeanValidationEvidence(
                    owningType,
                    memberName,
                    annotationType.get(),
                    attributes,
                    explicitMessage(annotation),
                    relativePath,
                    position.line,
                    position.column));
        }
        return evidence;
    }

    private static Optional<String> supportedAnnotationType(
            CompilationUnit unit, AnnotationExpr annotation) {
        String declaredName = annotation.getNameAsString();
        String simpleName = simpleName(declaredName);
        if (!SUPPORTED_ANNOTATIONS.contains(simpleName)) return Optional.empty();

        for (String validationPackage : VALIDATION_PACKAGES) {
            String qualifiedName = validationPackage + '.' + simpleName;
            if (declaredName.equals(qualifiedName)
                    || unit.getImports().stream().anyMatch(importDeclaration ->
                    importsAnnotation(importDeclaration, validationPackage, qualifiedName))) {
                return Optional.of(qualifiedName);
            }
        }
        return Optional.empty();
    }

    private static boolean importsAnnotation(
            ImportDeclaration declaration, String validationPackage, String qualifiedName) {
        if (declaration.isStatic()) return false;
        if (declaration.isAsterisk()) {
            return declaration.getNameAsString().equals(validationPackage);
        }
        return declaration.getNameAsString().equals(qualifiedName);
    }

    private static Map<String, String> declaredAttributes(AnnotationExpr annotation) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (annotation.isSingleMemberAnnotationExpr()) {
            attributes.put("value", annotation.asSingleMemberAnnotationExpr().getMemberValue().toString());
        } else if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .sorted(Comparator.comparing(MemberValuePair::getNameAsString))
                    .forEach(pair -> attributes.put(pair.getNameAsString(), pair.getValue().toString()));
        }
        return attributes;
    }

    private static String explicitMessage(AnnotationExpr annotation) {
        if (!annotation.isNormalAnnotationExpr()) return null;
        return annotation.asNormalAnnotationExpr().getPairs().stream()
                .filter(pair -> pair.getNameAsString().equals("message"))
                .map(MemberValuePair::getValue)
                .map(BeanValidationEvidenceExtractor::messageValue)
                .findFirst()
                .orElse(null);
    }

    private static String messageValue(Expression expression) {
        return expression.isStringLiteralExpr()
                ? expression.asStringLiteralExpr().asString()
                : expression.toString();
    }

    private static String owningType(Node node) {
        Optional<Node> current = node.getParentNode();
        while (current.isPresent()) {
            if (current.get() instanceof TypeDeclaration<?> type) return qualifiedName(type);
            current = current.get().getParentNode();
        }
        throw new IllegalStateException("Field has no owning Java type");
    }

    private static String qualifiedName(TypeDeclaration<?> type) {
        return type.getFullyQualifiedName().orElseGet(type::getNameAsString);
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }
}
