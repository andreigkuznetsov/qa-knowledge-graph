package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

public sealed interface ScenarioManifestJsonParseResult
        permits ScenarioManifestJsonParseResult.Completed, ScenarioManifestJsonParseResult.Failed {

    record Completed(List<ParsedMember> members) implements ScenarioManifestJsonParseResult {
        public Completed {
            members = List.copyOf(Objects.requireNonNull(members, "members"));
        }
    }

    record Failed(Failure failure) implements ScenarioManifestJsonParseResult {
        public Failed {
            Objects.requireNonNull(failure, "failure");
        }
    }

    record ParsedMember(ScenarioManifestCaptureResult.CapturedMember source, JsonNode document) {
        public ParsedMember {
            Objects.requireNonNull(source, "source");
            document = Objects.requireNonNull(document, "document").deepCopy();
        }

        @Override
        public JsonNode document() {
            return document.deepCopy();
        }
    }

    record Failure(Code code, String repositoryRelativePath, String message) {
        public Failure {
            Objects.requireNonNull(code, "code");
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            message = requireNonBlank(message, "message");
        }
    }

    enum Code {
        INVALID_UTF8,
        MALFORMED_JSON,
        DUPLICATE_JSON_MEMBER,
        TRAILING_JSON_CONTENT
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
