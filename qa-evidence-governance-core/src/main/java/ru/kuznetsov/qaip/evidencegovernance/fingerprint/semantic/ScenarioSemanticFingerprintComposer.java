package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ScenarioSemanticCompositionException.Code;

/** Authoritative ADR-015 anti-substitution boundary for Scenario semantic composition. */
public final class ScenarioSemanticFingerprintComposer {
    private ScenarioSemanticFingerprintComposer() {
    }

    public static ScenarioSemanticCompositionResult compose(ScenarioSemanticCompositionRequest request) {
        Objects.requireNonNull(request, "request");
        requireSupportedContracts(request);
        validateSteps(request.parentIdentity(), request.givenSteps(), StepSemanticFingerprintInput.Phase.GIVEN);
        validateSteps(request.parentIdentity(), request.whenSteps(), StepSemanticFingerprintInput.Phase.WHEN);
        validateSteps(request.parentIdentity(), request.thenSteps(), StepSemanticFingerprintInput.Phase.THEN);
        validateOperation(request.parentIdentity(), request.operationReference());
        validateRules(request.parentIdentity(), request.businessRuleReferences());

        ScenarioSemanticFingerprintInput accepted = new ScenarioSemanticFingerprintInput(
                request.parentIdentity().authority(), request.parentIdentity().scenarioKey(),
                request.parentIdentity().identitySchemeVersion(), request.exactTitle(),
                fingerprints(request.givenSteps()), fingerprints(request.whenSteps()),
                fingerprints(request.thenSteps()), request.operationReference().fingerprint(),
                ruleFingerprints(request.businessRuleReferences()),
                request.scenarioSemanticCanonicalizationVersion(), request.sourceNormalizationVersion(),
                request.scenarioSemanticContractVersion());
        return new ScenarioSemanticCompositionResult(
                accepted, ScenarioSemanticFingerprintEncoder.fingerprint(accepted));
    }

    private static void requireSupportedContracts(ScenarioSemanticCompositionRequest request) {
        if (!ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION.equals(
                request.parentIdentity().identitySchemeVersion())
                || !ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER.equals(
                request.scenarioSemanticCanonicalizationVersion())
                || !ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION.equals(
                request.sourceNormalizationVersion())
                || !ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION.equals(
                request.scenarioSemanticContractVersion())) {
            fail(Code.UNSUPPORTED_CONTRACT);
        }
    }

    private static void validateSteps(
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity parent,
            List<FingerprintStepAttestation> steps,
            StepSemanticFingerprintInput.Phase expectedPhase
    ) {
        for (int index = 0; index < steps.size(); index++) {
            FingerprintStepAttestation attestation = steps.get(index);
            if (!attestation.isAuthoritativelyBound()) fail(Code.STEP_ATTESTATION_MISMATCH);
            StepSemanticFingerprintInput input = attestation.input();
            if (!sameIdentity(parent, input.claimedScenarioAuthority(), input.scenarioKey(),
                    input.scenarioIdentitySchemeVersion())) {
                fail(Code.STEP_SCENARIO_IDENTITY_MISMATCH);
            }
            if (input.phase() != expectedPhase) fail(Code.STEP_PHASE_MISMATCH);
            if (!input.ordinalWithinPhase().equals(BigInteger.valueOf(index))) {
                fail(Code.STEP_ORDINAL_MISMATCH);
            }
        }
    }

    private static void validateOperation(
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity parent,
            FingerprintOperationReferenceAttestation attestation
    ) {
        if (!attestation.isAuthoritativelyBound()) fail(Code.OPERATION_REFERENCE_ATTESTATION_MISMATCH);
        HttpOperationReferenceSemanticFingerprintInput input = attestation.input();
        if (!sameIdentity(parent, input.claimedScenarioAuthority(), input.scenarioKey(),
                input.scenarioIdentitySchemeVersion())) {
            fail(Code.OPERATION_REFERENCE_SCENARIO_IDENTITY_MISMATCH);
        }
    }

    private static void validateRules(
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity parent,
            List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference> rules
    ) {
        for (int index = 0; index < rules.size(); index++) {
            ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference positioned = rules.get(index);
            if (!positioned.authoredPosition().equals(BigInteger.valueOf(index))) {
                fail(Code.BUSINESS_RULE_REFERENCE_POSITION_MISMATCH);
            }
            FingerprintBusinessRuleReferenceAttestation attestation = positioned.attestation();
            if (!attestation.isAuthoritativelyBound()) {
                fail(Code.BUSINESS_RULE_REFERENCE_ATTESTATION_MISMATCH);
            }
            BusinessRuleReferenceSemanticFingerprintInput input = attestation.input();
            if (!sameIdentity(parent, input.claimedScenarioAuthority(), input.scenarioKey(),
                    input.scenarioIdentitySchemeVersion())) {
                fail(Code.BUSINESS_RULE_REFERENCE_SCENARIO_IDENTITY_MISMATCH);
            }
        }
    }

    private static boolean sameIdentity(
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity parent,
            String authority,
            String scenarioKey,
            String identityVersion
    ) {
        return parent.authority().equals(authority)
                && parent.scenarioKey().equals(scenarioKey)
                && parent.identitySchemeVersion().equals(identityVersion);
    }

    private static List<StepSemanticFingerprint> fingerprints(List<FingerprintStepAttestation> attestations) {
        List<StepSemanticFingerprint> values = new ArrayList<>(attestations.size());
        attestations.forEach(attestation -> values.add(attestation.fingerprint()));
        return List.copyOf(values);
    }

    private static List<BusinessRuleReferenceSemanticFingerprint> ruleFingerprints(
            List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference> positioned
    ) {
        List<BusinessRuleReferenceSemanticFingerprint> values = new ArrayList<>(positioned.size());
        positioned.forEach(value -> values.add(value.attestation().fingerprint()));
        return List.copyOf(values);
    }

    private static void fail(Code code) {
        throw new ScenarioSemanticCompositionException(code);
    }
}
