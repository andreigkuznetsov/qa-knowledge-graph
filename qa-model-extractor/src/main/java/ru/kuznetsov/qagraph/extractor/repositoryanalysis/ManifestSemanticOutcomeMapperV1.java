package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.util.List; import java.util.Objects;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedManifestDatum;

/** Extractor mapping/orchestration only; Evidence Governance derives all authoritative results. */
public final class ManifestSemanticOutcomeMapperV1 {
    public VerifiedAdmittedManifestV1 mapVerifiedManifest(ScenarioAuthorityNormalizedProcessingV1 handoff,NormalizedManifestDatum manifest){
        return VerifiedAdmittedManifestSourceBridgeV1.fromCompleteHandoff(handoff,manifest);
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
