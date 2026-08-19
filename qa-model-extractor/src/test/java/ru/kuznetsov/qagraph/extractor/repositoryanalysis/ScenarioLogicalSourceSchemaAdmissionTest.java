package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioLogicalSourceSchemaAdmissionTest {
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1",
                    "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1",
                    "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1",
                    RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    private final ScenarioLogicalSourceMemberProcessor processor = new ScenarioLogicalSourceMemberProcessor();
    private final ScenarioLogicalSourceSchemaAdmission admission = new ScenarioLogicalSourceSchemaAdmission();

    @Test
    void freezesSchemaContractIdentifier() {
        assertEquals("qaip-scenario-authority-manifest-schema-v1",
                ScenarioLogicalSourceSchemaAdmission.SCHEMA_CONTRACT_IDENTIFIER);
    }

    @Test
    void attributedSchemaValidManifestIsStructurallyAdmitted() {
        AttributedMemberSchemaAdmissionOutcome outcome = attributed(admit(
                member("valid", validManifest("orders", "CREATE"))));

        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                outcome.structuralAdmissionState());
        assertEquals(List.of(), outcome.schemaDiagnostics());
        assertEquals("orders", outcome.claimedAuthority());
        assertEquals(ScenarioMemberProcessingOutcome.ParseOutcome.PARSED, outcome.parseOutcome());
        assertEquals(ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED,
                outcome.attributionOutcome());
    }

    @Test
    void attributedSchemaInvalidManifestIsRejectedButRemainsBoundToAuthority() {
        String invalid = validManifest("orders", "CREATE").replace(
                "\"given\":[\"A precondition\"]", "\"given\":[]");
        AttributedMemberSchemaAdmissionOutcome outcome = attributed(admit(member("invalid", invalid)));

        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                outcome.structuralAdmissionState());
        assertEquals("orders", outcome.claimedAuthority());
        assertTrue(outcome.schemaDiagnostics().stream()
                .anyMatch(value -> value.normativeSchemaKeyword().equals("minItems")));
        assertEquals(".qaip/scenarios/invalid.scenario.json",
                outcome.parentMemberRef().normalizedRepositoryRelativePath());
    }

    @Test
    void unattributableMemberBypassesSchemaValidationAndPassesThroughUnchanged() {
        AtomicInteger validations = new AtomicInteger();
        ScenarioLogicalSourceSchemaAdmission counting = new ScenarioLogicalSourceSchemaAdmission(document -> {
            validations.incrementAndGet();
            return List.of();
        });
        ScenarioLogicalSourceProcessingResult processing = process(member("broken", "not-json"));
        UnattributableMemberProcessingOutcome before = assertInstanceOf(
                UnattributableMemberProcessingOutcome.class, processing.memberOutcomes().getFirst());

        ScenarioSchemaAdmissionResult result = counting.admit(processing);

        assertSame(before, result.memberOutcomes().getFirst());
        assertEquals(0, validations.get());
    }

    @Test
    void mixedMembersRetainValidRejectedAndUnattributableOutcomesInParentOrder() {
        String rejected = validManifest("payments", "PAY").replace(
                "\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"2.0\"");
        ScenarioSchemaAdmissionResult result = admit(
                member("a-valid", validManifest("orders", "CREATE")),
                member("b-rejected", rejected),
                member("c-unattributable", "not-json"));

        assertEquals(List.of(
                        ".qaip/scenarios/a-valid.scenario.json",
                        ".qaip/scenarios/b-rejected.scenario.json",
                        ".qaip/scenarios/c-unattributable.scenario.json"),
                result.memberOutcomes().stream()
                        .map(value -> value.parentMemberRef().normalizedRepositoryRelativePath())
                        .toList());
        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                ((AttributedMemberSchemaAdmissionOutcome) result.memberOutcomes().get(0)).structuralAdmissionState());
        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                ((AttributedMemberSchemaAdmissionOutcome) result.memberOutcomes().get(1)).structuralAdmissionState());
        assertInstanceOf(UnattributableMemberProcessingOutcome.class, result.memberOutcomes().get(2));
    }

    @Test
    void oneMemberRetainsMultipleDeterministicallyOrderedDiagnostics() {
        String invalid = validManifest("orders", "CREATE")
                .replace("qaip-scenario-authority-manifest-v1", "wrong-format")
                .replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"2.0\"");

        AttributedMemberSchemaAdmissionOutcome first = attributed(admit(member("invalid", invalid)));
        AttributedMemberSchemaAdmissionOutcome second = attributed(admit(member("invalid", invalid)));

        assertEquals(2, first.schemaDiagnostics().size());
        assertEquals(first.schemaDiagnostics(), second.schemaDiagnostics());
        assertEquals(first.schemaDiagnostics().stream()
                        .map(value -> value.instanceLocation() + '|' + value.normativeSchemaKeyword()
                                + '|' + value.schemaRuleIdentifier() + '|' + value.typedParameters())
                        .sorted()
                        .toList(),
                first.schemaDiagnostics().stream()
                        .map(value -> value.instanceLocation() + '|' + value.normativeSchemaKeyword()
                                + '|' + value.schemaRuleIdentifier() + '|' + value.typedParameters())
                        .toList());
        assertTrue(first.schemaDiagnostics().stream()
                .allMatch(value -> value.stableCode() == ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION));
        assertTrue(first.schemaDiagnostics().stream()
                .allMatch(value -> value.instanceLocation().isEmpty()
                        || value.instanceLocation().startsWith("/")));
    }

    @Test
    void rejectionDoesNotSuppressLaterAttributedMember() {
        String rejected = validManifest("orders", "BAD").replace("\"then\":[\"An outcome\"]", "\"then\":[]");
        ScenarioSchemaAdmissionResult result = admit(
                member("a-rejected", rejected),
                member("b-valid", validManifest("orders", "GOOD")));

        assertEquals(2, result.memberOutcomes().size());
        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                ((AttributedMemberSchemaAdmissionOutcome) result.memberOutcomes().get(0)).structuralAdmissionState());
        assertEquals(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                ((AttributedMemberSchemaAdmissionOutcome) result.memberOutcomes().get(1)).structuralAdmissionState());
    }

    @Test
    void unsupportedDiagnosticMappingFailsOneMemberWithoutSuppressingLaterMembers() {
        AtomicInteger validations = new AtomicInteger();
        ScenarioLogicalSourceSchemaAdmission failFirst = new ScenarioLogicalSourceSchemaAdmission(document -> {
            if (validations.getAndIncrement() == 0) {
                throw new ScenarioSchemaDiagnosticMappingException("unknown validator rule");
            }
            return List.of();
        });
        ScenarioLogicalSourceProcessingResult processing = process(
                member("a-failure", validManifest("orders", "ONE")),
                member("b-valid", validManifest("orders", "TWO")));

        ScenarioSchemaAdmissionResult result = failFirst.admit(processing);

        AttributedMemberSchemaAdmissionFailure failure = assertInstanceOf(
                AttributedMemberSchemaAdmissionFailure.class, result.memberOutcomes().get(0));
        assertEquals("UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING", failure.failureCode());
        assertEquals(ScenarioSchemaDiagnosticAdapterV1.CONTRACT_IDENTIFIER,
                failure.mappingContractIdentifier());
        assertEquals(processing.memberOutcomes().get(0).parentMemberRef(), failure.parentMemberRef());
        assertInstanceOf(AttributedMemberSchemaAdmissionOutcome.class, result.memberOutcomes().get(1));

        ScenarioSourceNormalizationResult normalized = new ScenarioSourceDeclarationNormalizer().normalize(result);
        assertSame(failure, normalized.memberOutcomes().get(0));
    }

    @Test
    void admissionPreservesExactParentBindingAndProcessingContracts() {
        ScenarioLogicalSourceProcessingResult processing = process(member("bound", validManifest("orders", "ONE")));
        AttributedMemberProcessingOutcome before = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, processing.memberOutcomes().getFirst());

        AttributedMemberSchemaAdmissionOutcome after = attributed(admission.admit(processing));

        assertEquals(before.parentMemberRef(), after.parentMemberRef());
        assertEquals(before.claimedAuthority(), after.claimedAuthority());
        assertEquals(before.parserContractIdentifier(), after.parserContractIdentifier());
        assertEquals(before.attributionContractIdentifier(), after.attributionContractIdentifier());
        assertEquals(before.parseOutcome(), after.parseOutcome());
        assertEquals(before.attributionOutcome(), after.attributionOutcome());
        assertEquals(before.structuralLocation(), after.attributionStructuralLocation());
        assertEquals(processing.parentIdentity(), admission.admit(processing).parentIdentity());
    }

    @Test
    void resultDiagnosticsAndParsedSourceAreImmutable() {
        String invalid = validManifest("orders", "ONE").replace("\"ruleRefs\":[]", "\"unknown\":true");
        ScenarioSchemaAdmissionResult result = admit(member("immutable", invalid));
        AttributedMemberSchemaAdmissionOutcome outcome = attributed(result);
        ObjectNode exposed = (ObjectNode) outcome.parsedSource();
        exposed.put("authority", "changed");

        assertEquals("orders", outcome.parsedSource().path("authority").asText());
        assertThrows(UnsupportedOperationException.class, () -> result.memberOutcomes().clear());
        assertThrows(UnsupportedOperationException.class, () -> outcome.schemaDiagnostics().clear());
    }

    @Test
    void structuralStateCannotDisagreeWithDiagnostics() {
        ScenarioLogicalSourceProcessingResult processing = process(member("one", "{\"authority\":\"orders\"}"));
        AttributedMemberProcessingOutcome before = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, processing.memberOutcomes().getFirst());

        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberSchemaAdmissionOutcome(
                before.parentMemberRef(),
                before.claimedAuthority(),
                before.parserContractIdentifier(),
                before.attributionContractIdentifier(),
                before.parseOutcome(),
                before.attributionOutcome(),
                before.structuralLocation(),
                ScenarioLogicalSourceSchemaAdmission.SCHEMA_CONTRACT_IDENTIFIER,
                AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(ScenarioSchemaDiagnostic.v1(
                        "",
                        "required",
                        "qaip-scenario-authority-manifest-schema-v1#/required",
                        List.of(new ScenarioSchemaDiagnostic.TextParameter(
                                "missingProperty", "authority")))),
                before.parsedSource()));
    }

    @Test
    void canonicalAdmissionAcceptsOnlyEvidenceGovernanceDiagnosticsNotLegacyPresentationData()
            throws Exception {
        ParameterizedType diagnosticsType = (ParameterizedType)
                Arrays.stream(AttributedMemberSchemaAdmissionOutcome.class.getRecordComponents())
                        .filter(component -> component.getName().equals("schemaDiagnostics"))
                        .findFirst()
                        .orElseThrow()
                        .getGenericType();

        assertEquals(ScenarioSchemaDiagnostic.class, diagnosticsType.getActualTypeArguments()[0]);
        assertTrue(!ScenarioSchemaDiagnostic.class.isAssignableFrom(
                ScenarioManifestSchemaValidationResult.Diagnostic.class));
    }

    @Test
    void canonicalAdmissionCollectionRejectsDuplicateTuplesSurvivingAdapterConsolidation() {
        ScenarioLogicalSourceProcessingResult processing = process(
                member("one", validManifest("orders", "ONE")));
        AttributedMemberProcessingOutcome before = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, processing.memberOutcomes().getFirst());
        ScenarioSchemaDiagnostic duplicate = ScenarioSchemaDiagnostic.v1(
                "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));

        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberSchemaAdmissionOutcome(
                before.parentMemberRef(), before.claimedAuthority(), before.parserContractIdentifier(),
                before.attributionContractIdentifier(), before.parseOutcome(), before.attributionOutcome(),
                before.structuralLocation(), ScenarioLogicalSourceSchemaAdmission.SCHEMA_CONTRACT_IDENTIFIER,
                AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(duplicate, duplicate), before.parsedSource()));
    }

    private ScenarioSchemaAdmissionResult admit(
            ScenarioManifestStableCaptureResult.CapturedMember... members
    ) {
        return admission.admit(process(members));
    }

    private ScenarioLogicalSourceProcessingResult process(
            ScenarioManifestStableCaptureResult.CapturedMember... members
    ) {
        return processor.process(candidate(members));
    }

    private static AttributedMemberSchemaAdmissionOutcome attributed(ScenarioSchemaAdmissionResult result) {
        assertEquals(1, result.memberOutcomes().size());
        return assertInstanceOf(AttributedMemberSchemaAdmissionOutcome.class,
                result.memberOutcomes().getFirst());
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult.CapturedMember... members
    ) {
        List<ScenarioManifestStableCaptureResult.CapturedMember> ordered = List.of(members);
        ScenarioManifestStableCaptureResult.Completed capture = new ScenarioManifestStableCaptureResult.Completed(
                ordered, List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(
                capture,
                "repository:orders",
                "capture:17",
                CONTRACT,
                ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, String json) {
        byte[] exactBytes = json.getBytes(StandardCharsets.UTF_8);
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + name + ".scenario.json",
                exactBytes,
                exactBytes.length,
                RawSourceMemberFingerprint.calculate(exactBytes));
    }

    private static String validManifest(String authority, String scenarioKey) {
        return """
                {
                  "format":"qaip-scenario-authority-manifest-v1",
                  "schemaVersion":"1.0",
                  "authority":"%s",
                  "scenarioIdentityScheme":"qaip-scenario-identity-v1",
                  "scenarios":[{
                    "scenarioKey":"%s",
                    "title":"Scenario",
                    "given":["A precondition"],
                    "when":["An action"],
                    "then":["An outcome"],
                    "operationRef":{
                      "identityScheme":"qaip-http-operation-reference-v1",
                      "method":"POST",
                      "path":"/api/orders"
                    },
                    "ruleRefs":[]
                  }]
                }
                """.formatted(authority, scenarioKey);
    }
}
