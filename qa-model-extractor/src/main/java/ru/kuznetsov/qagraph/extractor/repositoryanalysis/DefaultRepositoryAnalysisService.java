package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.assembly.ProjectEvidenceGraphAggregator;
import ru.kuznetsov.qagraph.extractor.assembly.ProjectEvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.serialization.CanonicalProjectSerializer;
import ru.kuznetsov.qagraph.extractor.serialization.ProjectSerializationMetadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultRepositoryAnalysisService implements RepositoryAnalysisService {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final DiscoveryStep discovery;
    private final AggregationStep aggregation;
    private final SerializationStep serialization;

    public DefaultRepositoryAnalysisService() {
        RepositoryEvidenceDiscovery repositoryDiscovery = new RepositoryEvidenceDiscovery();
        ProjectEvidenceGraphAggregator graphAggregator = new ProjectEvidenceGraphAggregator();
        CanonicalProjectSerializer projectSerializer = new CanonicalProjectSerializer();
        this.discovery = repositoryDiscovery::discover;
        this.aggregation = graphAggregator::aggregate;
        this.serialization = projectSerializer::serializeProject;
    }

    DefaultRepositoryAnalysisService(
            DiscoveryStep discovery,
            AggregationStep aggregation,
            SerializationStep serialization
    ) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.aggregation = Objects.requireNonNull(aggregation, "aggregation");
        this.serialization = Objects.requireNonNull(serialization, "serialization");
    }

    @Override
    public RepositoryAnalysisResult analyze(RepositoryAnalysisRequest request) {
        Objects.requireNonNull(request, "request");

        RepositoryEvidenceDiscoveryResult discovered;
        try {
            discovered = Objects.requireNonNull(discovery.discover(request), "discovery result");
        } catch (Exception exception) {
            return failed(0, "Repository evidence discovery failed");
        }

        int operationCount = discovered.operationEvidence().size();
        ProjectEvidenceGraphProjection projectGraph;
        try {
            projectGraph = Objects.requireNonNull(
                    aggregation.aggregate(discovered.operationEvidence()), "aggregation result");
        } catch (Exception exception) {
            return failed(operationCount, "Project evidence aggregation failed");
        }

        if (projectGraph.businessOperations().isEmpty()) {
            return failed(0, "Repository analysis discovered no supported operations");
        }

        String fingerprint = fingerprint(request.projectName(), projectGraph);
        String projectIdentity = "PROJECT-" + fingerprint;
        ProjectSerializationMetadata metadata = metadata(
                request.projectName(), projectIdentity, fingerprint, projectGraph.businessOperations().getFirst().id());

        JsonNode canonicalProject;
        try {
            byte[] serialized = Objects.requireNonNull(
                    serialization.serialize(projectGraph, metadata), "serialized project");
            canonicalProject = JSON.readTree(serialized);
            if (canonicalProject == null) throw new IllegalStateException("serialized project is empty");
        } catch (Exception exception) {
            return failed(operationCount, "Canonical project serialization failed");
        }

        RepositoryAnalysisStatus status = discovered.warnings().isEmpty()
                ? RepositoryAnalysisStatus.COMPLETE
                : RepositoryAnalysisStatus.PARTIAL;
        return new RepositoryAnalysisResult(
                status,
                projectIdentity,
                canonicalProject,
                operationCount,
                discovered.warnings(),
                Optional.empty());
    }

    private static RepositoryAnalysisResult failed(int operationCount, String message) {
        return new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.FAILED,
                null,
                null,
                operationCount,
                List.of(),
                Optional.of(message));
    }

    private static ProjectSerializationMetadata metadata(
            String projectName,
            String projectIdentity,
            String fingerprint,
            String subjectId
    ) {
        String sourceId = "SOURCE-REPOSITORY-" + fingerprint;
        String locator = "repository:" + fingerprint;
        String checksum = "sha256:" + fingerprint;
        return new ProjectSerializationMetadata(
                projectIdentity,
                projectName,
                "Deterministically extracted repository evidence.",
                fingerprint,
                Map.of("contentFingerprint", checksum),
                new ProjectSerializationMetadata.RepositorySource(
                        sourceId, projectName, fingerprint, locator, locator, checksum, Map.of()),
                subjectId,
                new ProjectSerializationMetadata.EvidenceMetadata(
                        fingerprint,
                        checksum,
                        checksum,
                        "PROVENANCE-" + fingerprint,
                        locator,
                        checksum,
                        "Deterministic whole-repository evidence discovery and aggregation"));
    }

    private static String fingerprint(String projectName, ProjectEvidenceGraphProjection graph) {
        return sha256(projectName + '\n' + graph);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @FunctionalInterface
    interface DiscoveryStep {
        RepositoryEvidenceDiscoveryResult discover(RepositoryAnalysisRequest request) throws Exception;
    }

    @FunctionalInterface
    interface AggregationStep {
        ProjectEvidenceGraphProjection aggregate(Collection<EvidenceGraphProjection> projections) throws Exception;
    }

    @FunctionalInterface
    interface SerializationStep {
        byte[] serialize(ProjectEvidenceGraphProjection graph, ProjectSerializationMetadata metadata) throws Exception;
    }
}
