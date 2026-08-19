package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

public record ScenarioManifestSchemaValidationResult(List<ValidatedMember> members) {
    public ScenarioManifestSchemaValidationResult {
        members = List.copyOf(Objects.requireNonNull(members, "members"));
    }

    public boolean structurallyAdmitted() {
        return members.stream().allMatch(ValidatedMember::structurallyAdmitted);
    }

    public List<Diagnostic> diagnostics() {
        return members.stream().flatMap(member -> member.diagnostics().stream()).toList();
    }

    public record ValidatedMember(
            ScenarioManifestJsonParseResult.ParsedMember source,
            List<Diagnostic> diagnostics
    ) {
        public ValidatedMember {
            Objects.requireNonNull(source, "source");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        public boolean structurallyAdmitted() {
            return diagnostics.isEmpty();
        }
    }

    /**
     * Legacy noncanonical presentation/compatibility data. This record may contain
     * validator-owned wording and must never be used for ADR-014 admission,
     * canonical evidence, implicit canonical conversion, or fingerprinting.
     */
    @Deprecated(forRemoval = false)
    public record Diagnostic(
            Code code,
            String repositoryRelativePath,
            String instanceLocation,
            String keyword,
            String message
    ) {
        public Diagnostic {
            Objects.requireNonNull(code, "code");
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            instanceLocation = Objects.requireNonNull(instanceLocation, "instanceLocation");
            keyword = requireNonBlank(keyword, "keyword");
            message = requireNonBlank(message, "message");
        }
    }

    public enum Code {
        SCHEMA_VIOLATION
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
