package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.RelationshipType;
import java.util.*;

/** Deterministic analyzer for the deliberately narrow impact-evidence slice. */
public final class ImpactEvidenceAnalyzer {
    private final ManifestValidator validator = new ManifestValidator();
    private final AcceptedChangeDomain changeDomain = new AcceptedChangeDomain();

    public ImpactEvidenceResult analyze(ImpactEvidenceRequest request) {
        if (request == null) return failed(FailureCode.INVALID_REQUEST, "NULL_REQUEST", "request must not be null", "");
        Optional<ImpactAnalysisFailure> invalid = validator.validate(request.manifest(), request.context());
        if (invalid.isPresent()) return new ImpactEvidenceFailed(invalid.get());
        AcceptedChangeDomain.Extraction extraction = changeDomain.extract(request.verifiedChangeSet());
        if (extraction.failure().isPresent()) return new ImpactEvidenceFailed(extraction.failure().get());
        Optional<ImpactAnalysisFailure> mismatch = changeDomain.validateCompatibility(
                request.verifiedChangeSet(), extraction.supported(), request.manifest());
        if (mismatch.isPresent()) return new ImpactEvidenceFailed(mismatch.get());
        Map<String, ArtifactIdentityAssertion> assertions = new HashMap<>();
        request.manifest().identityAssertions().forEach(a -> assertions.put(a.localArtifactId(), a));
        ArtifactIdentityAssertion subject = assertions.get(request.subject().localArtifactId());
        if (subject == null) return failed(FailureCode.INVALID_REQUEST, "SUBJECT_NOT_DECLARED", "subject has no identity assertion", request.subject().localArtifactId());
        if (subject.nodeType() != NodeType.BUSINESS_RULE)
            return failed(FailureCode.INCOMPATIBLE_CHANGE_DOMAIN, "UNSUPPORTED_SUBJECT_TYPE",
                    "subject must be a BUSINESS_RULE", subject.assertionId());
        if (!(subject.resolution() instanceof ResolvedIdentity resolved))
            return completed(request, ImpactClassification.UNKNOWN, Optional.empty(),
                    List.of(UnknownReason.UNRESOLVED_SUBJECT_IDENTITY), List.of());

        for (AcceptedChangeDomain.Supported candidate : extraction.supported()) {
            DeclaredChange change = candidate.declaration();
            if (change.identity().equals(resolved.identity())) {
                return completed(request, ImpactClassification.AFFECTED,
                        Optional.of(new DirectChangeProof(request.verifiedChangeSet(),
                                candidate.declarationIndex(), subject)),
                        List.of(), List.of());
            }
        }

        List<QualifiedRelationship> qualified = new ArrayList<>();
        List<RejectedEvidenceReference> rejected = new ArrayList<>();
        for (RelationshipEvidence relationship : request.manifest().relationships()) {
            RelationshipQualification q = qualify(relationship, request.manifest(), assertions);
            if (q instanceof QualifiedRelationship accepted) qualified.add(accepted);
            else rejected.add(((RejectedRelationship) q).reference());
        }
        qualified.sort(Comparator.comparing(q -> q.evidence().datumId()));
        Optional<RelationshipPathProof> path = findPath(changedRoots(extraction.supported()),
                resolved.identity(), request.manifest().snapshot(), qualified);
        if (path.isPresent()) return completed(request, ImpactClassification.AFFECTED,
                Optional.of(path.get()), List.of(), rejected);
        return completed(request, ImpactClassification.UNKNOWN, Optional.empty(),
                List.of(UnknownReason.NO_QUALIFIED_IMPACT_PROOF), rejected);
    }

    private RelationshipQualification qualify(RelationshipEvidence r, FrozenEvidenceManifest m,
            Map<String, ArtifactIdentityAssertion> assertions) {
        List<QualificationReason> reasons = new ArrayList<>();
        if (!r.snapshot().sourceId().equals(m.sourceId())) reasons.add(QualificationReason.SOURCE_MISMATCH);
        if (!r.snapshot().equals(m.snapshot())) reasons.add(QualificationReason.SNAPSHOT_MISMATCH);
        ArtifactIdentityAssertion source = assertions.get(r.sourceLocalId());
        ArtifactIdentityAssertion target = assertions.get(r.targetLocalId());
        if (source == null) reasons.add(QualificationReason.MISSING_SOURCE_ENDPOINT);
        if (target == null) reasons.add(QualificationReason.MISSING_TARGET_ENDPOINT);
        if (source != null && !(source.resolution() instanceof ResolvedIdentity)) reasons.add(QualificationReason.UNRESOLVED_SOURCE_ENDPOINT);
        if (target != null && !(target.resolution() instanceof ResolvedIdentity)) reasons.add(QualificationReason.UNRESOLVED_TARGET_ENDPOINT);
        Set<String> provenance = new HashSet<>(); m.provenance().forEach(p -> provenance.add(p.provenanceId()));
        if (!provenance.contains(r.provenanceId())) reasons.add(QualificationReason.MISSING_PROVENANCE);
        if (r.relationshipType() != RelationshipType.DEPENDS_ON) reasons.add(QualificationReason.WRONG_RELATIONSHIP_TYPE);
        if (source != null && source.nodeType() != NodeType.BUSINESS_RULE) reasons.add(QualificationReason.WRONG_SOURCE_ENDPOINT_TYPE);
        if (target != null && target.nodeType() != NodeType.BUSINESS_RULE) reasons.add(QualificationReason.WRONG_TARGET_ENDPOINT_TYPE);
        if (!r.normalizationVersion().equals(ImpactEvidenceVersions.NORMALIZATION)) reasons.add(QualificationReason.UNSUPPORTED_NORMALIZATION);
        if (!reasons.isEmpty()) return new RejectedRelationship(new RejectedEvidenceReference(r.datumId(), reasons));
        CanonicalIdentity from = ((ResolvedIdentity) target.resolution()).identity();
        CanonicalIdentity to = ((ResolvedIdentity) source.resolution()).identity();
        return new QualifiedRelationship(r, from, to);
    }

    private List<CanonicalIdentity> changedRoots(List<AcceptedChangeDomain.Supported> changes) {
        List<CanonicalIdentity> roots = new ArrayList<>();
        for (AcceptedChangeDomain.Supported supported : changes) {
            CanonicalIdentity identity = supported.declaration().identity();
            if (!roots.contains(identity)) roots.add(identity);
        }
        return roots;
    }

    private Optional<RelationshipPathProof> findPath(List<CanonicalIdentity> roots,
            CanonicalIdentity subject, EvidenceSnapshotRef snapshot, List<QualifiedRelationship> edges) {
        Map<CanonicalIdentity, List<QualifiedRelationship>> outgoing = new HashMap<>();
        for (QualifiedRelationship edge : edges) outgoing.computeIfAbsent(edge.propagationFrom(), k -> new ArrayList<>()).add(edge);
        outgoing.values().forEach(v -> v.sort(Comparator
                .comparing((QualifiedRelationship e) -> e.propagationTo().value())
                .thenComparing(e -> e.evidence().datumId())
                .thenComparing(e -> e.evidence().contentFingerprint())));
        record State(CanonicalIdentity id, CanonicalIdentity root, List<QualifiedPathStep> path) { }
        ArrayDeque<State> queue = new ArrayDeque<>(); Set<CanonicalIdentity> visited = new HashSet<>();
        roots.forEach(root -> { if (visited.add(root)) queue.add(new State(root, root, List.of())); });
        while (!queue.isEmpty()) {
            State current = queue.remove();
            for (QualifiedRelationship edge : outgoing.getOrDefault(current.id(), List.of())) {
                if (!visited.add(edge.propagationTo())) continue;
                List<QualifiedPathStep> path = new ArrayList<>(current.path());
                path.add(new QualifiedPathStep(edge));
                if (edge.propagationTo().equals(subject)) return Optional.of(new RelationshipPathProof(current.root(), subject, snapshot, path));
                queue.add(new State(edge.propagationTo(), current.root(), List.copyOf(path)));
            }
        }
        return Optional.empty();
    }
    private ImpactEvidenceCompleted completed(ImpactEvidenceRequest r, ImpactClassification c,
            Optional<ImpactProof> p, List<UnknownReason> reasons, List<RejectedEvidenceReference> rejected) {
        ArtifactIdentityAssertion assertion = r.manifest().identityAssertions().stream()
                .filter(a -> a.localArtifactId().equals(r.subject().localArtifactId())).findFirst().orElseThrow();
        return new ImpactEvidenceCompleted(new ImpactConclusion(assertion, c, p, reasons,
                r.context(), rejected));
    }
    private ImpactEvidenceFailed failed(FailureCode f, String code, String message, String id) {
        return new ImpactEvidenceFailed(new ImpactAnalysisFailure(f, List.of(new AnalysisDiagnostic(code, message, id))));
    }
}
