package ru.kuznetsov.qagraph.extractor.serialization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record ProjectSerializationMetadata(
        String projectId,
        String projectName,
        String projectDescription,
        String projectVersion,
        Map<String, String> projectMetadata,
        RepositorySource repositorySource,
        String subjectLocalArtifactId,
        EvidenceMetadata evidence
) {
    public ProjectSerializationMetadata {
        requireNonBlank(projectId, "projectId");
        requireNonBlank(projectName, "projectName");
        projectMetadata = immutableSortedMap(projectMetadata, "projectMetadata");
        Objects.requireNonNull(repositorySource, "repositorySource");
        requireNonBlank(subjectLocalArtifactId, "subjectLocalArtifactId");
        Objects.requireNonNull(evidence, "evidence");
    }

    public record RepositorySource(
            String id,
            String name,
            String revision,
            String externalReference,
            String uri,
            String checksum,
            Map<String, String> metadata
    ) {
        public RepositorySource {
            requireNonBlank(id, "repositorySource.id");
            requireNonBlank(name, "repositorySource.name");
            requireNonBlank(revision, "repositorySource.revision");
            requireNonBlank(externalReference, "repositorySource.externalReference");
            requireNonBlank(uri, "repositorySource.uri");
            requireNonBlank(checksum, "repositorySource.checksum");
            metadata = immutableSortedMap(metadata, "repositorySource.metadata");
        }
    }

    public record EvidenceMetadata(
            String snapshotId,
            String contentFingerprint,
            String manifestFingerprint,
            String provenanceId,
            String originLocator,
            String originHash,
            String normalizationActivity
    ) {
        public EvidenceMetadata {
            requireNonBlank(snapshotId, "evidence.snapshotId");
            requireNonBlank(contentFingerprint, "evidence.contentFingerprint");
            requireNonBlank(manifestFingerprint, "evidence.manifestFingerprint");
            requireNonBlank(provenanceId, "evidence.provenanceId");
            requireNonBlank(originLocator, "evidence.originLocator");
            requireNonBlank(originHash, "evidence.originHash");
            requireNonBlank(normalizationActivity, "evidence.normalizationActivity");
        }
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }

    private static Map<String, String> immutableSortedMap(Map<String, String> values, String field) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(
                Objects.requireNonNull(values, field))));
    }
}
