package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/** Immutable untrusted request for ADR-015 Scenario semantic composition. */
public record ScenarioSemanticCompositionRequest(
        ClaimedScenarioIdentity parentIdentity,
        String exactTitle,
        List<FingerprintStepAttestation> givenSteps,
        List<FingerprintStepAttestation> whenSteps,
        List<FingerprintStepAttestation> thenSteps,
        FingerprintOperationReferenceAttestation operationReference,
        List<PositionedBusinessRuleReference> businessRuleReferences,
        String scenarioSemanticCanonicalizationVersion,
        String sourceNormalizationVersion,
        String scenarioSemanticContractVersion
) {
    public ScenarioSemanticCompositionRequest {
        Objects.requireNonNull(parentIdentity, "parentIdentity");
        Objects.requireNonNull(exactTitle, "exactTitle");
        givenSteps = immutable(givenSteps, "givenSteps");
        whenSteps = immutable(whenSteps, "whenSteps");
        thenSteps = immutable(thenSteps, "thenSteps");
        Objects.requireNonNull(operationReference, "operationReference");
        businessRuleReferences = immutable(businessRuleReferences, "businessRuleReferences");
        Objects.requireNonNull(scenarioSemanticCanonicalizationVersion,
                "scenarioSemanticCanonicalizationVersion");
        Objects.requireNonNull(sourceNormalizationVersion, "sourceNormalizationVersion");
        Objects.requireNonNull(scenarioSemanticContractVersion, "scenarioSemanticContractVersion");
    }

    public record ClaimedScenarioIdentity(
            String authority,
            String scenarioKey,
            String identitySchemeVersion
    ) {
        public ClaimedScenarioIdentity {
            Objects.requireNonNull(authority, "authority");
            Objects.requireNonNull(scenarioKey, "scenarioKey");
            Objects.requireNonNull(identitySchemeVersion, "identitySchemeVersion");
        }
    }

    /** Authored position participates only in composition integrity, never the leaf fingerprint. */
    public record PositionedBusinessRuleReference(
            BigInteger authoredPosition,
            FingerprintBusinessRuleReferenceAttestation attestation
    ) {
        public PositionedBusinessRuleReference {
            Objects.requireNonNull(authoredPosition, "authoredPosition");
            Objects.requireNonNull(attestation, "attestation");
        }
    }

    private static <T> List<T> immutable(List<T> values, String field) {
        return List.copyOf(Objects.requireNonNull(values, field));
    }
}
