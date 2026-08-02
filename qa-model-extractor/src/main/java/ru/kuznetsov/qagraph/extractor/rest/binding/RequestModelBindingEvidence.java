package ru.kuznetsov.qagraph.extractor.rest.binding;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record RequestModelBindingEvidence(
        String controllerClass,
        String controllerMethod,
        String parameterName,
        String declaredParameterType,
        String resolvedModelType,
        RequestBindingKind bindingKind,
        Set<ValidationActivation> validationActivations,
        String repositoryRelativePath,
        int line,
        int column
) {
    public RequestModelBindingEvidence {
        requireNonBlank(controllerClass, "controllerClass");
        requireNonBlank(controllerMethod, "controllerMethod");
        requireNonBlank(parameterName, "parameterName");
        requireNonBlank(declaredParameterType, "declaredParameterType");
        requireNonBlank(resolvedModelType, "resolvedModelType");
        Objects.requireNonNull(bindingKind, "bindingKind");
        Objects.requireNonNull(validationActivations, "validationActivations");
        validationActivations = validationActivations.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(validationActivations));
        requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        if (column < 1) throw new IllegalArgumentException("column must be positive");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
