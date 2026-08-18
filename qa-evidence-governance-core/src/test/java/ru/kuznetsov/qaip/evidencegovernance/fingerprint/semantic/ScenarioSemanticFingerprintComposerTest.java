package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioSemanticFingerprintComposerTest {
    @Test
    void validCompleteCompositionEqualsDirectApprovedEncoderResultAndPreservesOrder() {
        ScenarioSemanticCompositionRequest request = validRequest();

        ScenarioSemanticCompositionResult result = ScenarioSemanticFingerprintComposer.compose(request);

        assertEquals(ScenarioSemanticFingerprintEncoder.fingerprint(result.acceptedInput()),
                result.fingerprint());
        assertEquals(request.givenSteps().stream().map(FingerprintStepAttestation::fingerprint).toList(),
                result.acceptedInput().givenStepFingerprints());
        assertEquals(request.businessRuleReferences().stream()
                        .map(value -> value.attestation().fingerprint()).toList(),
                result.acceptedInput().businessRuleReferenceFingerprints());
    }

    @Test
    void stepFromAnotherScenarioIsRejectedWithoutFingerprint() {
        ScenarioSemanticCompositionRequest base = validRequest();
        ScenarioSemanticCompositionRequest request = replaceSteps(base,
                List.of(step("payments", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 0, "other")),
                base.whenSteps(), base.thenSteps());

        assertFailure(ScenarioSemanticCompositionException.Code.STEP_SCENARIO_IDENTITY_MISMATCH, request);
    }

    @Test
    void wrongStepPhaseIsRejected() {
        ScenarioSemanticCompositionRequest base = validRequest();
        ScenarioSemanticCompositionRequest request = replaceSteps(base,
                List.of(step("orders", "CREATE", StepSemanticFingerprintInput.Phase.WHEN, 0, "wrong phase")),
                base.whenSteps(), base.thenSteps());

        assertFailure(ScenarioSemanticCompositionException.Code.STEP_PHASE_MISMATCH, request);
    }

    @Test
    void missingNoncontiguousDuplicateAndWrongStepOrdinalsAreRejected() {
        ScenarioSemanticCompositionRequest base = validRequest();
        assertFailure(ScenarioSemanticCompositionException.Code.STEP_ORDINAL_MISMATCH,
                replaceSteps(base,
                        List.of(step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 1, "missing zero")),
                        base.whenSteps(), base.thenSteps()));
        assertFailure(ScenarioSemanticCompositionException.Code.STEP_ORDINAL_MISMATCH,
                replaceSteps(base, List.of(
                                step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 0, "first"),
                                step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 2, "gap")),
                        base.whenSteps(), base.thenSteps()));
        assertFailure(ScenarioSemanticCompositionException.Code.STEP_ORDINAL_MISMATCH,
                replaceSteps(base, List.of(
                                step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 0, "first"),
                                step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 0, "duplicate")),
                        base.whenSteps(), base.thenSteps()));
    }

    @Test
    void operationReferenceFromAnotherScenarioIsRejected() {
        ScenarioSemanticCompositionRequest base = validRequest();
        ScenarioSemanticCompositionRequest request = copy(base, base.givenSteps(), base.whenSteps(),
                base.thenSteps(), operation("payments", "CREATE", "POST", "/api/orders"),
                base.businessRuleReferences(), current(), normalization(), current());

        assertFailure(ScenarioSemanticCompositionException.Code.OPERATION_REFERENCE_SCENARIO_IDENTITY_MISMATCH,
                request);
    }

    @Test
    void businessRuleReferenceFromAnotherScenarioIsRejected() {
        ScenarioSemanticCompositionRequest base = validRequest();
        ScenarioSemanticCompositionRequest request = copy(base, base.givenSteps(), base.whenSteps(),
                base.thenSteps(), base.operationReference(),
                List.of(positioned(0, rule("payments", "CREATE", "policy", "order.limit"))),
                current(), normalization(), current());

        assertFailure(ScenarioSemanticCompositionException.Code.BUSINESS_RULE_REFERENCE_SCENARIO_IDENTITY_MISMATCH,
                request);
    }

    @Test
    void businessRulePositionGapDuplicateAndOrderMismatchAreRejected() {
        ScenarioSemanticCompositionRequest base = validRequest();
        assertRulePositionFailure(base, List.of(positioned(1, rule("orders", "CREATE", "policy", "order.limit"))));
        assertRulePositionFailure(base, List.of(
                positioned(0, rule("orders", "CREATE", "policy", "order.limit")),
                positioned(0, rule("orders", "CREATE", "fraud", "order.review"))));
        assertRulePositionFailure(base, List.of(
                positioned(1, rule("orders", "CREATE", "fraud", "order.review")),
                positioned(0, rule("orders", "CREATE", "policy", "order.limit"))));
    }

    @Test
    void attestationsAreFactoryOnlyAndAlwaysMatchAuthoritativeLeafEncoders() {
        assertNoPublicConstructor(FingerprintStepAttestation.class);
        assertNoPublicConstructor(FingerprintOperationReferenceAttestation.class);
        assertNoPublicConstructor(FingerprintBusinessRuleReferenceAttestation.class);

        FingerprintStepAttestation step = step("orders", "CREATE",
                StepSemanticFingerprintInput.Phase.GIVEN, 0, "exact");
        assertEquals(StepSemanticFingerprintEncoder.fingerprint(step.input()), step.fingerprint());
        FingerprintOperationReferenceAttestation operation = operation(
                "orders", "CREATE", "POST", "/api/orders");
        assertEquals(HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(operation.input()),
                operation.fingerprint());
        FingerprintBusinessRuleReferenceAttestation rule = rule(
                "orders", "CREATE", "policy", "order.limit");
        assertEquals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(rule.input()),
                rule.fingerprint());
    }

    @Test
    void unsupportedScenarioV1ContractsAreRejectedBeforeFingerprintConstruction() {
        ScenarioSemanticCompositionRequest base = validRequest();
        assertFailure(ScenarioSemanticCompositionException.Code.UNSUPPORTED_CONTRACT,
                copyWithIdentity(base, new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(
                        "orders", "CREATE", "qaip-scenario-identity-v2")));
        assertFailure(ScenarioSemanticCompositionException.Code.UNSUPPORTED_CONTRACT,
                copy(base, base.givenSteps(), base.whenSteps(), base.thenSteps(), base.operationReference(),
                        base.businessRuleReferences(), "scenario-authority-scenario-semantic-c14n-v2",
                        normalization(), current()));
        assertFailure(ScenarioSemanticCompositionException.Code.UNSUPPORTED_CONTRACT,
                copy(base, base.givenSteps(), base.whenSteps(), base.thenSteps(), base.operationReference(),
                        base.businessRuleReferences(), current(),
                        "scenario-authority-source-normalization-v2", current()));
    }

    @Test
    void requestAndSuccessfulResultAreImmutable() {
        ScenarioSemanticCompositionRequest base = validRequest();
        List<FingerprintStepAttestation> mutable = new ArrayList<>(base.givenSteps());
        ScenarioSemanticCompositionRequest request = copy(base, mutable, base.whenSteps(), base.thenSteps(),
                base.operationReference(), base.businessRuleReferences(), current(), normalization(), current());
        ScenarioSemanticCompositionResult result = ScenarioSemanticFingerprintComposer.compose(request);
        mutable.clear();

        assertEquals(2, request.givenSteps().size());
        assertEquals(2, result.acceptedInput().givenStepFingerprints().size());
        assertThrows(UnsupportedOperationException.class, request.givenSteps()::clear);
        assertThrows(UnsupportedOperationException.class,
                result.acceptedInput().givenStepFingerprints()::clear);
    }

    private static ScenarioSemanticCompositionRequest validRequest() {
        return new ScenarioSemanticCompositionRequest(parent(), "Create order",
                List.of(
                        step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 0, "customer exists"),
                        step("orders", "CREATE", StepSemanticFingerprintInput.Phase.GIVEN, 1, "balance exists")),
                List.of(step("orders", "CREATE", StepSemanticFingerprintInput.Phase.WHEN, 0, "submit order")),
                List.of(step("orders", "CREATE", StepSemanticFingerprintInput.Phase.THEN, 0, "order accepted")),
                operation("orders", "CREATE", "POST", "/api/orders"),
                List.of(
                        positioned(0, rule("orders", "CREATE", "policy", "order.limit")),
                        positioned(1, rule("orders", "CREATE", "fraud", "order.review"))),
                current(), normalization(), current());
    }

    private static ScenarioSemanticCompositionRequest replaceSteps(ScenarioSemanticCompositionRequest base,
                                                                    List<FingerprintStepAttestation> given,
                                                                    List<FingerprintStepAttestation> when,
                                                                    List<FingerprintStepAttestation> then) {
        return copy(base, given, when, then, base.operationReference(), base.businessRuleReferences(),
                current(), normalization(), current());
    }

    private static ScenarioSemanticCompositionRequest copy(
            ScenarioSemanticCompositionRequest base,
            List<FingerprintStepAttestation> given,
            List<FingerprintStepAttestation> when,
            List<FingerprintStepAttestation> then,
            FingerprintOperationReferenceAttestation operation,
            List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference> rules,
            String semanticVersion,
            String normalizationVersion,
            String semanticContractVersion
    ) {
        return new ScenarioSemanticCompositionRequest(base.parentIdentity(), base.exactTitle(), given, when, then,
                operation, rules, semanticVersion, normalizationVersion, semanticContractVersion);
    }

    private static ScenarioSemanticCompositionRequest copyWithIdentity(
            ScenarioSemanticCompositionRequest base,
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity identity
    ) {
        return new ScenarioSemanticCompositionRequest(identity, base.exactTitle(), base.givenSteps(), base.whenSteps(),
                base.thenSteps(), base.operationReference(), base.businessRuleReferences(),
                current(), normalization(), current());
    }

    private static FingerprintStepAttestation step(String authority, String key,
                                                    StepSemanticFingerprintInput.Phase phase,
                                                    long ordinal, String text) {
        return FingerprintStepAttestation.create(new StepSemanticFingerprintInput(authority, key,
                "qaip-scenario-identity-v1", phase, BigInteger.valueOf(ordinal),
                "qaip-scenario-step-identity-v1", text,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static FingerprintOperationReferenceAttestation operation(String authority, String key,
                                                                       String method, String path) {
        return FingerprintOperationReferenceAttestation.create(
                new HttpOperationReferenceSemanticFingerprintInput(authority, key,
                        "qaip-scenario-identity-v1", "OPERATION_REF",
                        "qaip-scenario-operation-reference-datum-identity-v1",
                        "qaip-http-operation-reference-v1", method, path,
                        HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static FingerprintBusinessRuleReferenceAttestation rule(String scenarioAuthority, String scenarioKey,
                                                                     String ruleAuthority, String ruleKey) {
        return FingerprintBusinessRuleReferenceAttestation.create(
                new BusinessRuleReferenceSemanticFingerprintInput(scenarioAuthority, scenarioKey,
                        "qaip-scenario-identity-v1", ruleAuthority, ruleKey,
                        "qaip-business-rule-identity-v1",
                        "qaip-scenario-business-rule-reference-datum-identity-v1",
                        BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
    }

    private static ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference positioned(
            long position, FingerprintBusinessRuleReferenceAttestation attestation) {
        return new ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference(
                BigInteger.valueOf(position), attestation);
    }

    private static ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity parent() {
        return new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(
                "orders", "CREATE", "qaip-scenario-identity-v1");
    }

    private static void assertRulePositionFailure(
            ScenarioSemanticCompositionRequest base,
            List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference> rules
    ) {
        assertFailure(ScenarioSemanticCompositionException.Code.BUSINESS_RULE_REFERENCE_POSITION_MISMATCH,
                copy(base, base.givenSteps(), base.whenSteps(), base.thenSteps(), base.operationReference(), rules,
                        current(), normalization(), current()));
    }

    private static void assertFailure(ScenarioSemanticCompositionException.Code expected,
                                      ScenarioSemanticCompositionRequest request) {
        ScenarioSemanticCompositionException failure = assertThrows(
                ScenarioSemanticCompositionException.class,
                () -> ScenarioSemanticFingerprintComposer.compose(request));
        assertEquals(expected, failure.code());
    }

    private static void assertNoPublicConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            assertFalse(Modifier.isPublic(constructor.getModifiers()));
        }
    }

    private static String current() {
        return ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER;
    }

    private static String normalization() {
        return ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION;
    }
}
