package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

public record ScenarioIdentityValidationResult(
        List<IdentityGroup> identities,
        List<DuplicateDiagnostic> diagnostics,
        List<String> excludedMemberPaths
) {
    public ScenarioIdentityValidationResult {
        identities = List.copyOf(Objects.requireNonNull(identities, "identities"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        excludedMemberPaths = List.copyOf(Objects.requireNonNull(excludedMemberPaths, "excludedMemberPaths"));
    }

    public record IdentityGroup(
            String authority,
            String scenarioKey,
            List<Declaration> declarations
    ) {
        public IdentityGroup {
            authority = requireNonBlank(authority, "authority");
            scenarioKey = requireNonBlank(scenarioKey, "scenarioKey");
            declarations = List.copyOf(Objects.requireNonNull(declarations, "declarations"));
            if (declarations.isEmpty()) {
                throw new IllegalArgumentException("identity group must contain declarations");
            }
        }

        public boolean unique() {
            return declarations.size() == 1;
        }
    }

    public record Declaration(DeclarationSource source, JsonNode document) {
        public Declaration {
            Objects.requireNonNull(source, "source");
            document = Objects.requireNonNull(document, "document").deepCopy();
        }

        @Override
        public JsonNode document() {
            return document.deepCopy();
        }
    }

    public record DeclarationSource(String repositoryRelativePath, int declarationIndex) {
        public DeclarationSource {
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            if (declarationIndex < 0) throw new IllegalArgumentException("declarationIndex must not be negative");
        }

        public String instanceLocation() {
            return "/scenarios/" + declarationIndex;
        }
    }

    public record DuplicateDiagnostic(
            Code code,
            String authority,
            String scenarioKey,
            List<DeclarationSource> declarations,
            String message
    ) {
        public DuplicateDiagnostic {
            Objects.requireNonNull(code, "code");
            authority = requireNonBlank(authority, "authority");
            scenarioKey = requireNonBlank(scenarioKey, "scenarioKey");
            declarations = List.copyOf(Objects.requireNonNull(declarations, "declarations"));
            if (declarations.size() < 2) {
                throw new IllegalArgumentException("duplicate diagnostic must contain every duplicate declaration");
            }
            message = requireNonBlank(message, "message");
        }
    }

    public enum Code {
        DUPLICATE_SCENARIO_IDENTITY
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
