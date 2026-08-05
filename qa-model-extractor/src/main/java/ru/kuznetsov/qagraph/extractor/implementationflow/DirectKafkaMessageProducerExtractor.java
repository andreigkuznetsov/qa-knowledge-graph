package ru.kuznetsov.qagraph.extractor.implementationflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DirectKafkaMessageProducerExtractor {
    private static final String KAFKA_TEMPLATE = "org.springframework.kafka.core.KafkaTemplate";
    private static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    public List<MessageProducerEvidence> extract(Path repositoryRoot, RestOperationEvidence operation)
            throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(operation, "operation");
        if (operation.sourceLocation() == null) return List.of();

        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path source = root.resolve(operation.sourceLocation().repositoryRelativePath()).normalize();
        if (!source.startsWith(root) || !Files.isRegularFile(source)) return List.of();
        CompilationUnit unit = parse(root, source);
        List<ClassOrInterfaceDeclaration> owners = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(type -> type.getNameAsString().equals(operation.controllerClass()))
                .filter(type -> type.getFullyQualifiedName().orElse("").equals(qualifiedController(operation)))
                .toList();
        if (owners.size() != 1) return List.of();
        ClassOrInterfaceDeclaration owner = owners.getFirst();
        List<MethodDeclaration> methods = owner.getMethodsByName(operation.controllerMethod()).stream()
                .filter(method -> method.getAnnotations().stream().anyMatch(annotation ->
                        annotation.getBegin().map(position -> position.line == operation.sourceLocation().line())
                                .orElse(false)))
                .toList();
        if (methods.size() != 1) return List.of();

        List<MessageProducerEvidence> evidence = new ArrayList<>();
        for (MethodCallExpr call : methods.getFirst().findAll(MethodCallExpr.class)) {
            if (!call.getNameAsString().equals("send") || call.getScope().isEmpty()
                    || !call.getScope().orElseThrow().isNameExpr()) continue;
            String fieldName = call.getScope().orElseThrow().asNameExpr().getNameAsString();
            List<FieldDeclaration> fields = owner.getFields().stream()
                    .filter(field -> field.getVariables().stream()
                            .anyMatch(variable -> variable.getNameAsString().equals(fieldName)))
                    .filter(field -> isKafkaTemplate(unit, field))
                    .toList();
            if (fields.size() != 1) continue;
            Optional<DependencyInjectionKind> injection = injectionKind(unit, owner, fields.getFirst(), fieldName);
            if (injection.isEmpty() || call.getBegin().isEmpty()) continue;
            var position = call.getBegin().orElseThrow();
            evidence.add(new MessageProducerEvidence(
                    operation,
                    qualifiedController(operation),
                    methods.getFirst().getNameAsString(),
                    fieldName,
                    call.getNameAsString(),
                    injection.get(),
                    new SourceLocation(operation.sourceLocation().repositoryRelativePath(),
                            position.line, position.column)));
        }
        return evidence.stream()
                .sorted(Comparator.comparing((MessageProducerEvidence value) -> value.ownerClass())
                        .thenComparing(MessageProducerEvidence::ownerMethod)
                        .thenComparing(MessageProducerEvidence::producerField)
                        .thenComparing(value -> value.publishingLocation().line())
                        .thenComparing(value -> value.publishingLocation().column()))
                .collect(java.util.stream.Collectors.toMap(
                        value -> value.ownerClass() + '#' + value.ownerMethod(),
                        value -> value,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream().toList();
    }

    private CompilationUnit parse(Path root, Path source) throws IOException {
        var result = parser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            throw new IOException("Cannot parse Java source " + root.relativize(source)
                    + ": " + result.getProblems());
        }
        return result.getResult().orElseThrow();
    }

    private static boolean isKafkaTemplate(CompilationUnit unit, FieldDeclaration field) {
        if (!field.getElementType().isClassOrInterfaceType()) return false;
        String declared = field.getElementType().asClassOrInterfaceType().getNameWithScope();
        if (declared.equals(KAFKA_TEMPLATE)) return true;
        return declared.equals("KafkaTemplate") && unit.getImports().stream().anyMatch(value ->
                !value.isStatic() && !value.isAsterisk() && value.getNameAsString().equals(KAFKA_TEMPLATE));
    }

    private static Optional<DependencyInjectionKind> injectionKind(
            CompilationUnit unit,
            ClassOrInterfaceDeclaration owner,
            FieldDeclaration field,
            String fieldName) {
        if (hasAnnotation(unit, field, AUTOWIRED)) {
            return Optional.of(DependencyInjectionKind.AUTOWIRED_FIELD);
        }
        boolean uninitialized = field.getVariables().stream()
                .filter(variable -> variable.getNameAsString().equals(fieldName))
                .allMatch(variable -> variable.getInitializer().isEmpty());
        if (field.isFinal() && uninitialized && owner.getAnnotations().stream().anyMatch(annotation ->
                annotation.getNameAsString().equals("RequiredArgsConstructor")
                        || annotation.getNameAsString().equals("AllArgsConstructor"))) {
            return Optional.of(DependencyInjectionKind.LOMBOK_CONSTRUCTOR);
        }
        if (owner.getConstructors().stream().anyMatch(constructor ->
                constructorInjects(constructor, fieldName, field.getElementType().asString()))) {
            return Optional.of(DependencyInjectionKind.CONSTRUCTOR);
        }
        return Optional.empty();
    }

    private static boolean hasAnnotation(CompilationUnit unit, FieldDeclaration field, String qualifiedName) {
        return field.getAnnotations().stream().anyMatch(annotation ->
                annotation.getNameAsString().equals(qualifiedName)
                        || annotation.getNameAsString().equals("Autowired")
                        && unit.getImports().stream().anyMatch(value ->
                        !value.isStatic() && !value.isAsterisk()
                                && value.getNameAsString().equals(qualifiedName)));
    }

    private static boolean constructorInjects(ConstructorDeclaration constructor, String fieldName, String fieldType) {
        boolean parameter = constructor.getParameters().stream().anyMatch(value ->
                value.getNameAsString().equals(fieldName) && value.getType().asString().equals(fieldType));
        return parameter && constructor.findAll(AssignExpr.class).stream().anyMatch(assignment ->
                assignment.getTarget().toString().equals("this." + fieldName)
                        && assignment.getValue() instanceof NameExpr name
                        && name.getNameAsString().equals(fieldName));
    }

    private static String qualifiedController(RestOperationEvidence operation) {
        return operation.javaPackage().isBlank()
                ? operation.controllerClass()
                : operation.javaPackage() + '.' + operation.controllerClass();
    }
}
