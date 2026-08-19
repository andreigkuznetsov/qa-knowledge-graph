package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AttributedMemberOutcomeFingerprintInput;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.RepositoryDerivationReportFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.RepositoryDerivationReportFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.RepositoryDerivationReportFingerprintInput;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.SemanticProvenanceAttestation;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.SemanticProvenanceFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.SemanticProvenanceOutputReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps exact Extractor-owned capture outcomes into the Evidence Governance report domain. */
public final class RepositoryDerivationReportFingerprintComposer {
    private RepositoryDerivationReportFingerprintComposer() {
    }

    public static Composition compose(
            ScenarioRepositoryCaptureSnapshotCandidate parent,
            ScenarioSchemaAdmissionResult admission,
            List<AttributedMemberEvidence> attributedEvidence
    ) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(admission, "admission");
        List<AttributedMemberEvidence> evidence = List.copyOf(
                Objects.requireNonNull(attributedEvidence, "attributedEvidence"));
        if (!parent.identity().equals(admission.parentIdentity())) {
            throw new IllegalArgumentException("schema admission belongs to another parent capture");
        }

        Map<ParentCapturedMemberRef, AttributedMemberEvidence> byMember = new HashMap<>();
        for (AttributedMemberEvidence item : evidence) {
            if (byMember.put(item.outcome.parentMemberRef(), item) != null) {
                throw new IllegalArgumentException("duplicate attributed-member evidence");
            }
        }

        List<RepositoryDerivationReportFingerprintInput.MemberOutcome> outcomes = new ArrayList<>();
        List<SemanticProvenanceAttestation> provenance = new ArrayList<>();
        for (ScenarioSchemaAdmissionOutcome outcome : admission.memberOutcomes()) {
            if (outcome instanceof AttributedMemberSchemaAdmissionOutcome attributed) {
                AttributedMemberEvidence item = byMember.remove(attributed.parentMemberRef());
                if (item == null || item.outcome != attributed) {
                    throw new IllegalArgumentException("missing exact attributed-member evidence");
                }
                var parentReference = parentReference(attributed.parentMemberRef());
                outcomes.add(new RepositoryDerivationReportFingerprintInput.AttributedMemberRetained(
                        parentReference, attributed.claimedAuthority(), item.output));
                provenance.add(item.provenance);
            } else if (outcome instanceof UnattributableMemberProcessingOutcome unattributable) {
                outcomes.add(new RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained(
                        parentReference(unattributable.parentMemberRef()),
                        unattributable.parseOutcome().name(),
                        unattributable.attributionOutcome().name(),
                        unattributable.structuralLocation()));
            } else {
                throw new IllegalArgumentException("unsupported schema admission outcome");
            }
        }
        if (!byMember.isEmpty()) throw new IllegalArgumentException("extra attributed-member evidence");
        provenance.sort((left, right) -> Arrays.compareUnsigned(
                SemanticProvenanceFingerprintEncoder.identityBytes(left.input().provenanceIdentity()),
                SemanticProvenanceFingerprintEncoder.identityBytes(right.input().provenanceIdentity())));

        List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> parentMembers =
                parent.members().stream()
                        .map(member -> parentReference(new ParentCapturedMemberRef(
                                parent.sourceId(), parent.snapshotId(), parent.contentFingerprint(),
                                member.repositoryRelativePath(), member.rawByteLength(),
                                member.rawMemberFingerprint())))
                        .toList();
        List<RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry> unsupported =
                parent.unsupportedMatchingEntries().stream()
                        .map(entry -> new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                                entry.repositoryRelativePath(), entry.entryKind(), entry.stableDiagnosticCode()))
                        .toList();
        var reportParent = new RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture(
                parent.sourceId(), parent.snapshotId(), parent.contentFingerprint(), parentMembers, unsupported);
        var input = new RepositoryDerivationReportFingerprintInput(
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION,
                reportParent,
                RepositoryDerivationReportFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                outcomes, unsupported, provenance);
        return new Composition(input, RepositoryDerivationReportFingerprintEncoder.fingerprint(input));
    }

    public record AttributedMemberEvidence(
            AttributedMemberSchemaAdmissionOutcome outcome,
            SemanticProvenanceOutputReference output,
            SemanticProvenanceAttestation provenance
    ) {
        public AttributedMemberEvidence {
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(provenance, "provenance");
            var identity = parentReference(outcome.parentMemberRef());
            if (!identity.equals(output.outputIdentity())
                    || !identity.equals(provenance.input().provenanceIdentity().outputDatumIdentity())
                    || !output.fingerprint().equals(provenance.input().outputReference().fingerprint())) {
                throw new IllegalArgumentException("attributed evidence identity/fingerprints do not agree");
            }
        }
    }

    public record Composition(
            RepositoryDerivationReportFingerprintInput input,
            RepositoryDerivationReportFingerprint fingerprint
    ) {
        public Composition {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(fingerprint, "fingerprint");
        }
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentReference(
            ParentCapturedMemberRef parent
    ) {
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                parent.parentSourceId(), parent.parentSnapshotId(), parent.parentContentFingerprint(),
                parent.normalizedRepositoryRelativePath(), parent.rawByteLength(), parent.rawMemberFingerprint());
    }
}
