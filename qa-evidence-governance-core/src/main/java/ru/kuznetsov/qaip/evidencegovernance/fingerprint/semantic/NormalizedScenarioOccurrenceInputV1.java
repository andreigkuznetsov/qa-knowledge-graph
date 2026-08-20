package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/** Exact, source-native input to the authoritative Scenario occurrence composition attempt. */
public record NormalizedScenarioOccurrenceInputV1(
        String sourceNormalizationVersion,
        String scenarioSemanticCanonicalizationVersion,
        String scenarioSemanticContractVersion,
        ScenarioDeclarationOccurrenceIdentity occurrenceIdentity,
        AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
        RepositoryCaptureAttestation repositoryCaptureAttestation,
        String structuralLocation,
        ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity claimedIdentity,
        String exactTitle,
        List<String> authoredGiven, List<NormalizedStep> givenSteps,
        List<String> authoredWhen, List<NormalizedStep> whenSteps,
        List<String> authoredThen, List<NormalizedStep> thenSteps,
        UnresolvedOperationReference operationReference,
        List<UnresolvedBusinessRuleReference> businessRuleReferences
) {
    public static final String MANIFEST_OCCURRENCE_IDENTITY_VERSION =
            "qaip-scenario-manifest-occurrence-identity-v1";
    public static final String DECLARATION_OCCURRENCE_IDENTITY_VERSION =
            "qaip-scenario-declaration-occurrence-identity-v1";

    public NormalizedScenarioOccurrenceInputV1 {
        Objects.requireNonNull(sourceNormalizationVersion); Objects.requireNonNull(scenarioSemanticCanonicalizationVersion);
        Objects.requireNonNull(scenarioSemanticContractVersion); Objects.requireNonNull(occurrenceIdentity);
        Objects.requireNonNull(parentMember); Objects.requireNonNull(repositoryCaptureAttestation);
        Objects.requireNonNull(structuralLocation); Objects.requireNonNull(claimedIdentity); Objects.requireNonNull(exactTitle);
        authoredGiven = copy(authoredGiven); givenSteps = copy(givenSteps);
        authoredWhen = copy(authoredWhen); whenSteps = copy(whenSteps);
        authoredThen = copy(authoredThen); thenSteps = copy(thenSteps);
        Objects.requireNonNull(operationReference); businessRuleReferences = copy(businessRuleReferences);
    }

    public record ManifestOccurrenceIdentity(String parentSourceId, String parentSnapshotId,
            ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint parentFingerprint,
            String memberPath, String identityVersion) {
        public ManifestOccurrenceIdentity { Objects.requireNonNull(parentSourceId); Objects.requireNonNull(parentSnapshotId);
            Objects.requireNonNull(parentFingerprint); Objects.requireNonNull(memberPath); Objects.requireNonNull(identityVersion); }
    }
    public record ScenarioDeclarationOccurrenceIdentity(ManifestOccurrenceIdentity manifest,
            String structuralPath, String identityVersion) {
        public ScenarioDeclarationOccurrenceIdentity { Objects.requireNonNull(manifest); Objects.requireNonNull(structuralPath); Objects.requireNonNull(identityVersion); }
    }
    public record NormalizedStep(ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity claimedIdentity,
            StepSemanticFingerprintInput.Phase phase, BigInteger ordinal, String identityVersion,
            String exactAuthoredText, String semanticCanonicalizationVersion) {
        public NormalizedStep { Objects.requireNonNull(claimedIdentity); Objects.requireNonNull(phase); Objects.requireNonNull(ordinal);
            Objects.requireNonNull(identityVersion); Objects.requireNonNull(exactAuthoredText); Objects.requireNonNull(semanticCanonicalizationVersion); }
    }
    public record UnresolvedOperationReference(ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity claimedIdentity,
            String role, String datumIdentityVersion, String targetProfile, String method, String path,
            String semanticCanonicalizationVersion) {
        public UnresolvedOperationReference { Objects.requireNonNull(claimedIdentity); Objects.requireNonNull(role); Objects.requireNonNull(datumIdentityVersion);
            Objects.requireNonNull(targetProfile); Objects.requireNonNull(method); Objects.requireNonNull(path); Objects.requireNonNull(semanticCanonicalizationVersion); }
    }
    public record UnresolvedBusinessRuleReference(BigInteger authoredPosition,
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity claimedIdentity, String referencedAuthority,
            String stableRuleKey, String identityScheme, String datumIdentityVersion, String semanticCanonicalizationVersion) {
        public UnresolvedBusinessRuleReference { Objects.requireNonNull(authoredPosition); Objects.requireNonNull(claimedIdentity);
            Objects.requireNonNull(referencedAuthority); Objects.requireNonNull(stableRuleKey); Objects.requireNonNull(identityScheme);
            Objects.requireNonNull(datumIdentityVersion); Objects.requireNonNull(semanticCanonicalizationVersion); }
    }
    private static <T> List<T> copy(List<T> values) { return List.copyOf(Objects.requireNonNull(values)); }
}
