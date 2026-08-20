package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.ArrayList; import java.util.List; import java.util.Objects;
/** Sole authoritative Manifest semantic composition primitive. */
public final class ManifestSemanticFingerprintComposerV1 {
    private ManifestSemanticFingerprintComposerV1(){}
    public static ManifestSemanticFingerprint composeManifestSemanticFingerprintV1(VerifiedAdmittedManifestV1 manifest,
            List<ScenarioOccurrenceCompositionOutcomeV1.Composed> orderedChildren){
        Objects.requireNonNull(manifest,"manifest");var children=List.copyOf(Objects.requireNonNull(orderedChildren,"orderedChildren"));
        if(children.size()!=manifest.authoredScenarios().size())fail(ManifestCompositionRejectionV1.Code.SCENARIO_COUNT_MISMATCH);
        var fps=new ArrayList<ScenarioSemanticFingerprint>(children.size());
        for(int i=0;i<children.size();i++){
            var child=children.get(i);var expected=manifest.authoredScenarios().get(i);
            if(!child.occurrence().equals(expected))fail(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION);
            fps.add(child.fingerprint());
        }
        return ManifestSemanticFingerprintEncoder.fingerprint(input(manifest,fps));
    }
    static ManifestSemanticFingerprintInput input(VerifiedAdmittedManifestV1 manifest,List<ScenarioSemanticFingerprint> fps){
        String normalization=manifest.authoredScenarios().isEmpty()?ManifestSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION:
                manifest.authoredScenarios().getFirst().sourceNormalizationVersion();
        return new ManifestSemanticFingerprintInput(ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER,manifest.claimedAuthority(),
                manifest.format(),manifest.schemaVersion(),manifest.scenarioIdentityScheme(),normalization,fps);
    }
    private static void fail(ManifestCompositionRejectionV1.Code code){throw new ManifestCompositionRejectionV1(code);}
}
