package ru.kuznetsov.qagraph.extractor.implementationflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
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

public final class DirectApplicationServiceRepositoryExtractor {
    private static final Set<String> SPRING_DATA_BASES = Set.of(
            "Repository", "CrudRepository", "ListCrudRepository", "PagingAndSortingRepository",
            "ListPagingAndSortingRepository", "JpaRepository", "MongoRepository", "ReactiveCrudRepository");
    private static final Set<String> INHERITED_METHODS = Set.of(
            "save", "saveAll", "findById", "existsById", "findAll", "findAllById", "count",
            "deleteById", "delete", "deleteAllById", "deleteAll");
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    public List<ApplicationServiceRepositoryEvidence> extract(
            Path repositoryRoot, List<ConsumerApplicationServiceEvidence> services) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(services, "services");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot) || services.isEmpty()) return List.of();
        Index index = index(root, sourceRoot);
        List<ApplicationServiceRepositoryEvidence> result = new ArrayList<>();
        for (ConsumerApplicationServiceEvidence service : services) {
            TypeInfo owner = index.types().get(service.serviceClass());
            if (owner == null) continue;
            List<MethodDeclaration> methods = owner.declaration().getMethodsByName(service.serviceMethod());
            if (methods.size() > 1) {
                methods = methods.stream().filter(method ->
                        sameLocation(method.getName(), service.serviceMethodLocation())).toList();
            }
            if (methods.size() != 1) continue;
            List<ResolvedCall> calls = qualifiedCalls(owner, methods.getFirst(), index);
            List<String> repositories = calls.stream().map(value -> value.target().qualifiedName()).distinct().toList();
            if (repositories.size() != 1) continue;
            TypeInfo repository = calls.getFirst().target();
            List<ApplicationServiceRepositoryEvidence.RepositoryInvocationEvidence> invocations = calls.stream()
                    .sorted(Comparator.comparingInt((ResolvedCall value) -> value.call().getBegin().orElseThrow().line)
                            .thenComparingInt(value -> value.call().getBegin().orElseThrow().column))
                    .map(value -> new ApplicationServiceRepositoryEvidence.RepositoryInvocationEvidence(
                            value.call().getNameAsString(), location(owner.path(), value.call())))
                    .distinct().toList();
            result.add(new ApplicationServiceRepositoryEvidence(
                    service, repository.qualifiedName(), location(repository.path(), repository.declaration().getName()),
                    calls.getFirst().injectionKind(), invocations));
        }
        result.sort(Comparator.comparing((ApplicationServiceRepositoryEvidence value) ->
                        value.applicationService().serviceClass())
                .thenComparing(value -> value.applicationService().serviceMethod())
                .thenComparing(ApplicationServiceRepositoryEvidence::repositoryClass));
        return List.copyOf(result);
    }

    private static List<ResolvedCall> qualifiedCalls(TypeInfo owner, MethodDeclaration method, Index index) {
        List<ResolvedCall> result = new ArrayList<>();
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || !call.getScope().orElseThrow().isNameExpr()) continue;
            String fieldName = call.getScope().orElseThrow().asNameExpr().getNameAsString();
            List<FieldDeclaration> fields = owner.declaration().getFields().stream()
                    .filter(field -> field.getVariables().stream()
                            .anyMatch(variable -> variable.getNameAsString().equals(fieldName))).toList();
            if (fields.size() != 1) continue;
            FieldDeclaration field = fields.getFirst();
            Optional<DependencyInjectionKind> injection = injectionKind(owner, field, fieldName);
            Optional<TypeInfo> target = resolveType(owner.unit(), field.getElementType().asString(), index);
            if (injection.isEmpty() || target.isEmpty() || !isRepository(target.get())
                    || !resolvesMethod(target.get(), call)) continue;
            result.add(new ResolvedCall(call, target.get(), injection.get()));
        }
        return result;
    }

    private static boolean resolvesMethod(TypeInfo repository, MethodCallExpr call) {
        long declarations = repository.declaration().getMethodsByName(call.getNameAsString()).stream()
                .filter(method -> method.getParameters().size() == call.getArguments().size()).count();
        return declarations == 1 || declarations == 0 && INHERITED_METHODS.contains(call.getNameAsString())
                && repository.declaration().getExtendedTypes().stream()
                .anyMatch(parent -> SPRING_DATA_BASES.contains(parent.getNameAsString()));
    }

    private static boolean isRepository(TypeInfo type) {
        if (type.declaration().isInterface()) return type.declaration().getExtendedTypes().stream()
                .anyMatch(parent -> SPRING_DATA_BASES.contains(parent.getNameAsString()));
        return hasAnnotation(type.unit(), type.declaration().getAnnotations(),
                "org.springframework.stereotype.Repository");
    }

    private static Optional<DependencyInjectionKind> injectionKind(
            TypeInfo owner, FieldDeclaration field, String fieldName) {
        if (hasAnnotation(owner.unit(), field.getAnnotations(),
                "org.springframework.beans.factory.annotation.Autowired")) {
            return Optional.of(DependencyInjectionKind.AUTOWIRED_FIELD);
        }
        boolean uninitialized = field.getVariables().stream()
                .filter(variable -> variable.getNameAsString().equals(fieldName))
                .allMatch(variable -> variable.getInitializer().isEmpty());
        if (field.isFinal() && uninitialized && (hasAnnotation(owner.unit(), owner.declaration().getAnnotations(),
                "lombok.RequiredArgsConstructor") || hasAnnotation(owner.unit(), owner.declaration().getAnnotations(),
                "lombok.AllArgsConstructor"))) return Optional.of(DependencyInjectionKind.LOMBOK_CONSTRUCTOR);
        if (owner.declaration().getConstructors().stream()
                .anyMatch(constructor -> constructorInjects(constructor, fieldName, field.getElementType().asString()))) {
            return Optional.of(DependencyInjectionKind.CONSTRUCTOR);
        }
        return Optional.empty();
    }

    private static boolean constructorInjects(ConstructorDeclaration constructor, String name, String type) {
        boolean parameter = constructor.getParameters().stream().anyMatch(value ->
                value.getNameAsString().equals(name) && value.getType().asString().equals(type));
        return parameter && constructor.findAll(AssignExpr.class).stream().anyMatch(assignment ->
                assignment.getTarget().toString().equals("this." + name)
                        && assignment.getValue() instanceof NameExpr value && value.getNameAsString().equals(name));
    }

    private static Optional<TypeInfo> resolveType(CompilationUnit unit, String declared, Index index) {
        List<TypeInfo> candidates = new ArrayList<>();
        if (index.types().containsKey(declared)) candidates.add(index.types().get(declared));
        unit.getImports().stream().filter(value -> !value.isStatic() && !value.isAsterisk())
                .map(value -> value.getNameAsString()).filter(value -> value.endsWith('.' + declared))
                .map(index.types()::get).filter(Objects::nonNull).forEach(candidates::add);
        String samePackage = unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + declared).orElse(declared);
        if (index.types().containsKey(samePackage)) candidates.add(index.types().get(samePackage));
        List<TypeInfo> unique = candidates.stream().distinct().toList();
        return unique.size() == 1 ? Optional.of(unique.getFirst()) : Optional.empty();
    }

    private Index index(Path root, Path sourceRoot) throws IOException {
        Map<String, TypeInfo> types = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> relative(root, path))).toList()) {
                var parsed = parser.parse(file);
                if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
                    throw new IOException("Cannot parse Java source " + relative(root, file));
                }
                CompilationUnit unit = parsed.getResult().orElseThrow();
                for (ClassOrInterfaceDeclaration declaration : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                    declaration.getFullyQualifiedName().ifPresent(name -> types.put(name,
                            new TypeInfo(name, unit, declaration, relative(root, file))));
                }
            }
        }
        return new Index(Map.copyOf(types));
    }

    private static boolean sameLocation(com.github.javaparser.ast.Node node, SourceLocation location) {
        return node.getBegin().map(position -> position.line == location.line()
                && position.column == location.column()).orElse(false);
    }

    private static boolean hasAnnotation(CompilationUnit unit, List<AnnotationExpr> annotations, String qualified) {
        String simple = qualified.substring(qualified.lastIndexOf('.') + 1);
        String pkg = qualified.substring(0, qualified.lastIndexOf('.'));
        return annotations.stream().anyMatch(annotation -> annotation.getNameAsString().equals(qualified)
                || annotation.getNameAsString().equals(simple) && unit.getImports().stream().anyMatch(value ->
                !value.isStatic() && (value.getNameAsString().equals(qualified)
                        || value.isAsterisk() && value.getNameAsString().equals(pkg))));
    }

    private static SourceLocation location(String path, com.github.javaparser.ast.Node node) {
        var position = node.getBegin().orElseThrow();
        return new SourceLocation(path, position.line, position.column);
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private record TypeInfo(String qualifiedName, CompilationUnit unit,
                            ClassOrInterfaceDeclaration declaration, String path) { }
    private record ResolvedCall(MethodCallExpr call, TypeInfo target, DependencyInjectionKind injectionKind) { }
    private record Index(Map<String, TypeInfo> types) { }
}
