package ru.kuznetsov.qaip.core.domain;

import java.util.List;
import java.util.Map;

public record Project(String projectContractVersion, String schemaVersion, Metadata metadata,
                      List<Map<String, Object>> sources, Subject subject, List<Node> nodes,
                      List<Relationship> relationships, EvidenceManifest evidenceManifest,
                      List<DeclaredChange> declaredChanges, Map<String, String> analysisContext) {
    public Project {
        sources = sources == null || sources.isEmpty()
                ? List.of()
                : sources.stream().map(Metadata::immutableMap).toList();
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);
        declaredChanges = List.copyOf(declaredChanges);
        analysisContext = analysisContext == null ? Map.of() : Map.copyOf(analysisContext);
    }
}
