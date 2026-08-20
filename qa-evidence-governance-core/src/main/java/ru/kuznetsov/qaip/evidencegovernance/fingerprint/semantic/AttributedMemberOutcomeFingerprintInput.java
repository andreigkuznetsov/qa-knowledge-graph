package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Complete immutable ADR-015 semantic input for one attributed member outcome. */
public record AttributedMemberOutcomeFingerprintInput(
        String attributedMemberSemanticCanonicalizationVersion,
        ParentCapturedMemberReference parentMemberReference,
        String claimedAuthority,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        ParseOutcome parseOutcome,
        AttributionOutcome attributionOutcome,
        Optional<String> attributionStructuralLocation,
        String schemaContractIdentifier,
        StructuralAdmissionState structuralAdmissionState,
        List<ScenarioSchemaDiagnostic> schemaDiagnostics,
        Optional<ManifestOccurrenceIdentity> manifestOccurrenceIdentity,
        Optional<ManifestSemanticFingerprint> manifestSemanticFingerprint
) {
    public static final String PARSER_CONTRACT_IDENTIFIER = "scenario-authority-json-parser-v1";
    public static final String ATTRIBUTION_CONTRACT_IDENTIFIER = "scenario-authority-attribution-v1";
    public static final String SCHEMA_CONTRACT_IDENTIFIER = "qaip-scenario-authority-manifest-schema-v1";
    public static final String MANIFEST_OCCURRENCE_IDENTITY_VERSION =
            "qaip-scenario-manifest-occurrence-identity-v1";

    public AttributedMemberOutcomeFingerprintInput {
        requireExact(attributedMemberSemanticCanonicalizationVersion,
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER,
                "attributedMemberSemanticCanonicalizationVersion");
        Objects.requireNonNull(parentMemberReference, "parentMemberReference");
        claimedAuthority = requireText(claimedAuthority, "claimedAuthority");
        requireExact(parserContractIdentifier, PARSER_CONTRACT_IDENTIFIER,
                "parserContractIdentifier");
        requireExact(attributionContractIdentifier, ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        if (parseOutcome != ParseOutcome.PARSED) {
            throw new IllegalArgumentException("attributed member parse outcome must be PARSED");
        }
        if (attributionOutcome != AttributionOutcome.ATTRIBUTED) {
            throw new IllegalArgumentException("attributed member attribution outcome must be ATTRIBUTED");
        }
        attributionStructuralLocation = Objects.requireNonNull(
                attributionStructuralLocation, "attributionStructuralLocation");
        attributionStructuralLocation.ifPresent(value -> requireJsonPointer(value, "attributionStructuralLocation"));
        requireExact(schemaContractIdentifier, SCHEMA_CONTRACT_IDENTIFIER,
                "schemaContractIdentifier");
        Objects.requireNonNull(structuralAdmissionState, "structuralAdmissionState");
        schemaDiagnostics = ScenarioSchemaDiagnostic.canonicalCollection(schemaDiagnostics);
        manifestOccurrenceIdentity = Objects.requireNonNull(
                manifestOccurrenceIdentity, "manifestOccurrenceIdentity");
        manifestSemanticFingerprint = Objects.requireNonNull(
                manifestSemanticFingerprint, "manifestSemanticFingerprint");

        if (structuralAdmissionState == StructuralAdmissionState.STRUCTURALLY_ADMITTED) {
            if (!schemaDiagnostics.isEmpty()
                    || manifestOccurrenceIdentity.isEmpty()) {
                throw new IllegalArgumentException(
                        "STRUCTURALLY_ADMITTED requires empty diagnostics and present Manifest identity");
            }
            manifestOccurrenceIdentity.get().requireMatches(parentMemberReference);
        } else if (schemaDiagnostics.isEmpty()
                || manifestOccurrenceIdentity.isPresent()
                || manifestSemanticFingerprint.isPresent()) {
            throw new IllegalArgumentException(
                    "STRUCTURALLY_REJECTED requires diagnostics and absent Manifest identity/fingerprint");
        }
    }

    public enum ParseOutcome { PARSED }
    public enum AttributionOutcome { ATTRIBUTED }
    public enum StructuralAdmissionState { STRUCTURALLY_ADMITTED, STRUCTURALLY_REJECTED }

    /** Neutral Evidence Governance representation of the exact ADR-014 parent-member reference. */
    public record ParentCapturedMemberReference(
            String parentSourceId,
            String parentSnapshotId,
            RepositoryCaptureFingerprint parentContentFingerprint,
            String normalizedRepositoryRelativePath,
            BigInteger rawByteLength,
            RawSourceMemberFingerprint rawMemberFingerprint
    ) {
        public ParentCapturedMemberReference {
            parentSourceId = requireText(parentSourceId, "parentSourceId");
            parentSnapshotId = requireText(parentSnapshotId, "parentSnapshotId");
            Objects.requireNonNull(parentContentFingerprint, "parentContentFingerprint");
            normalizedRepositoryRelativePath = requireNormalizedPath(
                    normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
            Objects.requireNonNull(rawByteLength, "rawByteLength");
            if (rawByteLength.signum() < 0
                    || rawByteLength.compareTo(CanonicalBinaryWriter.MAX_UNSIGNED_64) > 0) {
                throw new IllegalArgumentException("rawByteLength must have uint64 semantics");
            }
            Objects.requireNonNull(rawMemberFingerprint, "rawMemberFingerprint");
        }

        public ParentCapturedMemberReference(
                String parentSourceId,
                String parentSnapshotId,
                RepositoryCaptureFingerprint parentContentFingerprint,
                String normalizedRepositoryRelativePath,
                long rawByteLength,
                RawSourceMemberFingerprint rawMemberFingerprint
        ) {
            this(parentSourceId, parentSnapshotId, parentContentFingerprint,
                    normalizedRepositoryRelativePath, BigInteger.valueOf(rawByteLength), rawMemberFingerprint);
        }
    }

    /** Exact V1 Manifest occurrence identity, distinct from Manifest semantic content. */
    public record ManifestOccurrenceIdentity(
            String parentSourceId,
            String parentSnapshotId,
            RepositoryCaptureFingerprint parentContentFingerprint,
            String normalizedRepositoryRelativePath,
            String identityVersion
    ) {
        public ManifestOccurrenceIdentity {
            parentSourceId = requireText(parentSourceId, "manifest.parentSourceId");
            parentSnapshotId = requireText(parentSnapshotId, "manifest.parentSnapshotId");
            Objects.requireNonNull(parentContentFingerprint, "manifest.parentContentFingerprint");
            normalizedRepositoryRelativePath = requireNormalizedPath(
                    normalizedRepositoryRelativePath, "manifest.normalizedRepositoryRelativePath");
            requireExact(identityVersion, MANIFEST_OCCURRENCE_IDENTITY_VERSION, "identityVersion");
        }

        private void requireMatches(ParentCapturedMemberReference parent) {
            if (!parentSourceId.equals(parent.parentSourceId)
                    || !parentSnapshotId.equals(parent.parentSnapshotId)
                    || !parentContentFingerprint.equals(parent.parentContentFingerprint)
                    || !normalizedRepositoryRelativePath.equals(parent.normalizedRepositoryRelativePath)) {
                throw new IllegalArgumentException(
                        "Manifest occurrence identity must identify the exact parent member");
            }
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        new CanonicalBinaryWriter().writeText(value);
        return value;
    }

    private static String requireNormalizedPath(String value, String field) {
        requireText(value, field);
        if (value.startsWith("/") || value.endsWith("/") || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(field + " must be a normalized repository-relative path");
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(field + " contains a noncanonical segment");
            }
        }
        return value;
    }

    private static String requireJsonPointer(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException(field + " must be an RFC 6901 JSON Pointer");
        }
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '~') {
                if (++index >= value.length()
                        || (value.charAt(index) != '0' && value.charAt(index) != '1')) {
                    throw new IllegalArgumentException(field + " has an invalid RFC 6901 escape");
                }
            }
        }
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
