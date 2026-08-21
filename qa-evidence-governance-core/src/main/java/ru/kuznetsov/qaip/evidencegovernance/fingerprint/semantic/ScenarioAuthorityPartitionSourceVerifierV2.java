package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Sole authoritative verification boundary for a Logical Source V2 partition source. */
public final class ScenarioAuthorityPartitionSourceVerifierV2 {
    private ScenarioAuthorityPartitionSourceVerifierV2() {}

    public static VerifiedScenarioAuthorityPartitionSourceV2 verify(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput derivationReport,
            List<AttributedMemberOutcomeComposerV1.Composition> attributedMemberOutcomes,
            List<ManifestSemanticCompositionOutcomeV1> manifestOutcomes,
            List<VerifiedScenarioIdentityGroupV1> scenarioIdentityGroups
    ) {
        Objects.requireNonNull(capture, "capture");
        Objects.requireNonNull(derivationReport, "derivationReport");
        attributedMemberOutcomes = List.copyOf(Objects.requireNonNull(
                attributedMemberOutcomes, "attributedMemberOutcomes"));
        manifestOutcomes = List.copyOf(Objects.requireNonNull(manifestOutcomes, "manifestOutcomes"));
        scenarioIdentityGroups = List.copyOf(Objects.requireNonNull(
                scenarioIdentityGroups, "scenarioIdentityGroups"));

        requireReportRoot(capture, derivationReport); // stage 1: authoritative enumeration
        if (attributedMemberOutcomes.isEmpty()) {
            throw new IllegalArgumentException("an authority partition requires attributed-member evidence");
        }
        String authority = attributedMemberOutcomes.getFirst().input().claimedAuthority();
        requireAttributedMembers(capture, derivationReport, authority, attributedMemberOutcomes); // stage 2
        requireManifests(authority, attributedMemberOutcomes, manifestOutcomes); // stages 3-4
        requireGroups(capture, authority, manifestOutcomes, scenarioIdentityGroups); // stage 7

        return new VerifiedScenarioAuthorityPartitionSourceV2(
                capture, derivationReport, authority, attributedMemberOutcomes,
                manifestOutcomes, scenarioIdentityGroups);
    }

    private static void requireReportRoot(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report
    ) {
        var parent = report.parentCapture();
        if (!capture.sourceId().equals(parent.sourceId())
                || !capture.snapshotId().equals(parent.snapshotId())
                || !capture.contentFingerprint().equals(parent.contentFingerprint())
                || !capture.regularMembers().equals(parent.regularMembers())
                || !capture.unsupportedMatchingEntries().stream().map(entry ->
                        new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                                entry.normalizedRepositoryRelativePath(), entry.entryKind(),
                                entry.stableDiagnosticCode())).toList()
                        .equals(parent.unsupportedMatchingEntries())) {
            throw new IllegalArgumentException("derivation report does not terminate in the exact capture");
        }
    }

    private static void requireAttributedMembers(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report,
            String authority,
            List<AttributedMemberOutcomeComposerV1.Composition> supplied
    ) {
        List<RepositoryDerivationReportFingerprintInput.AttributedMemberRetained> expected = report.memberOutcomes()
                .stream()
                .filter(RepositoryDerivationReportFingerprintInput.AttributedMemberRetained.class::isInstance)
                .map(RepositoryDerivationReportFingerprintInput.AttributedMemberRetained.class::cast)
                .filter(outcome -> outcome.claimedAuthority().equals(authority))
                .toList();
        if (expected.isEmpty() || expected.size() != supplied.size()) {
            throw new IllegalArgumentException("partition attributed-member accounting is incomplete");
        }
        var unique = new HashSet<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference>();
        for (int index = 0; index < expected.size(); index++) {
            var authoritative = expected.get(index);
            var claimed = supplied.get(index);
            var input = claimed.input();
            if (!unique.add(input.parentMemberReference())) {
                throw new IllegalArgumentException("duplicate attributed member");
            }
            if (!capture.regularMembers().contains(input.parentMemberReference())
                    || !authoritative.parentMember().equals(input.parentMemberReference())
                    || !authority.equals(input.claimedAuthority())
                    || !authoritative.attributedMemberOutput().fingerprint().equals(claimed.fingerprint())) {
                throw new IllegalArgumentException("attributed-member proof or order was substituted");
            }
            AttributedMemberOutcomeComposerV1.Composition revalidated =
                    input.structuralAdmissionState()
                            == AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED
                            ? AttributedMemberOutcomeComposerV1.composeRejected(input)
                            : claimed;
            if (!revalidated.fingerprint().equals(claimed.fingerprint())) {
                throw new IllegalArgumentException("attributed-member fingerprint was substituted");
            }
        }
    }

    private static void requireManifests(
            String authority,
            List<AttributedMemberOutcomeComposerV1.Composition> members,
            List<ManifestSemanticCompositionOutcomeV1> supplied
    ) {
        List<AttributedMemberOutcomeComposerV1.Composition> admitted = members.stream()
                .filter(composition -> composition.input().structuralAdmissionState()
                        == AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED)
                .toList();
        if (admitted.size() != supplied.size()) {
            throw new IllegalArgumentException("admitted Manifest outcome accounting is incomplete");
        }
        for (int index = 0; index < admitted.size(); index++) {
            var member = admitted.get(index);
            var outcome = supplied.get(index);
            var revalidated = AttributedMemberOutcomeComposerV1.composeAdmitted(outcome);
            if (!authority.equals(outcome.manifest().claimedAuthority())
                    || !member.input().equals(revalidated.input())
                    || !member.fingerprint().equals(revalidated.fingerprint())) {
                throw new IllegalArgumentException("Manifest outcome disagrees with attributed-member outcome");
            }
        }
    }

    private static void requireGroups(
            RepositoryCaptureAttestation capture,
            String authority,
            List<ManifestSemanticCompositionOutcomeV1> manifests,
            List<VerifiedScenarioIdentityGroupV1> groups
    ) {
        List<ScenarioOccurrenceCompositionOutcomeV1> expected = manifests.stream()
                .flatMap(outcome -> outcome.childOutcomes().stream()).toList();
        boolean[] found = new boolean[expected.size()];
        ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity previous = null;
        for (var group : groups) {
            ScenarioIdentityGroupComposerV1.canonicalBytes(group);
            if (!authority.equals(group.claimedIdentity().authority())
                    || previous != null && compareIdentity(previous, group.claimedIdentity()) >= 0) {
                throw new IllegalArgumentException("Scenario groups have foreign authority or noncanonical order");
            }
            previous = group.claimedIdentity();
            for (var occurrence : group.occurrences()) {
                if (!sameCapture(capture, occurrence.occurrence().repositoryCaptureAttestation())) {
                    throw new IllegalArgumentException("Scenario occurrence belongs to a foreign capture");
                }
                int match = -1;
                for (int index = 0; index < expected.size(); index++) {
                    if (sameOutcome(expected.get(index), occurrence)) {
                        if (match >= 0) throw new IllegalArgumentException("ambiguous Scenario occurrence");
                        match = index;
                    }
                }
                if (match < 0 || found[match]) {
                    throw new IllegalArgumentException("extra, foreign, or duplicate Scenario occurrence");
                }
                found[match] = true;
            }
        }
        for (boolean present : found) {
            if (!present) throw new IllegalArgumentException("Scenario occurrence is missing from groups");
        }
    }

    private static boolean sameOutcome(
            ScenarioOccurrenceCompositionOutcomeV1 expected,
            ScenarioOccurrenceCompositionOutcomeV1 supplied
    ) {
        if (!sameOccurrence(expected.occurrence(), supplied.occurrence())) return false;
        if (expected instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed left
                && supplied instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed right) {
            return left.fingerprint().equals(right.fingerprint());
        }
        return expected instanceof ScenarioOccurrenceCompositionOutcomeV1.Unavailable left
                && supplied instanceof ScenarioOccurrenceCompositionOutcomeV1.Unavailable right
                && left.reason() == right.reason();
    }

    private static boolean sameOccurrence(
            NormalizedScenarioOccurrenceInputV1 left,
            NormalizedScenarioOccurrenceInputV1 right
    ) {
        return left.sourceNormalizationVersion().equals(right.sourceNormalizationVersion())
                && left.scenarioSemanticCanonicalizationVersion().equals(right.scenarioSemanticCanonicalizationVersion())
                && left.scenarioSemanticContractVersion().equals(right.scenarioSemanticContractVersion())
                && left.occurrenceIdentity().equals(right.occurrenceIdentity())
                && left.parentMember().equals(right.parentMember())
                && sameCapture(left.repositoryCaptureAttestation(), right.repositoryCaptureAttestation())
                && left.structuralLocation().equals(right.structuralLocation())
                && left.claimedIdentity().equals(right.claimedIdentity())
                && left.exactTitle().equals(right.exactTitle())
                && left.authoredGiven().equals(right.authoredGiven()) && left.givenSteps().equals(right.givenSteps())
                && left.authoredWhen().equals(right.authoredWhen()) && left.whenSteps().equals(right.whenSteps())
                && left.authoredThen().equals(right.authoredThen()) && left.thenSteps().equals(right.thenSteps())
                && left.operationReference().equals(right.operationReference())
                && left.businessRuleReferences().equals(right.businessRuleReferences());
    }

    private static boolean sameCapture(RepositoryCaptureAttestation left, RepositoryCaptureAttestation right) {
        return left.sourceId().equals(right.sourceId())
                && left.snapshotId().equals(right.snapshotId())
                && left.contentFingerprint().equals(right.contentFingerprint())
                && left.fingerprintInput().equals(right.fingerprintInput())
                && left.regularMembers().equals(right.regularMembers())
                && left.unsupportedMatchingEntries().equals(right.unsupportedMatchingEntries());
    }

    private static int compareIdentity(
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity left,
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity right
    ) {
        int value = compareCodePoints(left.authority(), right.authority());
        if (value == 0) value = compareCodePoints(left.scenarioKey(), right.scenarioKey());
        if (value == 0) value = compareCodePoints(left.identitySchemeVersion(), right.identitySchemeVersion());
        return value;
    }

    private static int compareCodePoints(String left, String right) {
        var l = left.codePoints().iterator();
        var r = right.codePoints().iterator();
        while (l.hasNext() && r.hasNext()) {
            int value = Integer.compare(l.nextInt(), r.nextInt());
            if (value != 0) return value;
        }
        return l.hasNext() ? 1 : r.hasNext() ? -1 : 0;
    }
}
