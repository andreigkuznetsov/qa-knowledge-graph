package ru.kuznetsov.qaip.core.validation;

import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocument;
import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocuments;

import java.util.List;
import java.util.Map;

final class ValidationFixtures {
    private ValidationFixtures() { }

    static Node node(String id, String type) {
        return new Node(id, type, id, null, null, List.of(), List.of(), Map.of(), Map.of());
    }

    static Relationship relationship(String id, String from, String type, String to) {
        return new Relationship(id, from, type, to, Map.of(), List.of());
    }

    static Project project(List<Node> nodes, List<Relationship> relationships) {
        return new Project("qaip-project-v1", "0.1",
                new Metadata("P-1", "Project", null, null, Map.of()), List.of(),
                new Subject("local-1"), nodes, relationships,
                new EvidenceManifest("impact-evidence-manifest-v1", "source", Map.of(),
                        "impact-evidence-normalization-v1", "impact-evidence-canonical-v1",
                        "fingerprint", List.of(), List.of(), List.of()),
                List.of(new DeclaredChange("NODE", "N-1", "ADDED", "0.1", null, Map.of())),
                Map.of());
    }

    static BoundProjectDocument bound(List<Node> nodes, List<Relationship> relationships) {
        return BoundProjectDocuments.forTesting(project(nodes, relationships));
    }
}
