package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryMapperTest {
    private final ProjectSummaryMapper mapper = new ProjectSummaryMapper();

    @Test
    void maps_representative_project_exactly_and_deterministically_without_mutation() {
        Project project = project(" P-Id ", " Contract-V1 ", " Schema-V1 ",
                List.of(Map.of("source", 1)), List.of(node()), List.of(relationship()),
                evidence(Map.of("snapshot", List.of(1, 2, 3)), List.of(Map.of("nested", List.of(1, 2))),
                        List.of(Map.of("relationship", 1)), List.of(Map.of("provenance", 1))),
                List.of(change()));
        Project before = project;

        ProjectSummaryResult expected = new ProjectSummaryResult(
                " P-Id ", " Contract-V1 ", " Schema-V1 ", 1, 1, 1, 3, 1);
        assertEquals(expected, mapper.map(project));
        assertEquals(expected, mapper.map(project));
        assertEquals(before, project);
    }

    @Test
    void empty_collections_map_to_zero_and_null_input_is_rejected() {
        assertEquals(new ProjectSummaryResult("P", "contract", "schema", 0, 0, 0, 0, 0),
                mapper.map(project("P", "contract", "schema", List.of(), List.of(), List.of(),
                        evidence(Map.of(), List.of(), List.of(), List.of()), List.of())));
        assertThrows(NullPointerException.class, () -> mapper.map(null));
    }

    @Test
    void each_aggregate_collection_changes_only_its_own_metric() {
        ProjectSummaryResult source = mapper.map(project("P", "c", "s", List.of(Map.of()), List.of(), List.of(),
                evidence(Map.of(), List.of(), List.of(), List.of()), List.of()));
        ProjectSummaryResult node = mapper.map(project("P", "c", "s", List.of(), List.of(node()), List.of(),
                evidence(Map.of(), List.of(), List.of(), List.of()), List.of()));
        ProjectSummaryResult relationship = mapper.map(project("P", "c", "s", List.of(), List.of(),
                List.of(relationship()), evidence(Map.of(), List.of(), List.of(), List.of()), List.of()));
        ProjectSummaryResult change = mapper.map(project("P", "c", "s", List.of(), List.of(), List.of(),
                evidence(Map.of(), List.of(), List.of(), List.of()), List.of(change())));
        assertEquals(List.of(1, 0, 0, 0), aggregateCounts(source));
        assertEquals(List.of(0, 1, 0, 0), aggregateCounts(node));
        assertEquals(List.of(0, 0, 1, 0), aggregateCounts(relationship));
        assertEquals(List.of(0, 0, 0, 1), aggregateCounts(change));
    }

    @Test
    void evidence_is_only_the_sum_of_three_top_level_record_lists() {
        assertEquals(1, summary(evidence(Map.of(), List.of(Map.of()), List.of(), List.of())).evidenceCount());
        assertEquals(1, summary(evidence(Map.of(), List.of(), List.of(Map.of()), List.of())).evidenceCount());
        assertEquals(1, summary(evidence(Map.of(), List.of(), List.of(), List.of(Map.of()))).evidenceCount());
        assertEquals(0, summary(evidence(Map.of("nested", List.of(1, 2, 3)), List.of(), List.of(), List.of())).evidenceCount());
        assertEquals(3, summary(evidence(Map.of(), List.of(Map.of("nested", List.of(1, 2))),
                List.of(Map.of()), List.of(Map.of()))).evidenceCount());
    }

    private ProjectSummaryResult summary(EvidenceManifest evidence) {
        return mapper.map(project("P", "c", "s", List.of(), List.of(), List.of(), evidence, List.of()));
    }
    private static List<Integer> aggregateCounts(ProjectSummaryResult value) {
        return List.of(value.sourceCount(), value.nodeCount(), value.relationshipCount(), value.declaredChangeCount());
    }
    private static Project project(String id, String contract, String schema, List<Map<String, Object>> sources,
                                   List<Node> nodes, List<Relationship> relationships,
                                   EvidenceManifest evidence, List<DeclaredChange> changes) {
        return new Project(contract, schema, new Metadata(id, "name", null, null, Map.of()), sources,
                new Subject("local"), nodes, relationships, evidence, changes, Map.of());
    }
    private static EvidenceManifest evidence(Map<String, Object> snapshot, List<Map<String, Object>> identities,
                                             List<Map<String, Object>> relationships,
                                             List<Map<String, Object>> provenance) {
        return new EvidenceManifest("evidence-v1", "source", snapshot, "normalization", "canonicalization",
                "fingerprint", identities, relationships, provenance);
    }
    private static Node node() {
        return new Node("N", "TYPE", "name", null, null, List.of(), List.of(), Map.of(), Map.of());
    }
    private static Relationship relationship() {
        return new Relationship("R", "N", "TYPE", "N", Map.of(), List.of());
    }
    private static DeclaredChange change() {
        return new DeclaredChange("NODE", "N", "ADDED", "1", null, Map.of());
    }
}
