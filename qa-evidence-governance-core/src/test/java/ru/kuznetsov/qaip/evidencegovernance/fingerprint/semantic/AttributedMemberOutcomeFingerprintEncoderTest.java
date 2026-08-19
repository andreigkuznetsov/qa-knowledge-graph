package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributedMemberOutcomeFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final RepositoryCaptureFingerprint CAPTURE = new RepositoryCaptureFingerprint(
            RepositoryCaptureFingerprint.VALUE_PREFIX + "a".repeat(64));
    private static final RawSourceMemberFingerprint RAW = new RawSourceMemberFingerprint(
            RawSourceMemberFingerprint.VALUE_PREFIX + "b".repeat(64));
    private static final ManifestSemanticFingerprint MANIFEST = new ManifestSemanticFingerprint(
            ManifestSemanticFingerprint.VALUE_PREFIX + "c".repeat(64));
    private static final ScenarioSchemaDiagnostic REQUIRED_AUTHORITY = ScenarioSchemaDiagnostic.v1(
            "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
            List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));
    private static final ScenarioSchemaDiagnostic FORMAT_CONST = ScenarioSchemaDiagnostic.v1(
            "/format", "const", ScenarioSchemaDiagnostic.RULE_PREFIX + "/properties/format/const",
            List.of(new ScenarioSchemaDiagnostic.TextParameter(
                    "expectedText", "qaip-scenario-authority-manifest-v1")));

    @Test
    void freezesIdentifiersAndGoldenVectors() {
        assertEquals("QAIP\u0000SCENARIO_AUTHORITY_ATTRIBUTED_MEMBER_OUTCOME\u0000V1",
                AttributedMemberOutcomeFingerprintEncoder.DOMAIN);
        assertEquals("scenario-authority-attributed-member-outcome-c14n-v1",
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", AttributedMemberOutcomeFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("scenario-authority-attributed-member-outcome-v1",
                AttributedMemberOutcomeFingerprint.VALUE_IDENTIFIER);

        assertEquals("51414950005343454e4152494f5f415554484f524954595f415454524942555445445f4d454d4245525f4f5554434f4d45005631",
                HEX.formatHex(AttributedMemberOutcomeFingerprintEncoder.DOMAIN.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertArrayEquals(golden("admitted"), AttributedMemberOutcomeFingerprintEncoder.encode(admitted(MANIFEST)));
        assertEquals("scenario-authority-attributed-member-outcome-v1:db399b2acb4327ed0cd535830c4b7ff6deb3ee39a7f5cb3d33238bf71520b4e9",
                fingerprint(admitted(MANIFEST)).value());
        assertArrayEquals(golden("rejected-one"), AttributedMemberOutcomeFingerprintEncoder.encode(rejected(
                Optional.of("/authority"), List.of(REQUIRED_AUTHORITY))));
        assertEquals("scenario-authority-attributed-member-outcome-v1:a828de58271491a7cbab601f9724b70ae39c820d1cc799b2b11a0ba04e9ac4a6",
                fingerprint(rejected(Optional.of("/authority"), List.of(REQUIRED_AUTHORITY))).value());
        assertArrayEquals(golden("rejected-multi"), AttributedMemberOutcomeFingerprintEncoder.encode(rejected(
                Optional.empty(), List.of(REQUIRED_AUTHORITY, FORMAT_CONST))));
        assertEquals("scenario-authority-attributed-member-outcome-v1:a32d0823741558cc42c9c68fb517afeef02f5ee90454ef2bc135013a35655d24",
                fingerprint(rejected(Optional.empty(), List.of(REQUIRED_AUTHORITY, FORMAT_CONST))).value());
    }

    @Test
    void semanticChangesAffectFingerprintAndRepeatedCalculationIsDeterministic() {
        AttributedMemberOutcomeFingerprintInput admitted = admitted(MANIFEST);
        assertEquals(fingerprint(admitted), fingerprint(admitted));
        assertNotEquals(fingerprint(admitted), fingerprint(admitted(new ManifestSemanticFingerprint(
                ManifestSemanticFingerprint.VALUE_PREFIX + "d".repeat(64)))));
        assertNotEquals(fingerprint(admitted), fingerprint(rejected(
                Optional.empty(), List.of(REQUIRED_AUTHORITY))));

        AttributedMemberOutcomeFingerprintInput rejected = rejected(
                Optional.empty(), List.of(REQUIRED_AUTHORITY));
        assertNotEquals(fingerprint(rejected), fingerprint(withAuthority(rejected, "payments")));
        assertNotEquals(fingerprint(rejected), fingerprint(withParent(rejected,
                parent(".qaip/scenarios/other.scenario.json", CAPTURE))));
        assertNotEquals(fingerprint(rejected), fingerprint(withParent(rejected,
                parent(path(), new RepositoryCaptureFingerprint(
                        RepositoryCaptureFingerprint.VALUE_PREFIX + "e".repeat(64))))));
        assertNotEquals(fingerprint(rejected), fingerprint(rejected(
                Optional.empty(), List.of(ScenarioSchemaDiagnostic.v1(
                        "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
                        List.of(new ScenarioSchemaDiagnostic.TextParameter(
                                "missingProperty", "scenarios")))))));
        assertNotEquals(fingerprint(rejected), fingerprint(rejected(
                Optional.of("/authority"), List.of(REQUIRED_AUTHORITY))));
    }

    @Test
    void rejectsEveryInconsistentStateCombinationAndUnsupportedContract() {
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(REQUIRED_AUTHORITY), Optional.of(occurrence()), Optional.of(MANIFEST), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.of(occurrence()), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.empty(), Optional.of(MANIFEST), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.of(MANIFEST), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.of(occurrence()), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprintInput(
                "scenario-authority-attributed-member-outcome-c14n-v2", parent(), "orders",
                parser(), attribution(), AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED, Optional.empty(),
                schema(), AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprintInput(
                current(), parent(), "orders", "unsupported-parser", attribution(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED, Optional.empty(),
                schema(), AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprintInput(
                current(), parent(), "orders", parser(), "unsupported-attribution",
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED, Optional.empty(),
                schema(), AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprintInput(
                current(), parent(), "orders", parser(), attribution(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED, Optional.empty(),
                "unsupported-schema", AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> new AttributedMemberOutcomeFingerprintInput(
                current(), parent(), "orders", parser(), attribution(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED, Optional.empty(),
                schema(), null, List.of(REQUIRED_AUTHORITY), Optional.empty(), Optional.empty()));
    }

    @Test
    void rejectsNoncanonicalDiagnosticCollectionsAndMismatchedOccurrence() {
        assertThrows(IllegalArgumentException.class, () -> rejected(
                Optional.empty(), List.of(FORMAT_CONST, REQUIRED_AUTHORITY)));
        assertThrows(IllegalArgumentException.class, () -> rejected(
                Optional.empty(), List.of(REQUIRED_AUTHORITY, REQUIRED_AUTHORITY)));
        AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity mismatch =
                new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                        "repository:other", "capture:17", CAPTURE, path(), occurrenceVersion());
        assertThrows(IllegalArgumentException.class, () -> input(
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.of(mismatch), Optional.of(MANIFEST), Optional.empty()));
    }

    @Test
    void syntaxValidatorAndOperationalMetadataCannotEnterInput() {
        List<String> excluded = List.of("json", "message", "networknt", "exception", "timestamp",
                "git", "qualification", "resolution", "operation", "rule");
        assertFalse(Arrays.stream(AttributedMemberOutcomeFingerprintInput.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .anyMatch(name -> excluded.stream().anyMatch(name::contains)));
    }

    @Test
    void encodingAndValueAreImmutableAndStrict() {
        byte[] first = AttributedMemberOutcomeFingerprintEncoder.encode(admitted(MANIFEST));
        byte[] second = AttributedMemberOutcomeFingerprintEncoder.encode(admitted(MANIFEST));
        assertArrayEquals(first, second);
        first[0] = 0x7f;
        assertArrayEquals(second, AttributedMemberOutcomeFingerprintEncoder.encode(admitted(MANIFEST)));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprint(
                AttributedMemberOutcomeFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new AttributedMemberOutcomeFingerprint(
                "sha-256-v1:" + "a".repeat(64)));
    }

    private static AttributedMemberOutcomeFingerprintInput admitted(ManifestSemanticFingerprint manifest) {
        return input(AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.of(occurrence()), Optional.of(manifest), Optional.empty());
    }

    private static AttributedMemberOutcomeFingerprintInput rejected(
            Optional<String> structuralLocation,
            List<ScenarioSchemaDiagnostic> diagnostics
    ) {
        return input(AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                diagnostics, Optional.empty(), Optional.empty(), structuralLocation);
    }

    private static AttributedMemberOutcomeFingerprintInput input(
            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState state,
            List<ScenarioSchemaDiagnostic> diagnostics,
            Optional<AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity> occurrence,
            Optional<ManifestSemanticFingerprint> manifest,
            Optional<String> structuralLocation
    ) {
        return new AttributedMemberOutcomeFingerprintInput(
                current(), parent(), "orders", parser(), attribution(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                structuralLocation, schema(), state, diagnostics, occurrence, manifest);
    }

    private static AttributedMemberOutcomeFingerprintInput withAuthority(
            AttributedMemberOutcomeFingerprintInput input, String authority
    ) {
        return new AttributedMemberOutcomeFingerprintInput(
                current(), input.parentMemberReference(), authority, parser(), attribution(),
                input.parseOutcome(), input.attributionOutcome(), input.attributionStructuralLocation(),
                schema(), input.structuralAdmissionState(), input.schemaDiagnostics(),
                input.manifestOccurrenceIdentity(), input.manifestSemanticFingerprint());
    }

    private static AttributedMemberOutcomeFingerprintInput withParent(
            AttributedMemberOutcomeFingerprintInput input,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent
    ) {
        return new AttributedMemberOutcomeFingerprintInput(
                current(), parent, input.claimedAuthority(), parser(), attribution(),
                input.parseOutcome(), input.attributionOutcome(), input.attributionStructuralLocation(),
                schema(), input.structuralAdmissionState(), input.schemaDiagnostics(),
                input.manifestOccurrenceIdentity(), input.manifestSemanticFingerprint());
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent() {
        return parent(path(), CAPTURE);
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent(
            String path, RepositoryCaptureFingerprint capture
    ) {
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                "repository:orders", "capture:17", capture, path, 321, RAW);
    }

    private static AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity occurrence() {
        return new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                "repository:orders", "capture:17", CAPTURE, path(), occurrenceVersion());
    }

    private static AttributedMemberOutcomeFingerprint fingerprint(
            AttributedMemberOutcomeFingerprintInput input
    ) {
        return AttributedMemberOutcomeFingerprintEncoder.fingerprint(input);
    }

    private static byte[] golden(String name) {
        try (var stream = AttributedMemberOutcomeFingerprintEncoderTest.class.getResourceAsStream(
                "/golden/attributed-member-outcome-" + name + ".canonical.b64")) {
            if (stream == null) {
                throw new IllegalStateException("missing golden vector " + name);
            }
            return Base64.getMimeDecoder().decode(stream.readAllBytes());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("cannot read golden vector " + name, exception);
        }
    }

    private static String current() { return AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER; }
    private static String parser() { return AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER; }
    private static String attribution() { return AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER; }
    private static String schema() { return AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER; }
    private static String occurrenceVersion() { return AttributedMemberOutcomeFingerprintInput.MANIFEST_OCCURRENCE_IDENTITY_VERSION; }
    private static String path() { return ".qaip/scenarios/orders.scenario.json"; }
}
