package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributorV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityExactJsonParserV1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributedMemberOutcomeFingerprintComposerTest {
    private static final RepositoryCaptureFingerprint CAPTURE = new RepositoryCaptureFingerprint(
            RepositoryCaptureFingerprint.VALUE_PREFIX + "a".repeat(64));
    private static final ParentCapturedMemberRef PARENT = new ParentCapturedMemberRef(
            "repository:orders", "capture:17", CAPTURE,
            ".qaip/scenarios/orders.scenario.json", 321,
            new RawSourceMemberFingerprint(RawSourceMemberFingerprint.VALUE_PREFIX + "b".repeat(64)));
    @Test
    void admittedCompositionConsumesOnlyAuthoritativeManifestOutcome() {
        var handoff=ManifestSemanticOutcomeMapperV1Test.handoff("capture:amo","amo","orders","A");
        var manifest=(ScenarioSourceNormalizedRecords.NormalizedManifestDatum)handoff.normalizationResult().memberOutcomes().getFirst();
        var outcome=new ManifestSemanticOutcomeMapperV1().attempt(handoff,manifest);
        var composition=AttributedMemberOutcomeFingerprintComposer.fingerprintAdmitted(outcome);
        assertEquals(Optional.of(((ManifestSemanticCompositionOutcomeV1.Composed)outcome).fingerprint()),
                composition.input().manifestSemanticFingerprint());
    }

    @Test
    void delegatesCanonicalStateAndDiagnosticIntegrityToEvidenceGovernance() {
        var diagnostic = ScenarioSchemaDiagnostic.v1(
                "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));
        var rejected = outcome(
                AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(diagnostic), JsonNodeFactory.instance.objectNode());

        var first=AttributedMemberOutcomeFingerprintComposer.fingerprintRejected(rejected);
        var second=AttributedMemberOutcomeFingerprintComposer.fingerprintRejected(rejected);
        assertEquals(first.input(),second.input());assertEquals(first.fingerprint(),second.fingerprint());
        assertThrows(IllegalArgumentException.class, () ->
                AttributedMemberOutcomeFingerprintComposer.fingerprintRejected(admitted(JsonNodeFactory.instance.objectNode())));
    }

    private static AttributedMemberSchemaAdmissionOutcome admitted(com.fasterxml.jackson.databind.JsonNode json) {
        return outcome(AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), json);
    }

    private static AttributedMemberSchemaAdmissionOutcome outcome(
            AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState state,
            List<ScenarioSchemaDiagnostic> diagnostics,
            com.fasterxml.jackson.databind.JsonNode json
    ) {
        com.fasterxml.jackson.databind.JsonNode proofDocument = json.deepCopy();
        if (proofDocument instanceof com.fasterxml.jackson.databind.node.ObjectNode object
                && !object.has("authority")) object.put("authority", "orders");
        var parsed = new ScenarioAuthorityExactJsonParserV1().parseExactBytes(
                proofDocument.toString().getBytes(StandardCharsets.UTF_8));
        var attribution = new ScenarioAuthorityAttributorV1().attribute(parsed);
        return new AttributedMemberSchemaAdmissionOutcome(
                PARENT, "orders", ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), ScenarioManifestSchemaValidator.SCHEMA_CONTRACT_IDENTIFIER,
                state, diagnostics, parsed, attribution, proofDocument);
    }
}
