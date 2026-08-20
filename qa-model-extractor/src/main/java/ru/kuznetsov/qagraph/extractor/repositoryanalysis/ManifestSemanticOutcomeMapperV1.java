package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.util.List; import java.util.Objects;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedManifestDatum;

/** Extractor mapping/orchestration only; Evidence Governance derives all authoritative results. */
public final class ManifestSemanticOutcomeMapperV1 {
    public VerifiedAdmittedManifestV1 mapVerifiedManifest(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        Objects.requireNonNull(handoff);Objects.requireNonNull(manifest);
        if(!handoff.normalizationResult().memberOutcomes().contains(manifest))
            throw new IllegalArgumentException("Manifest candidate is not in the complete source handoff");
        return mapVerifiedManifest(handoff,manifest.parentMemberRef());
    }
    /** Candidate-member migration boundary; admission and normalization are re-derived only by Evidence Governance. */
    public VerifiedAdmittedManifestV1 mapVerifiedManifest(ScenarioAuthorityNormalizedProcessingV1 handoff,
            ParentCapturedMemberRef selected){
        Objects.requireNonNull(handoff);Objects.requireNonNull(selected);
        if(handoff.normalizationResult().memberOutcomes().stream().noneMatch(x->x.parentMemberRef().equals(selected)))
            throw new IllegalArgumentException("selected candidate is not in the complete source handoff");
        var attested=handoff.captureAttestation().regularMembers();int position=-1;
        for(int index=0;index<attested.size();index++)if(attested.get(index).normalizedRepositoryRelativePath()
                .equals(selected.normalizedRepositoryRelativePath())){position=index;break;}
        if(position<0)throw new IllegalArgumentException("Manifest candidate does not select a captured member");
        var parent=new AdmittedManifestParentMemberReferenceV1(selected.parentSourceId(),selected.parentSnapshotId(),
                selected.parentContentFingerprint(),selected.normalizedRepositoryRelativePath(),position,
                java.math.BigInteger.valueOf(selected.rawByteLength()),selected.rawMemberFingerprint());
        var request=new AdmittedManifestVerificationRequestV1(handoff.captureAttestation(),parent,
                handoff.exactCapturedBytes(selected),AdmittedManifestVerificationRequestV1.selectedV1Identifiers());
        return new AdmittedManifestVerifierV1().verifyAdmittedManifestV1(request);
    }
    public NormalizedManifestSemanticCompositionInputV1 map(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        var verified=mapVerifiedManifest(handoff,manifest);var children=verified.authoredScenarios().stream()
                .map(ScenarioOccurrenceCompositionAttemptV1::attemptScenarioOccurrenceCompositionV1).toList();
        return NormalizedManifestSemanticCompositionInputV1.selectedV1(verified,children);
    }
    public ManifestSemanticCompositionOutcomeV1 attempt(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        return ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(map(handoff,manifest));
    }
}
