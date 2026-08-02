package ru.kuznetsov.qagraph.extractor.rest.binding;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class ControllerRequestModelBindingExtractor {
    private static final String SPRING_WEB = "org.springframework.web.bind.annotation";
    private static final Set<String> EXCLUDED_BINDING_ANNOTATIONS = Set.of(
            "PathVariable", "RequestParam", "RequestHeader", "CookieValue", "RequestPart");
    private static final Set<String> SECURITY_ANNOTATIONS = Set.of(
            "org.springframework.security.core.annotation.AuthenticationPrincipal",
            "org.springframework.security.core.annotation.CurrentSecurityContext");
    private static final Set<String> WRAPPERS = Set.of(
            "java.util.Collection", "java.util.List", "java.util.Set", "java.lang.Iterable",
            "java.util.Optional", "org.springframework.http.HttpEntity", "org.springframework.http.RequestEntity");
    private static final Set<String> INFRASTRUCTURE_TYPES = Set.of(
            "ServletRequest", "ServletResponse", "HttpServletRequest", "HttpServletResponse",
            "HttpSession", "WebRequest", "NativeWebRequest", "Principal", "Authentication",
            "SecurityContext", "BindingResult", "Errors", "Model", "ModelMap", "RedirectAttributes",
            "UriComponentsBuilder", "Locale", "TimeZone", "ZoneId", "InputStream", "OutputStream",
            "Reader", "Writer");
    private static final Comparator<RequestModelBindingEvidence> STABLE_ORDER = Comparator
            .comparing(RequestModelBindingEvidence::controllerClass)
            .thenComparing(RequestModelBindingEvidence::controllerMethod)
            .thenComparingInt(RequestModelBindingEvidence::line)
            .thenComparingInt(RequestModelBindingEvidence::column)
            .thenComparing(RequestModelBindingEvidence::parameterName)
            .thenComparing(RequestModelBindingEvidence::resolvedModelType);

    private final JavaParser parser;

    public ControllerRequestModelBindingExtractor() {
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    public List<RequestModelBindingEvidence> extract(
            Path repositoryRoot, RestOperationEvidence operation) throws IOException {
        Objects.requireNonNull(repositoryRoot, "repositoryRoot");
        Objects.requireNonNull(operation, "operation");
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path sourceRoot = normalizedRoot.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot) || operation.sourceLocation() == null) return List.of();

        List<Path> sourceFiles;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            sourceFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path ->
                            normalizedRoot.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }
        SourceIndex index = sourceIndex(normalizedRoot, sourceFiles);
        List<MethodSource> methods = index.methods().stream()
                .filter(method -> method.relativePath().equals(
                        operation.sourceLocation().repositoryRelativePath()))
                .filter(method -> method.ownerSimpleName().equals(operation.controllerClass()))
                .filter(method -> method.ownerPackage().equals(operation.javaPackage()))
                .filter(method -> method.method().getNameAsString().equals(operation.controllerMethod()))
                .filter(method -> hasMappingAtLine(method.method(), operation.sourceLocation().line()))
                .toList();
        if (methods.size() != 1) return List.of();

        MethodSource source = methods.getFirst();
        List<RequestModelBindingEvidence> result = new ArrayList<>();
        for (Parameter parameter : source.method().getParameters()) {
            binding(source, parameter, index.types()).ifPresent(result::add);
        }
        result.sort(STABLE_ORDER);
        return result.stream().distinct().toList();
    }

    private Optional<RequestModelBindingEvidence> binding(
            MethodSource source, Parameter parameter, Map<String, SourceType> types) {
        Optional<RequestBindingKind> kind = bindingKind(source.unit(), parameter);
        if (kind.isEmpty() || excluded(source.unit(), parameter)) return Optional.empty();
        Optional<String> modelType = resolveModelType(
                source.unit(), parameter.getType(), types, kind.get() != RequestBindingKind.IMPLICIT);
        if (modelType.isEmpty()) return Optional.empty();
        var position = parameter.getBegin().orElseThrow(() ->
                new IllegalStateException("Parsed controller parameter has no source location"));
        String controller = source.ownerPackage().isBlank()
                ? source.ownerSimpleName()
                : source.ownerPackage() + '.' + source.ownerSimpleName();
        return Optional.of(new RequestModelBindingEvidence(
                controller,
                source.method().getNameAsString(),
                parameter.getNameAsString(),
                parameter.getType().toString(),
                modelType.get(),
                kind.get(),
                validationActivations(source.unit(), parameter),
                source.relativePath(),
                position.line,
                position.column));
    }

    private static Optional<RequestBindingKind> bindingKind(CompilationUnit unit, Parameter parameter) {
        if (hasAnnotation(unit, parameter, SPRING_WEB, "RequestBody")) {
            return Optional.of(RequestBindingKind.REQUEST_BODY);
        }
        if (hasAnnotation(unit, parameter, SPRING_WEB, "ModelAttribute")) {
            return Optional.of(RequestBindingKind.MODEL_ATTRIBUTE);
        }
        return Optional.of(RequestBindingKind.IMPLICIT);
    }

    private static boolean excluded(CompilationUnit unit, Parameter parameter) {
        if (simpleName(parameter.getType().asString()).matches(
                "(?i).*(ServletRequest|ServletResponse|HttpSession|WebRequest|Principal|Authentication|SecurityContext|BindingResult|Errors|Model|ModelMap|RedirectAttributes|UriComponentsBuilder|Locale|TimeZone|ZoneId|InputStream|OutputStream|Reader|Writer)$")) {
            return true;
        }
        if (INFRASTRUCTURE_TYPES.contains(simpleName(parameter.getType().asString()))) return true;
        for (String name : EXCLUDED_BINDING_ANNOTATIONS) {
            if (hasAnnotation(unit, parameter, SPRING_WEB, name)) return true;
        }
        return parameter.getAnnotations().stream().anyMatch(annotation ->
                SECURITY_ANNOTATIONS.stream().anyMatch(type -> annotationMatches(unit, annotation, type)));
    }

    private static Set<ValidationActivation> validationActivations(
            CompilationUnit unit, Parameter parameter) {
        EnumSet<ValidationActivation> result = EnumSet.noneOf(ValidationActivation.class);
        if (hasAnnotation(unit, parameter, "jakarta.validation", "Valid")
                || hasAnnotation(unit, parameter, "javax.validation", "Valid")) {
            result.add(ValidationActivation.VALID);
        }
        if (hasAnnotation(unit, parameter, "org.springframework.validation.annotation", "Validated")) {
            result.add(ValidationActivation.VALIDATED);
        }
        return result;
    }

    private static Optional<String> resolveModelType(
            CompilationUnit unit, Type declaredType, Map<String, SourceType> types, boolean containersAllowed) {
        if (declaredType instanceof ArrayType array) {
            return containersAllowed
                    ? resolveDirectType(unit, array.getComponentType(), types)
                    : Optional.empty();
        }
        if (!(declaredType instanceof ClassOrInterfaceType classType)) return Optional.empty();
        if (classType.getTypeArguments().isPresent()) {
            if (!containersAllowed || classType.getTypeArguments().orElseThrow().size() != 1) {
                return Optional.empty();
            }
            if (resolveKnownType(unit, classType.getNameWithScope(), WRAPPERS).isEmpty()) return Optional.empty();
            Type argument = classType.getTypeArguments().orElseThrow().get(0);
            if (argument.isWildcardType() || argument.isTypeParameter()) return Optional.empty();
            return resolveDirectType(unit, argument, types);
        }
        return resolveDirectType(unit, classType, types);
    }

    private static Optional<String> resolveDirectType(
            CompilationUnit unit, Type type, Map<String, SourceType> types) {
        if (!(type instanceof ClassOrInterfaceType classType)) return Optional.empty();
        return resolveKnownType(unit, classType.getNameWithScope(), types.keySet());
    }

    private static Optional<String> resolveKnownType(
            CompilationUnit unit, String declaredName, Set<String> knownTypes) {
        List<String> candidates = new ArrayList<>();
        if (declaredName.contains(".") && knownTypes.contains(declaredName)) candidates.add(declaredName);
        unit.getImports().stream()
                .filter(declaration -> !declaration.isStatic() && !declaration.isAsterisk())
                .map(ImportDeclaration::getNameAsString)
                .filter(name -> simpleName(name).equals(simpleName(declaredName)))
                .filter(knownTypes::contains)
                .forEach(candidates::add);
        unit.getImports().stream()
                .filter(declaration -> !declaration.isStatic() && declaration.isAsterisk())
                .map(declaration -> declaration.getNameAsString() + '.' + simpleName(declaredName))
                .filter(knownTypes::contains)
                .forEach(candidates::add);
        String samePackage = unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + simpleName(declaredName))
                .orElse(simpleName(declaredName));
        if (knownTypes.contains(samePackage)) candidates.add(samePackage);
        String javaLang = "java.lang." + simpleName(declaredName);
        if (knownTypes.contains(javaLang)) candidates.add(javaLang);
        return candidates.stream().distinct().count() == 1
                ? Optional.of(candidates.getFirst())
                : Optional.empty();
    }

    private SourceIndex sourceIndex(Path root, List<Path> files) throws IOException {
        Map<String, SourceType> types = new LinkedHashMap<>();
        List<MethodSource> methods = new ArrayList<>();
        for (Path file : files) {
            ParseResult<CompilationUnit> result = parser.parse(file);
            CompilationUnit unit = result.getResult().orElseThrow(() ->
                    new IOException("Cannot parse Java source " + root.relativize(file)));
            if (!result.isSuccessful()) {
                throw new IOException("Cannot parse Java source " + root.relativize(file)
                        + ": " + result.getProblems());
            }
            String relative = root.relativize(file).toString().replace('\\', '/');
            for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                type.getFullyQualifiedName().ifPresent(name ->
                        types.put(name, new SourceType(name, relative)));
            }
            for (RecordDeclaration type : unit.findAll(RecordDeclaration.class)) {
                type.getFullyQualifiedName().ifPresent(name ->
                        types.put(name, new SourceType(name, relative)));
            }
            for (TypeDeclaration<?> owner : unit.getTypes()) {
                for (MethodDeclaration method : owner.getMethods()) {
                    methods.add(new MethodSource(unit, owner.getNameAsString(),
                            unit.getPackageDeclaration().map(value -> value.getNameAsString()).orElse(""),
                            method, relative));
                }
            }
        }
        return new SourceIndex(Map.copyOf(types), List.copyOf(methods));
    }

    private static boolean hasMappingAtLine(MethodDeclaration method, int line) {
        return method.getAnnotations().stream().anyMatch(annotation ->
                Set.of("GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping")
                        .contains(simpleName(annotation.getNameAsString()))
                        && annotation.getBegin().map(position -> position.line == line).orElse(false));
    }

    private static boolean hasAnnotation(
            CompilationUnit unit, Parameter parameter, String annotationPackage, String annotationName) {
        String qualified = annotationPackage + '.' + annotationName;
        return parameter.getAnnotations().stream().anyMatch(annotation ->
                annotationMatches(unit, annotation, qualified));
    }

    private static boolean annotationMatches(
            CompilationUnit unit, AnnotationExpr annotation, String qualifiedName) {
        String declared = annotation.getNameAsString();
        if (declared.contains(".")) return declared.equals(qualifiedName);
        if (!declared.equals(simpleName(qualifiedName))) return false;
        return unit.getImports().stream().anyMatch(declaration -> !declaration.isStatic()
                && (declaration.isAsterisk()
                && declaration.getNameAsString().equals(packageName(qualifiedName))
                || !declaration.isAsterisk() && declaration.getNameAsString().equals(qualifiedName)));
    }

    private static String packageName(String name) {
        return name.substring(0, name.lastIndexOf('.'));
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private record SourceType(String qualifiedName, String repositoryRelativePath) {
    }

    private record MethodSource(
            CompilationUnit unit,
            String ownerSimpleName,
            String ownerPackage,
            MethodDeclaration method,
            String relativePath) {
    }

    private record SourceIndex(Map<String, SourceType> types, List<MethodSource> methods) {
    }
}
