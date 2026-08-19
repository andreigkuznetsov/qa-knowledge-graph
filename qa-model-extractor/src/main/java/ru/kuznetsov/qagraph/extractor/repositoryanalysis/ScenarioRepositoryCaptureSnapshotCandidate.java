package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, untrusted ADR-013 Repository Capture Snapshot candidate. */
public final class ScenarioRepositoryCaptureSnapshotCandidate {
    private final SnapshotIdentity identity;
    private final ContractIdentifiers contractIdentifiers;
    private final List<ScenarioManifestStableCaptureResult.CapturedMember> members;
    private final List<ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry> unsupportedMatchingEntries;
    private final String mutationDetectionVersion;
    private final CaptureProvenance provenance;

    private ScenarioRepositoryCaptureSnapshotCandidate(
            SnapshotIdentity identity,
            ContractIdentifiers contractIdentifiers,
            ScenarioManifestStableCaptureResult.Completed capture,
            CaptureProvenance provenance
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.contractIdentifiers = Objects.requireNonNull(contractIdentifiers, "contractIdentifiers");
        this.members = List.copyOf(capture.members());
        this.unsupportedMatchingEntries = List.copyOf(capture.unsupportedMatchingEntries());
        this.mutationDetectionVersion = capture.mutationDetectionVersion();
        this.provenance = Objects.requireNonNull(provenance, "provenance");
    }

    /**
     * Constructs a candidate only from a completed stable capture. Fingerprint serialization remains
     * exclusively owned by {@link RepositoryCaptureFingerprintEncoder}.
     */
    public static ScenarioRepositoryCaptureSnapshotCandidate create(
            ScenarioManifestStableCaptureResult captureResult,
            String sourceId,
            String snapshotId,
            ContractIdentifiers contractIdentifiers,
            CaptureProvenance provenance
    ) {
        Objects.requireNonNull(captureResult, "captureResult");
        sourceId = requireNonBlank(sourceId, "sourceId");
        snapshotId = requireNonBlank(snapshotId, "snapshotId");
        Objects.requireNonNull(contractIdentifiers, "contractIdentifiers");
        Objects.requireNonNull(provenance, "provenance");

        if (!(captureResult instanceof ScenarioManifestStableCaptureResult.Completed capture)) {
            throw new IllegalArgumentException("a completed stable capture is required");
        }
        if (!RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION
                .equals(capture.mutationDetectionVersion())) {
            throw new IllegalArgumentException("capture mutation-detection version is not ADR-013 v1");
        }

        RepositoryCaptureFingerprintInput fingerprintInput = fingerprintInput(
                sourceId, contractIdentifiers, capture.members(), capture.unsupportedMatchingEntries());

        RepositoryCaptureFingerprint contentFingerprint =
                RepositoryCaptureFingerprintEncoder.fingerprint(fingerprintInput);
        return new ScenarioRepositoryCaptureSnapshotCandidate(
                new SnapshotIdentity(sourceId, snapshotId, contentFingerprint),
                contractIdentifiers,
                capture,
                provenance);
    }

    /** Exact authoritative ADR-013 input whose fingerprint identifies this approved candidate. */
    public RepositoryCaptureFingerprintInput repositoryCaptureFingerprintInput() {
        return fingerprintInput(sourceId(), contractIdentifiers, members, unsupportedMatchingEntries);
    }

    private static RepositoryCaptureFingerprintInput fingerprintInput(
            String sourceId,
            ContractIdentifiers contractIdentifiers,
            List<ScenarioManifestStableCaptureResult.CapturedMember> members,
            List<ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry> unsupportedMatchingEntries
    ) {
        return new RepositoryCaptureFingerprintInput(
                sourceId,
                contractIdentifiers.sourceContractVersion(),
                contractIdentifiers.sourceProfile(),
                contractIdentifiers.discoveryProfileVersion(),
                contractIdentifiers.pathNormalizationVersion(),
                contractIdentifiers.orderingVersion(),
                contractIdentifiers.memberByteFingerprintAlgorithm(),
                members.stream()
                        .map(member -> new RepositoryCaptureFingerprintInput.CapturedMember(
                                member.repositoryRelativePath(),
                                member.rawByteLength(),
                                member.rawMemberFingerprint()))
                        .toList(),
                unsupportedMatchingEntries.stream()
                        .map(entry -> new RepositoryCaptureFingerprintInput.UnsupportedMatchingEntry(
                                entry.repositoryRelativePath(),
                                entry.entryKind(),
                                entry.stableDiagnosticCode()))
                        .toList());
    }

    public SnapshotIdentity identity() {
        return identity;
    }

    public String sourceId() {
        return identity.sourceId();
    }

    public String snapshotId() {
        return identity.snapshotId();
    }

    public RepositoryCaptureFingerprint contentFingerprint() {
        return identity.contentFingerprint();
    }

    public ContractIdentifiers contractIdentifiers() {
        return contractIdentifiers;
    }

    public List<ScenarioManifestStableCaptureResult.CapturedMember> members() {
        return members;
    }

    public List<ScenarioManifestStableCaptureResult.UnsupportedMatchingEntry> unsupportedMatchingEntries() {
        return unsupportedMatchingEntries;
    }

    public String mutationDetectionVersion() {
        return mutationDetectionVersion;
    }

    public CaptureProvenance provenance() {
        return provenance;
    }

    /** The complete ADR-006 snapshot identity; no component alone identifies the observation. */
    public record SnapshotIdentity(
            String sourceId,
            String snapshotId,
            RepositoryCaptureFingerprint contentFingerprint
    ) {
        public SnapshotIdentity {
            sourceId = requireNonBlank(sourceId, "sourceId");
            snapshotId = requireNonBlank(snapshotId, "snapshotId");
            Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        }
    }

    /** Caller-supplied identity-bearing contract/profile versions used by the approved encoder. */
    public record ContractIdentifiers(
            String sourceContractVersion,
            String sourceProfile,
            String discoveryProfileVersion,
            String pathNormalizationVersion,
            String orderingVersion,
            String memberByteFingerprintAlgorithm
    ) {
        public ContractIdentifiers {
            sourceContractVersion = requireNonBlank(sourceContractVersion, "sourceContractVersion");
            sourceProfile = requireNonBlank(sourceProfile, "sourceProfile");
            discoveryProfileVersion = requireNonBlank(discoveryProfileVersion, "discoveryProfileVersion");
            pathNormalizationVersion = requireNonBlank(pathNormalizationVersion, "pathNormalizationVersion");
            orderingVersion = requireNonBlank(orderingVersion, "orderingVersion");
            memberByteFingerprintAlgorithm = requireNonBlank(
                    memberByteFingerprintAlgorithm, "memberByteFingerprintAlgorithm");
            if (!RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER.equals(memberByteFingerprintAlgorithm)) {
                throw new IllegalArgumentException("unsupported memberByteFingerprintAlgorithm: "
                        + memberByteFingerprintAlgorithm);
            }
        }
    }

    /** Observation metadata deliberately excluded from the repository content fingerprint. */
    public record CaptureProvenance(
            Optional<Instant> capturedAt,
            Optional<String> repositoryRevision,
            Optional<String> repositoryBranch,
            Optional<String> absoluteCheckoutPath,
            Optional<String> provenanceRef,
            Map<String, String> captureMetadata
    ) {
        public CaptureProvenance {
            capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
            repositoryRevision = optionalText(repositoryRevision, "repositoryRevision");
            repositoryBranch = optionalText(repositoryBranch, "repositoryBranch");
            absoluteCheckoutPath = optionalText(absoluteCheckoutPath, "absoluteCheckoutPath");
            provenanceRef = optionalText(provenanceRef, "provenanceRef");
            captureMetadata = Map.copyOf(Objects.requireNonNull(captureMetadata, "captureMetadata"));
            captureMetadata.forEach((key, value) -> {
                requireNonBlank(key, "captureMetadata key");
                requireNonBlank(value, "captureMetadata value");
            });
        }

        public static CaptureProvenance empty() {
            return new CaptureProvenance(
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
        }

        private static Optional<String> optionalText(Optional<String> value, String field) {
            Objects.requireNonNull(value, field);
            value.ifPresent(text -> requireNonBlank(text, field));
            return value;
        }
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
