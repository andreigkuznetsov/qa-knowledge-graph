package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.util.List; import java.util.Objects;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedManifestDatum;

/** Extractor mapping/orchestration only; Evidence Governance derives all authoritative results. */
public final class ManifestSemanticOutcomeMapperV1 {
    public VerifiedAdmittedManifestV1 mapVerifiedManifest(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        Objects.requireNonNull(handoff,"handoff");Objects.requireNonNull(manifest,"manifest");
        if(!handoff.normalizationResult().memberOutcomes().contains(manifest))throw new IllegalArgumentException("Manifest is not in the verified processing handoff");
        var source=manifest.sourceAdmission();var parent=reference(source.parentMemberRef());var mi=manifest.occurrenceIdentity();
        var identity=new NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity(mi.parentSourceId(),mi.parentSnapshotId(),
                mi.parentContentFingerprint(),mi.normalizedRepositoryRelativePath(),mi.identityVersion());
        var scenarioMapper=new ScenarioOccurrenceInputMapperV1();
        List<NormalizedScenarioOccurrenceInputV1> children=handoff.occurrences().stream()
                .filter(b->b.declaration().occurrenceIdentity().manifestOccurrenceIdentity().equals(mi))
                .map(b->scenarioMapper.map(handoff,b)).toList();
        var proof=new VerifiedAdmittedManifestV1.StructuralAdmissionProofV1(parent,source.claimedAuthority(),
                source.parserContractIdentifier(),source.attributionContractIdentifier(),source.schemaContractIdentifier(),
                source.structuralAdmissionState()==AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED);
        return VerifiedAdmittedManifestV1.fromSourceProcessing(new VerifiedAdmittedManifestV1.CompleteSourceProcessingEvidenceV1(
                handoff.captureAttestation(),parent,identity,manifest.claimedAuthority(),proof,manifest.format(),
                manifest.schemaVersion(),manifest.scenarioIdentityScheme(),children));
    }
    public NormalizedManifestSemanticCompositionInputV1 map(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        var verified=mapVerifiedManifest(handoff,manifest);var children=verified.authoredScenarios().stream()
                .map(ScenarioOccurrenceCompositionAttemptV1::attemptScenarioOccurrenceCompositionV1).toList();
        return NormalizedManifestSemanticCompositionInputV1.selectedV1(verified,children);
    }
    public ManifestSemanticCompositionOutcomeV1 attempt(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        return ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(map(handoff,manifest));
    }
    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference reference(ParentCapturedMemberRef p){
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(p.parentSourceId(),p.parentSnapshotId(),p.parentContentFingerprint(),
                p.normalizedRepositoryRelativePath(),p.rawByteLength(),p.rawMemberFingerprint());}
}
