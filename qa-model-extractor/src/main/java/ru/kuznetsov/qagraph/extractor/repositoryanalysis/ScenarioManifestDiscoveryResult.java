package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ScenarioManifestDiscoveryResult(
        List<Member> members,
        List<Diagnostic> diagnostics
) {
    public ScenarioManifestDiscoveryResult {
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public record Member(Path path, String repositoryRelativePath) {
        public Member {
            path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
        }
    }

    public record Diagnostic(Code code, String repositoryRelativePath, String message) {
        public Diagnostic {
            Objects.requireNonNull(code, "code");
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            message = requireNonBlank(message, "message");
        }
    }

    public enum Code {
        NON_DIRECTORY_DISCOVERY_ANCHOR(null),
        UNSUPPORTED_SYMBOLIC_LINK("SYMBOLIC_LINK"),
        UNSUPPORTED_DIRECTORY("DIRECTORY"),
        UNSUPPORTED_OTHER_NON_REGULAR_ENTRY("OTHER_NON_REGULAR");

        private final String unsupportedEntryKind;

        Code(String unsupportedEntryKind) {
            this.unsupportedEntryKind = unsupportedEntryKind;
        }

        public boolean isUnsupportedEntry() {
            return unsupportedEntryKind != null;
        }

        public String unsupportedEntryKind() {
            if (unsupportedEntryKind == null) {
                throw new IllegalStateException(name() + " is not an unsupported-entry diagnostic");
            }
            return unsupportedEntryKind;
        }
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
