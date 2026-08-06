package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.List;
import java.util.Objects;

public record ApplicationServiceRepositoryEvidence(
        ConsumerApplicationServiceEvidence applicationService,
        String repositoryClass,
        SourceLocation repositoryDeclarationLocation,
        DependencyInjectionKind injectionKind,
        List<RepositoryInvocationEvidence> invocations
) {
    public ApplicationServiceRepositoryEvidence {
        Objects.requireNonNull(applicationService, "applicationService");
        Objects.requireNonNull(repositoryClass, "repositoryClass");
        if (repositoryClass.isBlank()) throw new IllegalArgumentException("repositoryClass must not be blank");
        Objects.requireNonNull(repositoryDeclarationLocation, "repositoryDeclarationLocation");
        Objects.requireNonNull(injectionKind, "injectionKind");
        invocations = List.copyOf(Objects.requireNonNull(invocations, "invocations"));
        if (invocations.isEmpty()) throw new IllegalArgumentException("invocations must not be empty");
    }

    public record RepositoryInvocationEvidence(String repositoryMethod, SourceLocation invocationLocation) {
        public RepositoryInvocationEvidence {
            Objects.requireNonNull(repositoryMethod, "repositoryMethod");
            if (repositoryMethod.isBlank()) throw new IllegalArgumentException("repositoryMethod must not be blank");
            Objects.requireNonNull(invocationLocation, "invocationLocation");
        }
    }
}
