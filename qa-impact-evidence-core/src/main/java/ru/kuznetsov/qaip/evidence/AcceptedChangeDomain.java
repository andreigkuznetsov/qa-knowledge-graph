package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.verification.VerifiedChangeSet;
import ru.kuznetsov.qagraph.model.NodeType;

import java.util.*;

/** Fixed BUSINESS_RULE extraction and manifest compatibility for the slice. */
final class AcceptedChangeDomain {
    record Supported(int declarationIndex, DeclaredChange declaration) { }
    record Extraction(List<Supported> supported, Optional<ImpactAnalysisFailure> failure) { }

    Extraction extract(VerifiedChangeSet accepted) {
        List<Supported> supported = new ArrayList<>();
        List<AnalysisDiagnostic> failures = new ArrayList<>();
        List<DeclaredChange> changes = accepted.declaredChangeSet().changes();
        for (int index = 0; index < changes.size(); index++) {
            DeclaredChange change = changes.get(index);
            NodeType before = nodeType(change.beforeState());
            NodeType after = nodeType(change.afterState());
            boolean relevant;
            switch (change.kind()) {
                case ADDED -> relevant = after == NodeType.BUSINESS_RULE;
                case REMOVED -> relevant = before == NodeType.BUSINESS_RULE;
                case MODIFIED -> {
                    if ((before == NodeType.BUSINESS_RULE) != (after == NodeType.BUSINESS_RULE)) {
                        failures.add(diagnostic("BUSINESS_RULE_TYPE_TRANSITION", index, change));
                    }
                    relevant = before == NodeType.BUSINESS_RULE && after == NodeType.BUSINESS_RULE;
                }
                default -> throw new IllegalStateException("unsupported accepted change kind");
            }
            if (relevant) {
                if (change.category() != ArtifactCategory.NODE) {
                    failures.add(diagnostic("BUSINESS_RULE_CATEGORY_MISMATCH", index, change));
                } else {
                    supported.add(new Supported(index, change));
                }
            }
        }
        if (!failures.isEmpty()) return new Extraction(List.of(), Optional.of(
                new ImpactAnalysisFailure(FailureCode.INCOMPATIBLE_CHANGE_DOMAIN, failures)));
        return new Extraction(List.copyOf(supported), Optional.empty());
    }

    Optional<ImpactAnalysisFailure> validateCompatibility(VerifiedChangeSet accepted,
            List<Supported> supported, FrozenEvidenceManifest manifest) {
        Map<CanonicalIdentity, List<ArtifactIdentityAssertion>> resolved = new HashMap<>();
        for (ArtifactIdentityAssertion assertion : manifest.identityAssertions()) {
            if (assertion.resolution() instanceof ResolvedIdentity value) {
                resolved.computeIfAbsent(value.identity(), ignored -> new ArrayList<>()).add(assertion);
            }
        }
        Set<Integer> supportedIndexes = new HashSet<>();
        List<AnalysisDiagnostic> failures = new ArrayList<>();
        for (Supported candidate : supported) {
            supportedIndexes.add(candidate.declarationIndex());
            List<ArtifactIdentityAssertion> matches = resolved.getOrDefault(
                    candidate.declaration().identity(), List.of());
            if (matches.size() != 1) {
                failures.add(diagnostic("SUPPORTED_CHANGE_ASSERTION_COUNT",
                        candidate.declarationIndex(), candidate.declaration()));
            } else if (matches.getFirst().nodeType() != NodeType.BUSINESS_RULE) {
                failures.add(diagnostic("SUPPORTED_CHANGE_TYPE_MISMATCH",
                        candidate.declarationIndex(), candidate.declaration()));
            }
        }
        List<DeclaredChange> changes = accepted.declaredChangeSet().changes();
        for (int index = 0; index < changes.size(); index++) {
            if (supportedIndexes.contains(index)) continue;
            DeclaredChange change = changes.get(index);
            boolean collision = resolved.getOrDefault(change.identity(), List.of()).stream()
                    .anyMatch(a -> a.nodeType() == NodeType.BUSINESS_RULE);
            if (collision) failures.add(diagnostic("UNSUPPORTED_CHANGE_IDENTITY_COLLISION", index, change));
        }
        if (failures.isEmpty()) return Optional.empty();
        return Optional.of(new ImpactAnalysisFailure(FailureCode.CHANGE_MANIFEST_MISMATCH, failures));
    }

    private NodeType nodeType(Optional<ArtifactState> state) {
        if (state.isEmpty() || !(state.get() instanceof NodeArtifactState node)) return null;
        return NodeType.from(node.snapshot().path("type").asText(null));
    }

    private AnalysisDiagnostic diagnostic(String code, int index, DeclaredChange change) {
        return new AnalysisDiagnostic(code, "accepted change is incompatible with impact-evidence BUSINESS_RULE domain",
                String.format(Locale.ROOT, "%08d:%s", index, change.identity().value()));
    }
}
