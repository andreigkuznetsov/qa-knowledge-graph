package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;

/** Capture-bound, structurally admitted Manifest proof and sole authored-child enumeration. */
public final class VerifiedAdmittedManifestV1 {
    public static final String VERSION = "scenario-authority-verified-admitted-manifest-v1";

    private final RepositoryCaptureAttestation capture;
    private final AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember;
    private final NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity;
    private final String claimedAuthority;
    private final StructuralAdmissionProofV1 admissionProof;
    private final String format;
    private final String schemaVersion;
    private final String scenarioIdentityScheme;
    private final List<NormalizedScenarioOccurrenceInputV1> authoredScenarios;

    private VerifiedAdmittedManifestV1(RepositoryCaptureAttestation capture,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity,
            String claimedAuthority, StructuralAdmissionProofV1 admissionProof, String format,
            String schemaVersion, String scenarioIdentityScheme,
            List<NormalizedScenarioOccurrenceInputV1> authoredScenarios) {
        this.capture=capture; this.parentMember=parentMember; this.occurrenceIdentity=occurrenceIdentity;
        this.claimedAuthority=claimedAuthority; this.admissionProof=admissionProof; this.format=format;
        this.schemaVersion=schemaVersion; this.scenarioIdentityScheme=scenarioIdentityScheme;
        this.authoredScenarios=authoredScenarios;
    }

    /** Single complete source-processing boundary; no independently supplied enumeration overload exists. */
    public static VerifiedAdmittedManifestV1 fromSourceProcessing(CompleteSourceProcessingEvidenceV1 evidence) {
        Objects.requireNonNull(evidence,"evidence");
        return verified(evidence.capture(),evidence.parentMember(),evidence.occurrenceIdentity(),
                evidence.claimedAuthority(),evidence.admissionProof(),evidence.format(),evidence.schemaVersion(),
                evidence.scenarioIdentityScheme(),evidence.derivedAuthoredScenarios());
    }

    static VerifiedAdmittedManifestV1 verified(RepositoryCaptureAttestation capture,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity,
            String claimedAuthority, StructuralAdmissionProofV1 admissionProof, String format,
            String schemaVersion, String scenarioIdentityScheme,
            List<NormalizedScenarioOccurrenceInputV1> derivedAuthoredScenarios) {
        Objects.requireNonNull(capture,"capture"); Objects.requireNonNull(parentMember,"parentMember");
        Objects.requireNonNull(occurrenceIdentity,"occurrenceIdentity"); requireText(claimedAuthority,"claimedAuthority");
        Objects.requireNonNull(admissionProof,"admissionProof"); requireText(format,"format");
        requireText(schemaVersion,"schemaVersion"); requireText(scenarioIdentityScheme,"scenarioIdentityScheme");
        var children=List.copyOf(Objects.requireNonNull(derivedAuthoredScenarios,"derivedAuthoredScenarios"));
        if(!capture.sourceId().equals(parentMember.parentSourceId())||!capture.snapshotId().equals(parentMember.parentSnapshotId())
                ||!capture.contentFingerprint().equals(parentMember.parentContentFingerprint())||!capture.regularMembers().contains(parentMember))
            throw new IllegalArgumentException("parent member is not in the verified Repository Capture");
        if(!occurrenceIdentity.parentSourceId().equals(parentMember.parentSourceId())
                ||!occurrenceIdentity.parentSnapshotId().equals(parentMember.parentSnapshotId())
                ||!occurrenceIdentity.parentFingerprint().equals(parentMember.parentContentFingerprint())
                ||!occurrenceIdentity.memberPath().equals(parentMember.normalizedRepositoryRelativePath())
                ||!NormalizedScenarioOccurrenceInputV1.MANIFEST_OCCURRENCE_IDENTITY_VERSION.equals(occurrenceIdentity.identityVersion()))
            throw new IllegalArgumentException("Manifest occurrence does not match parent member");
        if(!admissionProof.parentMember().equals(parentMember)||!admissionProof.claimedAuthority().equals(claimedAuthority)
                ||!admissionProof.structurallyAdmitted()) throw new IllegalArgumentException("structural admission proof mismatch");
        for(int i=0;i<children.size();i++){
            var child=children.get(i);
            if(!child.repositoryCaptureAttestation().contentFingerprint().equals(capture.contentFingerprint())
                    ||!child.parentMember().equals(parentMember)||!child.occurrenceIdentity().manifest().equals(occurrenceIdentity)
                    ||!child.claimedIdentity().authority().equals(claimedAuthority)
                    ||!child.structuralLocation().equals("/scenarios/"+i)
                    ||!child.occurrenceIdentity().structuralPath().equals(child.structuralLocation()))
                throw new IllegalArgumentException("derived authored Scenario sequence does not match admitted Manifest");
        }
        return new VerifiedAdmittedManifestV1(capture,parentMember,occurrenceIdentity,claimedAuthority,admissionProof,
                format,schemaVersion,scenarioIdentityScheme,children);
    }

    public RepositoryCaptureAttestation capture(){return capture;}
    public AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember(){return parentMember;}
    public NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity(){return occurrenceIdentity;}
    public String claimedAuthority(){return claimedAuthority;}
    public StructuralAdmissionProofV1 admissionProof(){return admissionProof;}
    public String format(){return format;}
    public String schemaVersion(){return schemaVersion;}
    public String scenarioIdentityScheme(){return scenarioIdentityScheme;}
    public List<NormalizedScenarioOccurrenceInputV1> authoredScenarios(){return authoredScenarios;}

    public record StructuralAdmissionProofV1(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            String claimedAuthority, String parserContractIdentifier, String attributionContractIdentifier,
            String schemaContractIdentifier, boolean structurallyAdmitted) {
        public StructuralAdmissionProofV1 { Objects.requireNonNull(parentMember); requireText(claimedAuthority,"claimedAuthority");
            requireText(parserContractIdentifier,"parserContractIdentifier"); requireText(attributionContractIdentifier,"attributionContractIdentifier");
            requireText(schemaContractIdentifier,"schemaContractIdentifier"); }
    }
    public record CompleteSourceProcessingEvidenceV1(RepositoryCaptureAttestation capture,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity,
            String claimedAuthority, StructuralAdmissionProofV1 admissionProof, String format,
            String schemaVersion, String scenarioIdentityScheme,
            List<NormalizedScenarioOccurrenceInputV1> derivedAuthoredScenarios) {
        public CompleteSourceProcessingEvidenceV1 { Objects.requireNonNull(capture);Objects.requireNonNull(parentMember);
            Objects.requireNonNull(occurrenceIdentity);requireText(claimedAuthority,"claimedAuthority");Objects.requireNonNull(admissionProof);
            requireText(format,"format");requireText(schemaVersion,"schemaVersion");requireText(scenarioIdentityScheme,"scenarioIdentityScheme");
            derivedAuthoredScenarios=List.copyOf(Objects.requireNonNull(derivedAuthoredScenarios)); }
    }
    private static void requireText(String value,String field){if(value==null||value.isEmpty())throw new IllegalArgumentException(field+" must not be empty");}
}
