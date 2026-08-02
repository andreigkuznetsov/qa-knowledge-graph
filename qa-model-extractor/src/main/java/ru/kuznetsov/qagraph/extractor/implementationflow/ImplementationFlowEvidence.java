package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.Objects;

public record ImplementationFlowEvidence(
        RestOperationEvidence operation,
        String controllerClass,
        String controllerMethod,
        String serviceClass,
        String serviceMethod,
        String repositoryClass,
        String repositoryMethod,
        ImplementationInvocationKind invocationKind,
        DependencyInjectionKind serviceInjection,
        DependencyInjectionKind repositoryInjection,
        SourceLocation controllerInvocationLocation,
        SourceLocation serviceMethodLocation,
        SourceLocation repositoryInvocationLocation,
        SourceLocation repositoryDeclarationLocation
) {
    public ImplementationFlowEvidence {
        Objects.requireNonNull(operation, "operation");
        requireNonBlank(controllerClass, "controllerClass");
        requireNonBlank(controllerMethod, "controllerMethod");
        requireNonBlank(serviceClass, "serviceClass");
        requireNonBlank(serviceMethod, "serviceMethod");
        requireNonBlank(repositoryClass, "repositoryClass");
        requireNonBlank(repositoryMethod, "repositoryMethod");
        Objects.requireNonNull(invocationKind, "invocationKind");
        Objects.requireNonNull(serviceInjection, "serviceInjection");
        Objects.requireNonNull(repositoryInjection, "repositoryInjection");
        Objects.requireNonNull(controllerInvocationLocation, "controllerInvocationLocation");
        Objects.requireNonNull(serviceMethodLocation, "serviceMethodLocation");
        Objects.requireNonNull(repositoryInvocationLocation, "repositoryInvocationLocation");
        Objects.requireNonNull(repositoryDeclarationLocation, "repositoryDeclarationLocation");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
