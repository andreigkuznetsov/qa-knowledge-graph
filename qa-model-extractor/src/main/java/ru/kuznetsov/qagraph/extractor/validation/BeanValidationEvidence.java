package ru.kuznetsov.qagraph.extractor.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record BeanValidationEvidence(
        String owningJavaType,
        String memberName,
        String annotationType,
        Map<String, String> declaredAttributes,
        String explicitMessage,
        String repositoryRelativePath,
        int line,
        int column
) {
    public BeanValidationEvidence {
        requireNonBlank(owningJavaType, "owningJavaType");
        requireNonBlank(memberName, "memberName");
        requireNonBlank(annotationType, "annotationType");
        declaredAttributes = Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(
                Objects.requireNonNull(declaredAttributes, "declaredAttributes"))));
        requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
