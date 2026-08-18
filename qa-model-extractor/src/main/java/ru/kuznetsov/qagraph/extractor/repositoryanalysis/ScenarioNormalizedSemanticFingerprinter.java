package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Maps exact normalized source-native Scenario records through the ADR-015 composition boundary. */
public final class ScenarioNormalizedSemanticFingerprinter {
    public Result fingerprint(NormalizedScenarioDeclarationOccurrence declaration) {
        Objects.requireNonNull(declaration, "declaration");
        try {
            ClaimedScenarioIdentity claimed = declaration.claimedScenarioIdentity();
            var parent = new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(
                    claimed.authority(), claimed.scenarioKey(), claimed.identityScheme());

            List<FingerprintStepAttestation> given = new ArrayList<>();
            List<FingerprintStepAttestation> when = new ArrayList<>();
            List<FingerprintStepAttestation> then = new ArrayList<>();
            for (NormalizedScenarioStep step : declaration.steps()) {
                ScenarioStepIdentity identity = step.identity();
                FingerprintStepAttestation attestation = FingerprintStepAttestation.create(
                        new StepSemanticFingerprintInput(
                                identity.claimedScenarioIdentity().authority(),
                                identity.claimedScenarioIdentity().scenarioKey(),
                                identity.claimedScenarioIdentity().identityScheme(),
                                StepSemanticFingerprintInput.Phase.valueOf(identity.phase().name()),
                                BigInteger.valueOf(identity.ordinal()), identity.identityVersion(),
                                step.exactAuthoredText(), StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
                switch (identity.phase()) {
                    case GIVEN -> given.add(attestation);
                    case WHEN -> when.add(attestation);
                    case THEN -> then.add(attestation);
                }
            }

            NormalizedOperationReferenceDatum operation = declaration.operationReference();
            OperationReferenceDatumIdentity operationIdentity = operation.identity();
            FingerprintOperationReferenceAttestation operationAttestation =
                    FingerprintOperationReferenceAttestation.create(
                            new HttpOperationReferenceSemanticFingerprintInput(
                                    operationIdentity.claimedScenarioIdentity().authority(),
                                    operationIdentity.claimedScenarioIdentity().scenarioKey(),
                                    operationIdentity.claimedScenarioIdentity().identityScheme(),
                                    operationIdentity.role(), operationIdentity.identityVersion(),
                                    operation.targetProfile(), operation.method(), operation.path(),
                                    HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));

            List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference> rules = new ArrayList<>();
            for (NormalizedBusinessRuleReferenceDatum rule : declaration.businessRuleReferences()) {
                BusinessRuleReferenceDatumIdentity identity = rule.identity();
                FingerprintBusinessRuleReferenceAttestation attestation =
                        FingerprintBusinessRuleReferenceAttestation.create(
                                new BusinessRuleReferenceSemanticFingerprintInput(
                                        identity.claimedScenarioIdentity().authority(),
                                        identity.claimedScenarioIdentity().scenarioKey(),
                                        identity.claimedScenarioIdentity().identityScheme(),
                                        identity.referencedAuthority(), identity.stableRuleKey(),
                                        identity.identityScheme(), identity.identityVersion(),
                                        BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
                rules.add(new ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference(
                        BigInteger.valueOf(rule.authoredArrayPosition()), attestation));
            }

            ScenarioSemanticCompositionResult composed = ScenarioSemanticFingerprintComposer.compose(
                    new ScenarioSemanticCompositionRequest(parent, declaration.title(), given, when, then,
                            operationAttestation, rules,
                            ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                            ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                            ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION));
            return new Result(composed.fingerprint(), null);
        } catch (ScenarioSemanticCompositionException exception) {
            return new Result(null, UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE);
        } catch (IllegalArgumentException exception) {
            return new Result(null, UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT);
        }
    }

    public record Result(
            ScenarioSemanticFingerprint fingerprint,
            UnavailableReason unavailableReason
    ) {
        public Result {
            if ((fingerprint == null) == (unavailableReason == null)) {
                throw new IllegalArgumentException(
                        "exactly one of fingerprint or unavailableReason must be present");
            }
        }
    }

    public enum UnavailableReason {
        UNSUPPORTED_SEMANTIC_CONTRACT,
        SCENARIO_COMPOSITION_INTEGRITY_FAILURE
    }
}
