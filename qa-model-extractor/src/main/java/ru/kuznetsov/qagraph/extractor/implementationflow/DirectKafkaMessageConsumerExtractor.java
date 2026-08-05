package ru.kuznetsov.qagraph.extractor.implementationflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
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

public final class DirectKafkaMessageConsumerExtractor {
    private static final String KAFKA_LISTENER = "org.springframework.kafka.annotation.KafkaListener";
    private final JavaParser parser = new JavaParser(new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));

    public List<MessageConsumerEvidence> extract(Path repositoryRoot) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) return List.of();
        Index index = index(root, sourceRoot);
        List<MessageConsumerEvidence> result = new ArrayList<>();
        for (TypeInfo owner : index.types().values().stream()
                .sorted(Comparator.comparing(TypeInfo::qualifiedName)).toList()) {
            for (MethodDeclaration method : owner.declaration().getMethods().stream()
                    .sorted(Comparator.comparing(MethodDeclaration::getNameAsString)).toList()) {
                List<AnnotationExpr> listeners = method.getAnnotations().stream()
                        .filter(annotation -> isKafkaListener(owner.unit(), annotation)).toList();
                if (listeners.size() != 1) continue;
                AnnotationExpr listener = listeners.getFirst();
                Optional<Expression> topicExpression = annotationValue(listener, "topics", "value");
                if (topicExpression.isEmpty()) continue;
                Optional<ResolvedValue> topic = resolve(root, owner, single(topicExpression.get()), index);
                if (topic.isEmpty()) continue;
                String group = null;
                Optional<Expression> groupExpression = annotationValue(listener, "groupId");
                if (groupExpression.isPresent()) {
                    group = resolve(root, owner, single(groupExpression.get()), index)
                            .map(ResolvedValue::value).orElse(null);
                }
                var position = listener.getBegin();
                if (position.isEmpty()) continue;
                result.add(new MessageConsumerEvidence(
                        "Kafka", owner.qualifiedName(), method.getNameAsString(), topic.get().value(), "TOPIC", group,
                        new SourceLocation(owner.path(), position.get().line, position.get().column),
                        topic.get().location()));
            }
        }
        return result.stream().distinct().sorted(Comparator
                .comparing(MessageConsumerEvidence::listenerClass)
                .thenComparing(MessageConsumerEvidence::listenerMethod)
                .thenComparing(MessageConsumerEvidence::destinationName)
                .thenComparing(value -> value.consumerGroup() == null ? "" : value.consumerGroup()))
                .toList();
    }

    private static Optional<Expression> annotationValue(AnnotationExpr annotation, String... names) {
        if (annotation.isSingleMemberAnnotationExpr()) {
            return Stream.of(names).anyMatch(name -> name.equals("value"))
                    ? Optional.of(annotation.asSingleMemberAnnotationExpr().getMemberValue()) : Optional.empty();
        }
        if (!annotation.isNormalAnnotationExpr()) return Optional.empty();
        NormalAnnotationExpr normal = annotation.asNormalAnnotationExpr();
        return normal.getPairs().stream()
                .filter(pair -> Stream.of(names).anyMatch(name -> name.equals(pair.getNameAsString())))
                .map(pair -> pair.getValue()).findFirst();
    }

    private static Expression single(Expression expression) {
        if (!expression.isArrayInitializerExpr()) return expression;
        ArrayInitializerExpr array = expression.asArrayInitializerExpr();
        return array.getValues().size() == 1 ? array.getValues().get(0) : expression;
    }

    private Optional<ResolvedValue> resolve(
            Path root, TypeInfo owner, Expression expression, Index index) throws IOException {
        if (expression.isArrayInitializerExpr()) return Optional.empty();
        if (expression.isStringLiteralExpr()) {
            String text = expression.asStringLiteralExpr().asString();
            Optional<String> property = propertyKey(text);
            if (property.isPresent()) return resolveConfiguration(root, property.get());
            if (!isStaticValue(text)) return Optional.empty();
            return expression.getBegin().map(position -> new ResolvedValue(text,
                    new SourceLocation(owner.path(), position.line, position.column)));
        }
        if (expression.isNameExpr()) {
            return resolveConstant(owner, expression.asNameExpr().getNameAsString());
        }
        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr access = expression.asFieldAccessExpr();
            return resolveType(owner.unit(), access.getScope().toString(), index)
                    .flatMap(type -> resolveConstant(type, access.getNameAsString()));
        }
        return Optional.empty();
    }

    private static Optional<ResolvedValue> resolveConstant(TypeInfo type, String name) {
        return type.declaration().getFields().stream()
                .filter(FieldDeclaration::isStatic).filter(FieldDeclaration::isFinal)
                .flatMap(field -> field.getVariables().stream())
                .filter(variable -> variable.getNameAsString().equals(name))
                .filter(variable -> variable.getInitializer().isPresent()
                        && variable.getInitializer().orElseThrow().isStringLiteralExpr())
                .findFirst().flatMap(variable -> variable.getName().getBegin().map(position -> new ResolvedValue(
                        variable.getInitializer().orElseThrow().asStringLiteralExpr().asString(),
                        new SourceLocation(type.path(), position.line, position.column))));
    }

    private Optional<ResolvedValue> resolveConfiguration(Path root, String key) throws IOException {
        Path resources = root.resolve("src/main/resources");
        List<ResolvedValue> matches = new ArrayList<>();
        for (String name : List.of("application.yml", "application.yaml")) {
            Path file = resources.resolve(name);
            if (Files.isRegularFile(file)) flattenYaml(root, file).getOrDefault(key, List.of()).forEach(matches::add);
        }
        Path properties = resources.resolve("application.properties");
        if (Files.isRegularFile(properties)) {
            List<String> lines = Files.readAllLines(properties);
            for (int line = 0; line < lines.size(); line++) {
                String text = lines.get(line).trim();
                if (text.isEmpty() || text.startsWith("#") || !text.contains("=")) continue;
                int separator = text.indexOf('=');
                if (text.substring(0, separator).trim().equals(key)) {
                    matches.add(new ResolvedValue(text.substring(separator + 1).trim(),
                            new SourceLocation(relative(root, properties), line + 1, separator + 2)));
                }
            }
        }
        return matches.size() == 1 && isStaticValue(matches.getFirst().value())
                ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private static Map<String, List<ResolvedValue>> flattenYaml(Path root, Path file) throws IOException {
        Map<String, List<ResolvedValue>> result = new LinkedHashMap<>();
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
                result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ResolvedValue(
                        normalized, new SourceLocation(relative(root, file), line + 1, raw.indexOf(value) + 1)));
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
                        new TypeInfo(name, unit, declaration, relative(root, file))));
            }
        }
        return new Index(Map.copyOf(types));
    }

    private static Optional<TypeInfo> resolveType(CompilationUnit unit, String declared, Index index) {
        if (index.types().containsKey(declared)) return Optional.of(index.types().get(declared));
        List<TypeInfo> candidates = unit.getImports().stream()
                .filter(value -> !value.isStatic() && !value.isAsterisk())
                .map(value -> value.getNameAsString())
                .filter(value -> value.endsWith('.' + declared))
                .map(index.types()::get).filter(Objects::nonNull).toList();
        String samePackage = unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + declared).orElse(declared);
        if (index.types().containsKey(samePackage)) {
            candidates = new ArrayList<>(candidates);
            candidates.add(index.types().get(samePackage));
        }
        return candidates.stream().distinct().count() == 1
                ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    private static boolean isKafkaListener(CompilationUnit unit, AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        if (name.equals(KAFKA_LISTENER)) return true;
        return name.equals("KafkaListener") && unit.getImports().stream().anyMatch(value ->
                !value.isStatic() && (value.getNameAsString().equals(KAFKA_LISTENER)
                        || value.isAsterisk()
                        && value.getNameAsString().equals("org.springframework.kafka.annotation")));
    }

    private static Optional<String> propertyKey(String value) {
        return value.startsWith("${") && value.endsWith("}") && !value.contains(":")
                ? Optional.of(value.substring(2, value.length() - 1)) : Optional.empty();
    }

    private static boolean isStaticValue(String value) {
        return !value.isBlank() && !value.contains("${") && !value.contains("#{");
    }

    private static String unquote(String value) {
        return value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private record TypeInfo(String qualifiedName, CompilationUnit unit,
                            ClassOrInterfaceDeclaration declaration, String path) { }
    private record Index(Map<String, TypeInfo> types) { }
    private record ResolvedValue(String value, SourceLocation location) { }
    private record YamlLevel(int indent, String key) { }
}
