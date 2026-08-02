package ru.kuznetsov.qagraph.extractor.implementationflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class DirectImplementationFlowExtractor {
    private static final Set<String> MAPPINGS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");
    private static final Set<String> SPRING_DATA_BASES = Set.of(
            "Repository", "CrudRepository", "ListCrudRepository", "PagingAndSortingRepository",
            "ListPagingAndSortingRepository", "JpaRepository", "MongoRepository", "ReactiveCrudRepository");
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    public Optional<ImplementationFlowEvidence> extract(
            Path repositoryRoot, RestOperationEvidence operation) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(operation, "operation");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot) || operation.sourceLocation() == null) return Optional.empty();
        Index index = index(root, sourceRoot);
        List<TypeInfo> controllerTypes = index.types().values().stream()
                .filter(type -> type.simpleName().equals(operation.controllerClass()))
                .filter(type -> type.packageName().equals(operation.javaPackage()))
                .filter(type -> type.path().equals(operation.sourceLocation().repositoryRelativePath()))
                .toList();
        if (controllerTypes.size() != 1) return Optional.empty();
        TypeInfo controller = controllerTypes.getFirst();
        List<MethodDeclaration> controllerMethods = controller.declaration().getMethodsByName(
                        operation.controllerMethod()).stream()
                .filter(method -> hasMappingAtLine(method, operation.sourceLocation().line()))
                .toList();
        if (controllerMethods.size() != 1) return Optional.empty();

        Optional<ResolvedCall> serviceCall = uniqueDependencyCall(
                controller, controllerMethods.getFirst(), index, TargetKind.SERVICE);
        if (serviceCall.isEmpty()) return Optional.empty();
        ResolvedCall service = serviceCall.get();
        List<MethodDeclaration> serviceMethods = service.target().declaration()
                .getMethodsByName(service.call().getNameAsString()).stream()
                .filter(method -> method.getParameters().size() == service.call().getArguments().size())
                .toList();
        if (serviceMethods.size() != 1) return Optional.empty();
        MethodDeclaration serviceMethod = serviceMethods.getFirst();
        if (isRecursive(serviceMethod)) return Optional.empty();
        Optional<ResolvedCall> repositoryCall = uniqueDependencyCall(
                service.target(), serviceMethod, index, TargetKind.REPOSITORY);
        if (repositoryCall.isEmpty()) return Optional.empty();
        ResolvedCall repository = repositoryCall.get();

        return Optional.of(new ImplementationFlowEvidence(
                operation,
                controller.qualifiedName(),
                controllerMethods.getFirst().getNameAsString(),
                service.target().qualifiedName(),
                serviceMethod.getNameAsString(),
                repository.target().qualifiedName(),
                repository.call().getNameAsString(),
                ImplementationInvocationKind.DIRECT_FIELD_METHOD_INVOCATION,
                service.injectionKind(),
                repository.injectionKind(),
                location(controller.path(), service.call()),
                location(service.target().path(), serviceMethod.getName()),
                location(service.target().path(), repository.call()),
                location(repository.target().path(), repository.target().declaration().getName())));
    }

    private Optional<ResolvedCall> uniqueDependencyCall(
            TypeInfo owner, MethodDeclaration method, Index index, TargetKind kind) {
        List<ResolvedCall> calls = new ArrayList<>();
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || !call.getScope().orElseThrow().isNameExpr()) continue;
            String fieldName = call.getScope().orElseThrow().asNameExpr().getNameAsString();
            List<FieldDeclaration> fields = owner.declaration().getFields().stream()
                    .filter(field -> field.getVariables().stream().anyMatch(variable ->
                            variable.getNameAsString().equals(fieldName)))
                    .toList();
            if (fields.size() != 1) continue;
            FieldDeclaration field = fields.getFirst();
            Optional<DependencyInjectionKind> injection = injectionKind(owner, field, fieldName);
            if (injection.isEmpty()) continue;
            Optional<TypeInfo> target = resolveType(owner.unit(), field.getElementType().asString(), index);
            if (target.isEmpty() || !matchesKind(target.get(), kind)) continue;
            calls.add(new ResolvedCall(call, target.get(), injection.get()));
        }
        List<String> targets = calls.stream().map(call -> call.target().qualifiedName()).distinct().toList();
        if (targets.size() != 1 || calls.isEmpty()) return Optional.empty();
        if (kind == TargetKind.SERVICE && calls.stream()
                .map(call -> call.call().getNameAsString() + '/' + call.call().getArguments().size())
                .distinct().count() != 1) return Optional.empty();
        return calls.stream().min(Comparator
                .comparingInt((ResolvedCall call) -> call.call().getBegin().orElseThrow().line)
                .thenComparingInt(call -> call.call().getBegin().orElseThrow().column));
    }

    private static boolean matchesKind(TypeInfo type, TargetKind kind) {
        if (kind == TargetKind.SERVICE) {
            return !type.declaration().isInterface()
                    && hasAnnotation(type.unit(), type.declaration().getAnnotations(),
                    "org.springframework.stereotype.Service");
        }
        if (type.declaration().isInterface()) {
            return type.declaration().getExtendedTypes().stream().anyMatch(parent ->
                    SPRING_DATA_BASES.contains(parent.getNameAsString()));
        }
        return hasAnnotation(type.unit(), type.declaration().getAnnotations(),
                "org.springframework.stereotype.Repository");
    }

    private static Optional<DependencyInjectionKind> injectionKind(
            TypeInfo owner, FieldDeclaration field, String fieldName) {
        if (hasAnnotation(owner.unit(), field.getAnnotations(),
                "org.springframework.beans.factory.annotation.Autowired")) {
            return Optional.of(DependencyInjectionKind.AUTOWIRED_FIELD);
        }
        boolean uninitializedField = field.getVariables().stream()
                .filter(variable -> variable.getNameAsString().equals(fieldName))
                .allMatch(variable -> variable.getInitializer().isEmpty());
        if (field.isFinal() && uninitializedField
                && (hasAnnotation(owner.unit(), owner.declaration().getAnnotations(),
                "lombok.RequiredArgsConstructor")
                || hasAnnotation(owner.unit(), owner.declaration().getAnnotations(),
                "lombok.AllArgsConstructor"))) {
            return Optional.of(DependencyInjectionKind.LOMBOK_CONSTRUCTOR);
        }
        if (owner.declaration().getConstructors().stream().anyMatch(constructor ->
                constructorInjects(constructor, fieldName, field.getElementType().asString()))) {
            return Optional.of(DependencyInjectionKind.CONSTRUCTOR);
        }
        return Optional.empty();
    }

    private static boolean isRecursive(MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream().anyMatch(call ->
                call.getNameAsString().equals(method.getNameAsString())
                        && call.getArguments().size() == method.getParameters().size()
                        && (call.getScope().isEmpty()
                        || call.getScope().map(Object::toString).orElse("").equals("this")));
    }

    private static boolean constructorInjects(
            ConstructorDeclaration constructor, String fieldName, String fieldType) {
        boolean parameter = constructor.getParameters().stream().anyMatch(value ->
                value.getNameAsString().equals(fieldName) && value.getType().asString().equals(fieldType));
        if (!parameter) return false;
        return constructor.findAll(AssignExpr.class).stream().anyMatch(assignment ->
                assignment.getTarget().toString().equals("this." + fieldName)
                        && assignment.getValue() instanceof NameExpr name
                        && name.getNameAsString().equals(fieldName));
    }

    private Optional<TypeInfo> resolveType(CompilationUnit unit, String declared, Index index) {
        List<TypeInfo> candidates = new ArrayList<>();
        if (declared.contains(".") && index.types().containsKey(declared)) {
            candidates.add(index.types().get(declared));
        }
        unit.getImports().stream()
                .filter(value -> !value.isStatic() && !value.isAsterisk())
                .map(ImportDeclaration::getNameAsString)
                .filter(value -> simpleName(value).equals(simpleName(declared)))
                .map(index.types()::get)
                .filter(Objects::nonNull)
                .forEach(candidates::add);
        unit.getImports().stream()
                .filter(value -> !value.isStatic() && value.isAsterisk())
                .map(value -> value.getNameAsString() + '.' + simpleName(declared))
                .map(index.types()::get)
                .filter(Objects::nonNull)
                .forEach(candidates::add);
        String samePackage = unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + simpleName(declared))
                .orElse(simpleName(declared));
        if (index.types().containsKey(samePackage)) candidates.add(index.types().get(samePackage));
        List<TypeInfo> unique = candidates.stream().distinct().toList();
        return unique.size() == 1 ? Optional.of(unique.getFirst()) : Optional.empty();
    }

    private Index index(Path root, Path sourceRoot) throws IOException {
        Map<String, TypeInfo> types = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }
        for (Path file : files) {
            var result = parser.parse(file);
            CompilationUnit unit = result.getResult().orElseThrow(() ->
                    new IOException("Cannot parse Java source " + root.relativize(file)));
            if (!result.isSuccessful()) {
                throw new IOException("Cannot parse Java source " + root.relativize(file)
                        + ": " + result.getProblems());
            }
            String path = root.relativize(file).toString().replace('\\', '/');
            for (ClassOrInterfaceDeclaration declaration : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                declaration.getFullyQualifiedName().ifPresent(name -> types.put(name, new TypeInfo(
                        name, simpleName(name), packageName(name), unit, declaration, path)));
            }
        }
        return new Index(Map.copyOf(types));
    }

    private static boolean hasMappingAtLine(MethodDeclaration method, int line) {
        return method.getAnnotations().stream().anyMatch(annotation ->
                MAPPINGS.contains(simpleName(annotation.getNameAsString()))
                        && annotation.getBegin().map(position -> position.line == line).orElse(false));
    }

    private static boolean hasAnnotation(
            CompilationUnit unit, List<AnnotationExpr> annotations, String qualifiedName) {
        return annotations.stream().anyMatch(annotation -> {
            String declared = annotation.getNameAsString();
            if (declared.contains(".")) return declared.equals(qualifiedName);
            if (!declared.equals(simpleName(qualifiedName))) return false;
            return unit.getImports().stream().anyMatch(value -> !value.isStatic()
                    && (value.isAsterisk() && value.getNameAsString().equals(packageName(qualifiedName))
                    || !value.isAsterisk() && value.getNameAsString().equals(qualifiedName)));
        });
    }

    private static SourceLocation location(String path, com.github.javaparser.ast.Node node) {
        var position = node.getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed implementation evidence has no source location"));
        return new SourceLocation(path, position.line, position.column);
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private static String packageName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(0, separator);
    }

    private enum TargetKind { SERVICE, REPOSITORY }

    private record TypeInfo(
            String qualifiedName,
            String simpleName,
            String packageName,
            CompilationUnit unit,
            ClassOrInterfaceDeclaration declaration,
            String path) {
    }

    private record ResolvedCall(
            MethodCallExpr call, TypeInfo target, DependencyInjectionKind injectionKind) {
    }

    private record Index(Map<String, TypeInfo> types) {
    }
}
