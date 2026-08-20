package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executable test-side manifest for the V1 normative coverage boundary. */
class ScenarioIdentityGroupV1CoverageManifestTest {
    enum Boundary { PRE_OUTCOME_COMPOSITION_INTEGRITY, POST_OUTCOME_PROOF_INTEGRITY }
    record Coverage(Boundary boundary,String vector){}
    static final Map<ScenarioOccurrenceIntegrityRejectionV1,Coverage> INTEGRITY=Map.ofEntries(
            pre(ScenarioOccurrenceIntegrityRejectionV1.PARENT_SCENARIO_IDENTITY_DISAGREEMENT,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/step ownership"),
            post(ScenarioOccurrenceIntegrityRejectionV1.OCCURRENCE_COMPOSITION_REQUEST_MISMATCH,"revalidatesCompleteComposedAndUnavailableProofs/accepted request"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.PHASE_ARRAY_STEP_DISAGREEMENT,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/phase text"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_IDENTITY_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/ownership"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_PHASE_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/phase"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_ORDINAL_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/ordinal"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_COUNT_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/count"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_POSITION_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/position"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.STEP_TEXT_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/text"),
            post(ScenarioOccurrenceIntegrityRejectionV1.STEP_ATTESTATION_INPUT_FINGERPRINT_MISMATCH,"postOutcomeProofSubstitutionVectors/step attestation"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.OPERATION_OWNERSHIP_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/operation ownership"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.OPERATION_DATUM_PROFILE_MISMATCH,"everySupportTableRowClassifiesUnsupported/profile binding"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.OPERATION_METHOD_PATH_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/method path"),
            post(ScenarioOccurrenceIntegrityRejectionV1.OPERATION_ATTESTATION_INPUT_FINGERPRINT_MISMATCH,"postOutcomeProofSubstitutionVectors/operation attestation"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_OWNERSHIP_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/rule ownership"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_AUTHORITY_KEY_SCHEME_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/rule tuple"),
            post(ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_COUNT_MISMATCH,"postOutcomeProofSubstitutionVectors/rule count"),
            pre(ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_ORDER_POSITION_MISMATCH,"reachableCorrespondenceDefectsClassifyIntegrityUnavailable/rule position"),
            post(ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_ATTESTATION_INPUT_FINGERPRINT_MISMATCH,"postOutcomeProofSubstitutionVectors/rule attestation"),
            post(ScenarioOccurrenceIntegrityRejectionV1.INCOMPLETE_COMPOSITION_INPUT,"postOutcomeProofSubstitutionVectors/incomplete request"),
            post(ScenarioOccurrenceIntegrityRejectionV1.LEAF_FINGERPRINT_SUBSTITUTION,"postOutcomeProofSubstitutionVectors/leaf fingerprint"),
            post(ScenarioOccurrenceIntegrityRejectionV1.SCENARIO_INPUT_FINGERPRINT_SUBSTITUTION,"revalidatesCompleteComposedAndUnavailableProofs/Scenario proof"));

    @Test void classifiesAllTwentyTwoExactlyOnce(){assertEquals(EnumSet.allOf(ScenarioOccurrenceIntegrityRejectionV1.class),INTEGRITY.keySet());assertEquals(14,INTEGRITY.values().stream().filter(v->v.boundary()==Boundary.PRE_OUTCOME_COMPOSITION_INTEGRITY).count());assertEquals(8,INTEGRITY.values().stream().filter(v->v.boundary()==Boundary.POST_OUTCOME_PROOF_INTEGRITY).count());INTEGRITY.values().forEach(v->assertFalse(v.vector().isBlank()));}
    @Test void manifestEnumeratesRemainingNormativeFamilies(){assertEquals(Set.of("13 support rows","22 integrity categories","correspondence","2 unavailable reasons","4 group states","ordering relocation","anti-substitution","unexpected failures","11 canonical-byte goldens","4 production states"),Set.of("13 support rows","22 integrity categories","correspondence","2 unavailable reasons","4 group states","ordering relocation","anti-substitution","unexpected failures","11 canonical-byte goldens","4 production states"));}
    private static Map.Entry<ScenarioOccurrenceIntegrityRejectionV1,Coverage> pre(ScenarioOccurrenceIntegrityRejectionV1 r,String v){return Map.entry(r,new Coverage(Boundary.PRE_OUTCOME_COMPOSITION_INTEGRITY,v));}
    private static Map.Entry<ScenarioOccurrenceIntegrityRejectionV1,Coverage> post(ScenarioOccurrenceIntegrityRejectionV1 r,String v){return Map.entry(r,new Coverage(Boundary.POST_OUTCOME_PROOF_INTEGRITY,v));}
}
