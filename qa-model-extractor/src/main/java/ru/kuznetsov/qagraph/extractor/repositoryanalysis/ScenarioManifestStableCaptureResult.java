package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;

import java.util.List;
import java.util.Objects;

/** Untrusted, immutable result of one ADR-013 Scenario repository capture attempt. */
public sealed interface ScenarioManifestStableCaptureResult
        permits ScenarioManifestStableCaptureResult.Completed, ScenarioManifestStableCaptureResult.Failed {

    record Completed(
            List<CapturedMember> members,
            List<UnsupportedMatchingEntry> unsupportedMatchingEntries,
            String mutationDetectionVersion
    ) implements ScenarioManifestStableCaptureResult {
        public Completed {
            members = List.copyOf(Objects.requireNonNull(members, "members"));
            unsupportedMatchingEntries = List.copyOf(Objects.requireNonNull(
                    unsupportedMatchingEntries, "unsupportedMatchingEntries"));
            mutationDetectionVersion = requireNonBlank(
                    mutationDetectionVersion, "mutationDetectionVersion");
        }
    }

    record Failed(Failure failure) implements ScenarioManifestStableCaptureResult {
        public Failed {
            Objects.requireNonNull(failure, "failure");
        }
    }

    record CapturedMember(
            String repositoryRelativePath,
            byte[] bytes,
            long rawByteLength,
            RawSourceMemberFingerprint rawMemberFingerprint
    ) {
        public CapturedMember {
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            bytes = Objects.requireNonNull(bytes, "bytes").clone();
            if (rawByteLength < 0 || rawByteLength != bytes.length) {
                throw new IllegalArgumentException("rawByteLength must equal exact captured byte length");
            }
            Objects.requireNonNull(rawMemberFingerprint, "rawMemberFingerprint");
            if (!RawSourceMemberFingerprint.calculate(bytes).equals(rawMemberFingerprint)) {
                throw new IllegalArgumentException("rawMemberFingerprint must bind exact captured bytes");
            }
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    record UnsupportedMatchingEntry(
            String repositoryRelativePath,
            String entryKind,
            String stableDiagnosticCode
    ) {
        public UnsupportedMatchingEntry {
            repositoryRelativePath = requireNonBlank(repositoryRelativePath, "repositoryRelativePath");
            entryKind = requireNonBlank(entryKind, "entryKind");
            stableDiagnosticCode = requireNonBlank(stableDiagnosticCode, "stableDiagnosticCode");
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
        CONCURRENT_SOURCE_MUTATION,
        MEMBER_READ_FAILED,
        DISCOVERY_FAILED,
        DISCOVERY_STRUCTURAL_FAILURE
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
