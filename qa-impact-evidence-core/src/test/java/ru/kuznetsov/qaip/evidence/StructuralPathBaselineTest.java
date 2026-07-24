package ru.kuznetsov.qaip.evidence;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidence.EvidenceTestFixtures.*;

class StructuralPathBaselineTest {
    record BaselineResult(boolean direct, boolean path, List<String> warnings) { }

    @Test void executableBaselineDemonstratesQualificationAndProjectionValue() throws Exception {
        var accepted = verified("BR-C");
        var assertions = List.of(resolved("c", "BR-C"), resolved("s", "BR-S"));

        compare(accepted, manifest(assertions, List.of(relation("qualified", "s", "c"))),
                "s", true, ImpactClassification.AFFECTED);
        compare(accepted, manifest(assertions, List.of()), "s", false, ImpactClassification.UNKNOWN);

        var unresolvedManifest = manifest(List.of(resolved("c", "BR-C"), unresolved("s")), List.of());
        BaselineResult unresolvedBaseline = baseline(accepted, unresolvedManifest, "s");
        ImpactConclusion unresolved = completed(new ImpactEvidenceAnalyzer().analyze(
                request(accepted, unresolvedManifest, "s")));
        assertFalse(unresolvedBaseline.path());
        assertEquals(List.of("UNRESOLVED_SUBJECT"), unresolvedBaseline.warnings());
        assertEquals(UnknownReason.UNRESOLVED_SUBJECT_IDENTITY, unresolved.unknownReasons().getFirst());

        var wrongSnapshot = new RelationshipEvidence("wrong-snapshot",
                new EvidenceSnapshotRef("jira", "other", H1), "s", "c",
                ru.kuznetsov.qagraph.model.RelationshipType.DEPENDS_ON, "blocks",
                ImpactEvidenceVersions.NORMALIZATION, H2, "p-1");
        assertStructuralPathButUnknown(accepted, manifest(assertions, List.of(wrongSnapshot)), "s");

        var missingProvenance = new RelationshipEvidence("missing-provenance", SNAPSHOT,
                "s", "c", ru.kuznetsov.qagraph.model.RelationshipType.DEPENDS_ON, "blocks",
                ImpactEvidenceVersions.NORMALIZATION, H2, "absent");
        assertStructuralPathButUnknown(accepted, manifest(assertions, List.of(missingProvenance)), "s");

        var directManifest = manifest(List.of(resolved("c", "BR-C"), resolved("x", "BR-X")),
                List.of(relation("cycle-1", "x", "c"), relation("cycle-2", "c", "x")));
        BaselineResult directBaseline = baseline(accepted, directManifest, "c");
        ImpactConclusion direct = completed(new ImpactEvidenceAnalyzer().analyze(
                request(accepted, directManifest, "c")));
        assertTrue(directBaseline.direct());
        assertTrue(directBaseline.path());
        assertInstanceOf(DirectChangeProof.class, direct.proof().orElseThrow());
    }

    private void compare(ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet accepted,
            FrozenEvidenceManifest manifest, String subject, boolean expectedPath,
            ImpactClassification expected) {
        assertEquals(expectedPath, baseline(accepted, manifest, subject).path());
        assertEquals(expected, completed(new ImpactEvidenceAnalyzer().analyze(
                request(accepted, manifest, subject))).classification());
    }

    private void assertStructuralPathButUnknown(
            ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet accepted,
            FrozenEvidenceManifest manifest, String subject) {
        BaselineResult baseline = baseline(accepted, manifest, subject);
        ImpactConclusion conclusion = completed(new ImpactEvidenceAnalyzer().analyze(
                request(accepted, manifest, subject)));
        assertTrue(baseline.path());
        assertFalse(baseline.warnings().isEmpty());
        assertEquals(ImpactClassification.UNKNOWN, conclusion.classification());
        assertFalse(conclusion.rejectedEvidence().isEmpty());
    }

    private BaselineResult baseline(
            ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet accepted,
            FrozenEvidenceManifest manifest, String subjectLocal) {
        Map<String, ArtifactIdentityAssertion> assertions = new HashMap<>();
        manifest.identityAssertions().forEach(a -> assertions.put(a.localArtifactId(), a));
        ArtifactIdentityAssertion subject = assertions.get(subjectLocal);
        if (subject == null || !(subject.resolution() instanceof ResolvedIdentity resolvedSubject))
            return new BaselineResult(false, false, List.of("UNRESOLVED_SUBJECT"));
        Set<CanonicalIdentity> changed = new HashSet<>();
        accepted.declaredChangeSet().changes().forEach(c -> changed.add(c.identity()));
        boolean direct = changed.contains(resolvedSubject.identity());
        Map<CanonicalIdentity, List<CanonicalIdentity>> outgoing = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        for (RelationshipEvidence relation : manifest.relationships()) {
            ArtifactIdentityAssertion source = assertions.get(relation.sourceLocalId());
            ArtifactIdentityAssertion target = assertions.get(relation.targetLocalId());
            if (source != null && target != null && source.resolution() instanceof ResolvedIdentity s
                    && target.resolution() instanceof ResolvedIdentity t) {
                outgoing.computeIfAbsent(t.identity(), ignored -> new ArrayList<>()).add(s.identity());
            }
            boolean provenance = manifest.provenance().stream()
                    .anyMatch(p -> p.provenanceId().equals(relation.provenanceId()));
            if (!relation.snapshot().equals(manifest.snapshot())) warnings.add(relation.datumId()+":SNAPSHOT");
            if (!provenance) warnings.add(relation.datumId()+":PROVENANCE");
        }
        ArrayDeque<CanonicalIdentity> queue = new ArrayDeque<>(changed);
        Set<CanonicalIdentity> visited = new HashSet<>(changed);
        boolean path = false;
        while (!queue.isEmpty() && !path) for (CanonicalIdentity next : outgoing.getOrDefault(queue.remove(), List.of())) {
            if (next.equals(resolvedSubject.identity())) { path = true; break; }
            if (visited.add(next)) queue.add(next);
        }
        return new BaselineResult(direct, path, List.copyOf(warnings));
    }

    private ImpactConclusion completed(ImpactEvidenceResult result) {
        return assertInstanceOf(ImpactEvidenceCompleted.class, result).conclusion();
    }
}
