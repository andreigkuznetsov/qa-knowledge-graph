package ru.kuznetsov.qagraph.extractor.integrationtest;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class StaticTestEndpointResolver {
    private final Map<String, Map<String, String>> stringConstants;
    private final Map<String, EnumInfo> enums;

    private StaticTestEndpointResolver(
            Map<String, Map<String, String>> stringConstants,
            Map<String, EnumInfo> enums) {
        this.stringConstants = Map.copyOf(stringConstants);
        this.enums = Map.copyOf(enums);
    }

    static StaticTestEndpointResolver from(List<Path> sourceFiles, JavaParser parser) throws IOException {
        Map<String, Map<String, String>> constants = new LinkedHashMap<>();
        Map<String, EnumInfo> enums = new LinkedHashMap<>();
        for (Path sourceFile : sourceFiles) {
            var result = parser.parse(sourceFile);
            CompilationUnit unit = result.getResult().orElseThrow(() ->
                    new IOException("Cannot parse Java test source " + sourceFile));
            if (!result.isSuccessful()) {
                throw new IOException("Cannot parse Java test source " + sourceFile + ": " + result.getProblems());
            }
            for (TypeDeclaration<?> type : unit.getTypes()) {
                String qualifiedName = qualifiedName(unit, type.getNameAsString());
                Map<String, String> typeConstants = staticFinalStrings(type);
                if (!typeConstants.isEmpty()) constants.put(qualifiedName, typeConstants);
            }
            for (EnumDeclaration declaration : unit.findAll(EnumDeclaration.class)) {
                enumInfo(unit, declaration).ifPresent(info ->
                        enums.put(qualifiedName(unit, declaration.getNameAsString()), info));
            }
        }
        return new StaticTestEndpointResolver(constants, enums);
    }

    Optional<String> resolve(CompilationUnit unit, Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return Optional.of(expression.asStringLiteralExpr().asString());
        }
        if (expression.isNameExpr()) return resolveName(unit, expression.asNameExpr());
        if (expression.isFieldAccessExpr()) return resolveFieldAccess(unit, expression.asFieldAccessExpr());
        if (expression.isMethodCallExpr()) return resolveEnumAccessor(unit, expression.asMethodCallExpr());
        return Optional.empty();
    }

    private Optional<String> resolveName(CompilationUnit unit, NameExpr expression) {
        String member = expression.getNameAsString();
        List<String> candidates = new ArrayList<>();
        unit.getTypes().stream()
                .map(type -> qualifiedName(unit, type.getNameAsString()))
                .map(stringConstants::get)
                .filter(java.util.Objects::nonNull)
                .map(values -> values.get(member))
                .filter(java.util.Objects::nonNull)
                .forEach(candidates::add);
        for (ImportDeclaration declaration : unit.getImports()) {
            if (!declaration.isStatic()) continue;
            if (declaration.isAsterisk()) {
                addConstant(candidates, declaration.getNameAsString(), member);
            } else if (simpleName(declaration.getNameAsString()).equals(member)) {
                String owner = declaration.getNameAsString().substring(
                        0, declaration.getNameAsString().length() - member.length() - 1);
                addConstant(candidates, owner, member);
            }
        }
        return unique(candidates);
    }

    private Optional<String> resolveFieldAccess(CompilationUnit unit, FieldAccessExpr expression) {
        if (!expression.getScope().isNameExpr()) return Optional.empty();
        String typeName = expression.getScope().asNameExpr().getNameAsString();
        List<String> candidates = new ArrayList<>();
        for (String owner : resolveTypes(unit, typeName)) {
            addConstant(candidates, owner, expression.getNameAsString());
        }
        return unique(candidates);
    }

    private Optional<String> resolveEnumAccessor(CompilationUnit unit, MethodCallExpr expression) {
        if (!expression.getArguments().isEmpty() || expression.getScope().isEmpty()) return Optional.empty();
        String accessor = expression.getNameAsString();
        Expression scope = expression.getScope().orElseThrow();
        List<String> candidates = new ArrayList<>();
        if (scope.isNameExpr()) {
            String constant = scope.asNameExpr().getNameAsString();
            for (ImportDeclaration declaration : unit.getImports()) {
                if (!declaration.isStatic()) continue;
                if (declaration.isAsterisk()) {
                    addEnum(candidates, declaration.getNameAsString(), constant, accessor);
                } else if (simpleName(declaration.getNameAsString()).equals(constant)) {
                    String owner = declaration.getNameAsString().substring(
                            0, declaration.getNameAsString().length() - constant.length() - 1);
                    addEnum(candidates, owner, constant, accessor);
                }
            }
        } else if (scope.isFieldAccessExpr()) {
            FieldAccessExpr constantAccess = scope.asFieldAccessExpr();
            if (constantAccess.getScope().isNameExpr()) {
                for (String owner : resolveTypes(unit,
                        constantAccess.getScope().asNameExpr().getNameAsString())) {
                    addEnum(candidates, owner, constantAccess.getNameAsString(), accessor);
                }
            }
        }
        return unique(candidates);
    }

    private void addConstant(List<String> candidates, String owner, String member) {
        Map<String, String> values = stringConstants.get(owner);
        if (values != null && values.containsKey(member)) candidates.add(values.get(member));
    }

    private void addEnum(List<String> candidates, String owner, String constant, String accessor) {
        EnumInfo info = enums.get(owner);
        if (info == null || !info.accessors().contains(accessor)) return;
        String value = info.values().get(constant);
        if (value != null) candidates.add(value);
    }

    private List<String> resolveTypes(CompilationUnit unit, String typeName) {
        List<String> candidates = new ArrayList<>();
        if (typeName.contains(".")) candidates.add(typeName);
        unit.getImports().stream()
                .filter(declaration -> !declaration.isStatic() && !declaration.isAsterisk())
                .map(ImportDeclaration::getNameAsString)
                .filter(name -> simpleName(name).equals(typeName))
                .forEach(candidates::add);
        String samePackage = qualifiedName(unit, typeName);
        if (stringConstants.containsKey(samePackage) || enums.containsKey(samePackage)) candidates.add(samePackage);
        return candidates;
    }

    private static Map<String, String> staticFinalStrings(TypeDeclaration<?> type) {
        Map<String, String> result = new LinkedHashMap<>();
        for (FieldDeclaration field : type.getFields()) {
            if (!field.isStatic() || !field.isFinal() || !field.getElementType().asString().equals("String")) continue;
            field.getVariables().forEach(variable -> variable.getInitializer()
                    .filter(Expression::isStringLiteralExpr)
                    .ifPresent(value -> result.put(
                            variable.getNameAsString(), value.asStringLiteralExpr().asString())));
        }
        return Map.copyOf(result);
    }

    private static Optional<EnumInfo> enumInfo(CompilationUnit unit, EnumDeclaration declaration) {
        List<FieldDeclaration> stringFields = declaration.getFields().stream()
                .filter(FieldDeclaration::isFinal)
                .filter(field -> field.getElementType().asString().equals("String"))
                .toList();
        if (stringFields.size() != 1) return Optional.empty();
        String fieldName = stringFields.getFirst().getVariable(0).getNameAsString();
        Map<String, String> values = new LinkedHashMap<>();
        for (EnumConstantDeclaration entry : declaration.getEntries()) {
            if (entry.getArguments().size() != 1 || !entry.getArgument(0).isStringLiteralExpr()) continue;
            values.put(entry.getNameAsString(), entry.getArgument(0).asStringLiteralExpr().asString());
        }
        List<String> accessors = new ArrayList<>();
        String beanGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        if (hasLombokGetter(unit, declaration, stringFields.getFirst())) accessors.add(beanGetter);
        declaration.getMethods().stream()
                .filter(method -> method.getParameters().isEmpty())
                .filter(method -> returnsField(method, fieldName))
                .map(MethodDeclaration::getNameAsString)
                .forEach(accessors::add);
        if (values.isEmpty() || accessors.isEmpty()) return Optional.empty();
        return Optional.of(new EnumInfo(Map.copyOf(values), List.copyOf(accessors)));
    }

    private static boolean hasLombokGetter(
            CompilationUnit unit, EnumDeclaration declaration, FieldDeclaration field) {
        boolean imported = unit.getImports().stream().anyMatch(value -> !value.isStatic()
                && (value.getNameAsString().equals("lombok.Getter")
                || value.isAsterisk() && value.getNameAsString().equals("lombok")));
        if (!imported) return false;
        return declaration.getAnnotations().stream().anyMatch(value -> simpleName(value.getNameAsString()).equals("Getter"))
                || field.getAnnotations().stream().anyMatch(value -> simpleName(value.getNameAsString()).equals("Getter"));
    }

    private static boolean returnsField(MethodDeclaration method, String fieldName) {
        if (method.getBody().isEmpty()) return false;
        List<ReturnStmt> returns = method.getBody().orElseThrow().findAll(ReturnStmt.class);
        if (returns.size() != 1 || returns.getFirst().getExpression().isEmpty()) return false;
        Expression expression = returns.getFirst().getExpression().orElseThrow();
        return expression.isNameExpr() && expression.asNameExpr().getNameAsString().equals(fieldName)
                || expression.isFieldAccessExpr()
                && expression.asFieldAccessExpr().getNameAsString().equals(fieldName)
                && expression.asFieldAccessExpr().getScope().isThisExpr();
    }

    private static String qualifiedName(CompilationUnit unit, String typeName) {
        return unit.getPackageDeclaration()
                .map(value -> value.getNameAsString() + '.' + typeName)
                .orElse(typeName);
    }

    private static Optional<String> unique(List<String> candidates) {
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private record EnumInfo(Map<String, String> values, List<String> accessors) {
    }
}
