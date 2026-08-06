package ru.kuznetsov.qagraph.extractor.implementationflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class DirectKafkaMessageDestinationExtractor {
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    public Optional<MessageDestinationEvidence> extract(
            Path repositoryRoot, MessageProducerEvidence producer) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(producer, "producer");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) return Optional.empty();
        Index index = index(root, sourceRoot);
        TypeInfo owner = index.types().get(producer.ownerClass());
        if (owner == null) return Optional.empty();
        List<MethodCallExpr> sends = owner.declaration().getMethodsByName(producer.ownerMethod()).stream()
                .flatMap(method -> method.findAll(MethodCallExpr.class).stream())
                .filter(call -> call.getNameAsString().equals(producer.publishingMethod()))
                .filter(call -> call.getBegin().map(position ->
                        position.line == producer.publishingLocation().line()
                                && position.column == producer.publishingLocation().column()).orElse(false))
                .filter(call -> !call.getArguments().isEmpty())
                .toList();
        if (sends.size() != 1) return Optional.empty();
        Optional<ResolvedDestination> resolved = resolve(
                root, owner, sends.getFirst().getArgument(0), index);
        return resolved.map(value -> new MessageDestinationEvidence(
                producer, "Kafka", value.name(), "TOPIC", value.location()));
    }

    private Optional<ResolvedDestination> resolve(
            Path root, TypeInfo owner, Expression expression, Index index) throws IOException {
        if (expression.isStringLiteralExpr()) {
            StringLiteralExpr literal = expression.asStringLiteralExpr();
            return literal.getBegin().map(position -> new ResolvedDestination(
                    literal.asString(), new SourceLocation(owner.path(), position.line, position.column)));
        }
        if (expression.isNameExpr()) {
            return resolveConstant(owner, expression.asNameExpr().getNameAsString());
        }
        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr access = expression.asFieldAccessExpr();
            Optional<TypeInfo> type = resolveType(owner.unit(), access.getScope().toString(), index);
            return type.flatMap(value -> resolveConstant(value, access.getNameAsString()));
        }
        if (!expression.isMethodCallExpr()) return Optional.empty();
        MethodCallExpr accessorCall = expression.asMethodCallExpr();
        if (!accessorCall.getArguments().isEmpty() || accessorCall.getScope().isEmpty()
                || !accessorCall.getScope().orElseThrow().isNameExpr()) return Optional.empty();
        String componentField = accessorCall.getScope().orElseThrow().asNameExpr().getNameAsString();
        List<FieldDeclaration> componentFields = owner.declaration().getFields().stream()
                .filter(field -> field.getVariables().stream().anyMatch(variable ->
                        variable.getNameAsString().equals(componentField)))
                .toList();
        if (componentFields.size() != 1) return Optional.empty();
        Optional<TypeInfo> component = resolveType(
                owner.unit(), componentFields.getFirst().getElementType().asString(), index);
        if (component.isEmpty()) return Optional.empty();
        var methods = component.get().declaration().getMethodsByName(accessorCall.getNameAsString()).stream()
                .filter(method -> method.getParameters().isEmpty())
                .toList();
        if (methods.size() != 1) return Optional.empty();
        List<ReturnStmt> returns = methods.getFirst().findAll(ReturnStmt.class).stream()
                .filter(value -> value.getExpression().isPresent()
                        && value.getExpression().orElseThrow().isNameExpr())
                .toList();
        if (returns.size() != 1) return Optional.empty();
        String valueField = returns.getFirst().getExpression().orElseThrow().asNameExpr().getNameAsString();
        List<FieldDeclaration> valueFields = component.get().declaration().getFields().stream()
                .filter(field -> field.getVariables().stream().anyMatch(variable ->
                        variable.getNameAsString().equals(valueField)))
                .toList();
        if (valueFields.size() != 1) return Optional.empty();
        Optional<String> propertyKey = valueFields.getFirst().getAnnotations().stream()
                .filter(annotation -> annotation.getNameAsString().equals("Value")
                        || annotation.getNameAsString().equals("org.springframework.beans.factory.annotation.Value"))
                .filter(com.github.javaparser.ast.expr.AnnotationExpr::isSingleMemberAnnotationExpr)
                .map(annotation -> annotation.asSingleMemberAnnotationExpr().getMemberValue())
                .filter(Expression::isStringLiteralExpr)
                .map(value -> value.asStringLiteralExpr().asString())
                .filter(value -> value.startsWith("${") && value.endsWith("}") && !value.contains(":"))
                .map(value -> value.substring(2, value.length() - 1))
                .findFirst();
        if (propertyKey.isEmpty()) return Optional.empty();
        return resolveConfiguration(root, propertyKey.get());
    }

    private static Optional<ResolvedDestination> resolveConstant(TypeInfo type, String name) {
        return type.declaration().getFields().stream()
                .filter(FieldDeclaration::isStatic)
                .filter(FieldDeclaration::isFinal)
                .flatMap(field -> field.getVariables().stream())
                .filter(variable -> variable.getNameAsString().equals(name))
                .filter(variable -> variable.getInitializer().isPresent()
                        && variable.getInitializer().orElseThrow().isStringLiteralExpr())
                .findFirst()
                .flatMap(variable -> variable.getName().getBegin().map(position -> new ResolvedDestination(
                        variable.getInitializer().orElseThrow().asStringLiteralExpr().asString(),
                        new SourceLocation(type.path(), position.line, position.column))));
    }

    private Optional<ResolvedDestination> resolveConfiguration(Path root, String key) throws IOException {
        Path resources = root.resolve("src/main/resources");
        List<ResolvedDestination> matches = new ArrayList<>();
        for (String name : List.of("application.yml", "application.yaml")) {
            Path file = resources.resolve(name);
            if (Files.isRegularFile(file)) flattenYaml(root, file).getOrDefault(key, List.of()).forEach(matches::add);
        }
        Path propertiesFile = resources.resolve("application.properties");
        if (Files.isRegularFile(propertiesFile)) {
            List<String> lines = Files.readAllLines(propertiesFile);
            for (int line = 0; line < lines.size(); line++) {
                String text = lines.get(line).trim();
                if (text.startsWith("#") || !text.contains("=")) continue;
                int separator = text.indexOf('=');
                if (text.substring(0, separator).trim().equals(key)) {
                    matches.add(new ResolvedDestination(text.substring(separator + 1).trim(),
                            new SourceLocation(relative(root, propertiesFile), line + 1, separator + 2)));
                }
            }
        }
        return matches.size() == 1 && isStaticValue(matches.getFirst().name())
                ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private static Map<String, List<ResolvedDestination>> flattenYaml(Path root, Path file) throws IOException {
        Map<String, List<ResolvedDestination>> result = new LinkedHashMap<>();
        Deque<YamlLevel> levels = new ArrayDeque<>();
        List<String> lines = Files.readAllLines(file);
        for (int line = 0; line < lines.size(); line++) {
            String raw = lines.get(line);
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains(":")) continue;
            int indent = raw.indexOf(trimmed);
            while (!levels.isEmpty() && levels.peekLast().indent() >= indent) levels.removeLast();
            int separator = trimmed.indexOf(':');
            String part = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            String prefix = levels.stream().map(YamlLevel::key)
                    .reduce((left, right) -> left + '.' + right).orElse("");
            String key = prefix.isEmpty() ? part : prefix + '.' + part;
            if (value.isEmpty()) {
                levels.addLast(new YamlLevel(indent, part));
            } else {
                String normalized = unquote(value);
                result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ResolvedDestination(
                        normalized, new SourceLocation(relative(root, file), line + 1,
                        raw.indexOf(value) + 1)));
            }
        }
        return result;
    }

    private Index index(Path root, Path sourceRoot) throws IOException {
        Map<String, TypeInfo> types = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> relative(root, path))).toList();
        }
        for (Path file : files) {
            var parsed = parser.parse(file);
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
                throw new IOException("Cannot parse Java source " + relative(root, file));
            }
            CompilationUnit unit = parsed.getResult().orElseThrow();
            for (ClassOrInterfaceDeclaration declaration : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                declaration.getFullyQualifiedName().ifPresent(name -> types.put(name,
                        new TypeInfo(unit, declaration, relative(root, file))));
            }
        }
        return new Index(Map.copyOf(types));
    }

    private static Optional<TypeInfo> resolveType(CompilationUnit unit, String declared, Index index) {
        String simple = declared.contains("<") ? declared.substring(0, declared.indexOf('<')) : declared;
        if (index.types().containsKey(simple)) return Optional.of(index.types().get(simple));
        List<TypeInfo> candidates = unit.getImports().stream()
                .filter(value -> !value.isStatic() && !value.isAsterisk())
                .map(value -> value.getNameAsString())
                .filter(value -> value.endsWith('.' + simple))
                .map(index.types()::get).filter(Objects::nonNull).toList();
        String samePackage = unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + simple).orElse(simple);
        if (index.types().containsKey(samePackage)) {
            candidates = new ArrayList<>(candidates);
            candidates.add(index.types().get(samePackage));
        }
        return candidates.stream().distinct().count() == 1
                ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    private static boolean isStaticValue(String value) {
        return !value.isBlank() && !value.contains("${") && !value.contains("#{");
    }

    private static String unquote(String value) {
        return value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private record TypeInfo(CompilationUnit unit, ClassOrInterfaceDeclaration declaration, String path) { }
    private record Index(Map<String, TypeInfo> types) { }
    private record ResolvedDestination(String name, SourceLocation location) { }
    private record YamlLevel(int indent, String key) { }
}
