package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public sealed interface ScenarioManifestCaptureResult
        permits ScenarioManifestCaptureResult.Completed, ScenarioManifestCaptureResult.Failed {

    record Completed(List<CapturedMember> members) implements ScenarioManifestCaptureResult {
        public Completed {
            members = List.copyOf(Objects.requireNonNull(members, "members"));
        }
    }

    record Failed(Failure failure) implements ScenarioManifestCaptureResult {
        public Failed {
            Objects.requireNonNull(failure, "failure");
        }
    }

    record CapturedMember(Path path, String repositoryRelativePath, byte[] bytes) {
        public CapturedMember {
            path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            bytes = Objects.requireNonNull(bytes, "bytes").clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
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
        MEMBER_READ_FAILED
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
