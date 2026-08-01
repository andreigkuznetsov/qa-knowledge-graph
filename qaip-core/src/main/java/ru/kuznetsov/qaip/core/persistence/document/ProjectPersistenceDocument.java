package ru.kuznetsov.qaip.core.persistence.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.List;
import java.util.Map;

record ProjectPersistenceDocument(String persistenceDocumentVersion, ProjectDocument project) { }

record ProjectDocument(String projectContractVersion, String schemaVersion, MetadataDocument metadata,
                       List<PersistenceValueDocument> sources, SubjectDocument subject,
                       List<NodeDocument> nodes, List<RelationshipDocument> relationships,
                       EvidenceManifestDocument evidenceManifest, List<DeclaredChangeDocument> declaredChanges,
                       Map<String, String> analysisContext) {
    ProjectDocument {
        sources = List.copyOf(sources);
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);
        declaredChanges = List.copyOf(declaredChanges);
        analysisContext = Map.copyOf(analysisContext);
    }
}

record MetadataDocument(String id, String name, String description, String version,
                        PersistenceValueDocument attributes) { }

record SubjectDocument(String localArtifactId) { }

record NodeDocument(String id, String type, String name, String description, String status,
                    List<String> tags, List<PersistenceValueDocument> sourceReferences,
                    PersistenceValueDocument metadata, PersistenceValueDocument attributes) {
    NodeDocument {
        tags = List.copyOf(tags);
        sourceReferences = List.copyOf(sourceReferences);
    }
}

record RelationshipDocument(String id, String from, String type, String to,
                            PersistenceValueDocument properties,
                            List<PersistenceValueDocument> sourceReferences) {
    RelationshipDocument {
        sourceReferences = List.copyOf(sourceReferences);
    }
}

record EvidenceManifestDocument(String contractVersion, String sourceId, PersistenceValueDocument snapshot,
                                String normalizationVersion, String canonicalizationVersion,
                                String manifestFingerprint, List<PersistenceValueDocument> identityAssertions,
                                List<PersistenceValueDocument> relationships,
                                List<PersistenceValueDocument> provenance) {
    EvidenceManifestDocument {
        identityAssertions = List.copyOf(identityAssertions);
        relationships = List.copyOf(relationships);
        provenance = List.copyOf(provenance);
    }
}

record DeclaredChangeDocument(String artifactCategory, String canonicalIdentity, String changeKind,
                              String schemaVersion, PersistenceValueDocument beforeState,
                              PersistenceValueDocument afterState) { }

@JsonInclude(JsonInclude.Include.NON_NULL)
record PersistenceValueDocument(String type, JsonNode value, String unscaledValue, Integer scale,
                                List<PersistenceValueDocument> items,
                                Map<String, PersistenceValueDocument> entries) {
    static PersistenceValueDocument ofType(String type) {
        return new PersistenceValueDocument(type, null, null, null, null, null);
    }

    static PersistenceValueDocument scalar(String type, String value) {
        return new PersistenceValueDocument(type, TextNode.valueOf(value), null, null, null, null);
    }

    static PersistenceValueDocument bool(boolean value) {
        return new PersistenceValueDocument("BOOLEAN", BooleanNode.valueOf(value), null, null, null, null);
    }

    static PersistenceValueDocument decimal(String unscaledValue, int scale) {
        return new PersistenceValueDocument("BIG_DECIMAL", null, unscaledValue, scale, null, null);
    }

    static PersistenceValueDocument list(List<PersistenceValueDocument> items) {
        return new PersistenceValueDocument("LIST", null, null, null, List.copyOf(items), null);
    }

    static PersistenceValueDocument object(Map<String, PersistenceValueDocument> entries) {
        return new PersistenceValueDocument("OBJECT", null, null, null, null, Map.copyOf(entries));
    }
}
