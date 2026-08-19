package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticProvenanceFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final RepositoryCaptureFingerprint CAPTURE = new RepositoryCaptureFingerprint(
            RepositoryCaptureFingerprint.VALUE_PREFIX + "a".repeat(64));
    private static final RawSourceMemberFingerprint RAW = new RawSourceMemberFingerprint(
            RawSourceMemberFingerprint.VALUE_PREFIX + "b".repeat(64));
    private static final ManifestSemanticFingerprint MANIFEST = new ManifestSemanticFingerprint(
            ManifestSemanticFingerprint.VALUE_PREFIX + "c".repeat(64));
    private static final ScenarioSchemaDiagnostic REQUIRED = ScenarioSchemaDiagnostic.v1(
            "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
            List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));

    @Test
    void freezesIdentifiersAndGoldenVectors() {
        var admitted = provenance(admittedInput(parent()));
        var rejected = provenance(rejectedInput(parent()));
        assertEquals("scenario-authority-semantic-provenance-c14n-v1",
                SemanticProvenanceFingerprintEncoder.ENCODING_IDENTIFIER);
        assertEquals("sha-256-v1", SemanticProvenanceFingerprintEncoder.DIGEST_IDENTIFIER);
        assertEquals("scenario-authority-semantic-provenance-v1",
                SemanticProvenanceFingerprint.VALUE_IDENTIFIER);
        assertEquals("scenario-authority-semantic-provenance-identity-v1",
                SemanticProvenanceIdentityV1.IDENTITY_CONTRACT_VERSION);
        assertEquals("DERIVE_ATTRIBUTED_MEMBER_OUTCOME", activity());
        assertEquals("ATTRIBUTED_MEMBER_OUTCOME", outputKind());
        assertEquals("DERIVED", outcome());
        assertEquals("51414950005343454e4152494f5f415554484f524954595f53454d414e5449435f50524f56454e414e4345005631",
                HEX.formatHex(SemanticProvenanceFingerprintEncoder.DOMAIN.getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals(golden("identity"),
                SemanticProvenanceFingerprintEncoder.identityBytes(admitted.provenanceIdentity()));
        assertArrayEquals(golden("parent"),
                SemanticProvenanceFingerprintEncoder.parentBytes(admitted.parents().getFirst()));
        assertArrayEquals(golden("admitted"), SemanticProvenanceFingerprintEncoder.encode(admitted));
        assertEquals("scenario-authority-semantic-provenance-v1:9ca7b967523100eec3945665a9e0abe691ea3f2525de6f815c461c40154af484",
                fingerprint(admitted).value());
        assertArrayEquals(golden("rejected"), SemanticProvenanceFingerprintEncoder.encode(rejected));
        assertEquals("scenario-authority-semantic-provenance-v1:be3f5d9c28b613445fd5b40850a4572bb72c01e19b7626974532187013d40adf",
                fingerprint(rejected).value());
    }

    @Test
    void isDeterministicAndBindsMemberAndAttributedOutcomeContent() {
        var admitted = provenance(admittedInput(parent()));
        assertEquals(fingerprint(admitted), fingerprint(admitted));
        assertArrayEquals(SemanticProvenanceFingerprintEncoder.encode(admitted),
                SemanticProvenanceFingerprintEncoder.encode(admitted));
        assertNotEquals(fingerprint(admitted), fingerprint(provenance(rejectedInput(parent()))));
        var differentMember = fingerprint(provenance(admittedInput(parent(
                ".qaip/scenarios/other.scenario.json"))));
        assertNotEquals(fingerprint(admitted), differentMember);
        assertEquals("scenario-authority-semantic-provenance-v1:f190c375619195a4ace0e86c14d5774c9284e89ca9cfe50b481315a7b5621cb6",
                differentMember.value());
    }

    @Test
    void rejectsFingerprintSubstitutionAndParentIdentityMismatch() {
        var authoritative = admittedInput(parent());
        var wrongFingerprint = AttributedMemberOutcomeFingerprintEncoder.fingerprint(
                admittedInput(parent(".qaip/scenarios/other.scenario.json")));
        assertThrows(IllegalArgumentException.class, () ->
                SemanticProvenanceOutputReference.verified(authoritative, wrongFingerprint));

        var output = output(authoritative);
        var wrongParent = SemanticProvenanceParentReference.capturedMember(
                parent(".qaip/scenarios/other.scenario.json"));
        assertThrows(IllegalArgumentException.class, () -> candidate(
                SemanticProvenanceFingerprintEncoder.ENCODING_IDENTIFIER,
                SemanticProvenanceIdentityV1.forAttributedMember(output.outputIdentity()),
                SemanticProvenanceIdentityV1.ACTIVITY_KIND,
                SemanticProvenanceFingerprintInput.ACTIVITY_VERSION,
                output, List.of(wrongParent), SemanticProvenanceFingerprintInput.DERIVATION_OUTCOME));
    }

    @Test
    void rejectsInvalidParentCardinalityAndEveryOtherParentKind() {
        var output = output(admittedInput(parent()));
        var identity = SemanticProvenanceIdentityV1.forAttributedMember(output.outputIdentity());
        assertThrows(IllegalArgumentException.class, () -> candidate(
                current(), identity, activity(), activityVersion(), output, List.of(), outcome()));
        assertThrows(IllegalArgumentException.class, () -> candidate(
                current(), identity, activity(), activityVersion(), output,
                List.of(parentRef(), parentRef()), outcome()));
        assertThrows(IllegalArgumentException.class, () ->
                new SemanticProvenanceParentReference("ATTRIBUTED_MEMBER_OUTCOME", parent()));
        assertThrows(IllegalArgumentException.class, () ->
                new SemanticProvenanceParentReference("SEMANTIC_DATUM", parent()));
        assertThrows(IllegalArgumentException.class, () ->
                new SemanticProvenanceParentReference("SEMANTIC_PROVENANCE", parent()));
        assertThrows(IllegalArgumentException.class, () ->
                new SemanticProvenanceParentReference("REPOSITORY_DERIVATION_REPORT", parent()));
    }

    @Test
    void rejectsUnsupportedVersionsActivityOutputAndOutcome() {
        var output = output(admittedInput(parent()));
        var identity = SemanticProvenanceIdentityV1.forAttributedMember(output.outputIdentity());
        assertThrows(IllegalArgumentException.class, () -> candidate(
                "scenario-authority-semantic-provenance-c14n-v2", identity,
                activity(), activityVersion(), output, List.of(parentRef()), outcome()));
        assertThrows(IllegalArgumentException.class, () -> candidate(
                current(), identity, "COMPOSE_MANIFEST_SEMANTIC_CONTENT",
                activityVersion(), output, List.of(parentRef()), outcome()));
        assertThrows(IllegalArgumentException.class, () -> candidate(
                current(), identity, activity(), "scenario-authority-derive-attributed-member-outcome-v2",
                output, List.of(parentRef()), outcome()));
        assertThrows(IllegalArgumentException.class, () -> candidate(
                current(), identity, activity(), activityVersion(), output, List.of(parentRef()), "FAILED"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProvenanceIdentityV1(
                "scenario-authority-semantic-provenance-identity-v2", activity(), outputKind(), parent()));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProvenanceIdentityV1(
                SemanticProvenanceIdentityV1.IDENTITY_CONTRACT_VERSION,
                "CLASSIFY_SCENARIO_IDENTITY_GROUP", outputKind(), parent()));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProvenanceIdentityV1(
                SemanticProvenanceIdentityV1.IDENTITY_CONTRACT_VERSION,
                activity(), "SCENARIO_SEMANTIC_CONTENT", parent()));
    }

    @Test
    void fingerprintValueRejectsMalformedOrNonlowercaseValues() {
        assertThrows(IllegalArgumentException.class, () -> new SemanticProvenanceFingerprint(
                SemanticProvenanceFingerprint.VALUE_PREFIX + "A".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new SemanticProvenanceFingerprint(
                "sha-256-v1:" + "a".repeat(64)));
    }

    private static SemanticProvenanceFingerprintInput provenance(
            AttributedMemberOutcomeFingerprintInput attributedInput
    ) {
        return SemanticProvenanceFingerprintInput.deriveAttributedMemberOutcome(output(attributedInput));
    }

    private static SemanticProvenanceOutputReference output(
            AttributedMemberOutcomeFingerprintInput attributedInput
    ) {
        return SemanticProvenanceOutputReference.verified(
                attributedInput, AttributedMemberOutcomeFingerprintEncoder.fingerprint(attributedInput));
    }

    private static SemanticProvenanceFingerprintInput candidate(
            String version,
            SemanticProvenanceIdentityV1 identity,
            String activity,
            String activityVersion,
            SemanticProvenanceOutputReference output,
            List<SemanticProvenanceParentReference> parents,
            String outcome
    ) {
        return new SemanticProvenanceFingerprintInput(
                version, identity, activity, activityVersion, output, parents, outcome);
    }

    private static AttributedMemberOutcomeFingerprintInput admittedInput(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent
    ) {
        return attributedInput(parent,
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.of(new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                        parent.parentSourceId(), parent.parentSnapshotId(), parent.parentContentFingerprint(),
                        parent.normalizedRepositoryRelativePath(),
                        AttributedMemberOutcomeFingerprintInput.MANIFEST_OCCURRENCE_IDENTITY_VERSION)),
                Optional.of(MANIFEST));
    }

    private static AttributedMemberOutcomeFingerprintInput rejectedInput(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent
    ) {
        return attributedInput(parent,
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(REQUIRED), Optional.empty(), Optional.empty());
    }

    private static AttributedMemberOutcomeFingerprintInput attributedInput(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent,
            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState state,
            List<ScenarioSchemaDiagnostic> diagnostics,
            Optional<AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity> occurrence,
            Optional<ManifestSemanticFingerprint> manifest
    ) {
        return new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER,
                parent, "orders",
                AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,
                state, diagnostics, occurrence, manifest);
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent() {
        return parent(".qaip/scenarios/orders.scenario.json");
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent(String path) {
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                "repository:orders", "capture:17", CAPTURE, path, 321, RAW);
    }

    private static SemanticProvenanceParentReference parentRef() {
        return SemanticProvenanceParentReference.capturedMember(parent());
    }

    private static SemanticProvenanceFingerprint fingerprint(SemanticProvenanceFingerprintInput input) {
        return SemanticProvenanceFingerprintEncoder.fingerprint(input);
    }

    private static byte[] golden(String name) {
        try (var stream = SemanticProvenanceFingerprintEncoderTest.class.getResourceAsStream(
                "/golden/semantic-provenance-derive-" + name + ".canonical.b64")) {
            if (stream == null) throw new IllegalStateException("missing golden vector " + name);
            return Base64.getMimeDecoder().decode(stream.readAllBytes());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("cannot read golden vector " + name, exception);
        }
    }

    private static String current() { return SemanticProvenanceFingerprintEncoder.ENCODING_IDENTIFIER; }
    private static String activity() { return SemanticProvenanceIdentityV1.ACTIVITY_KIND; }
    private static String activityVersion() { return SemanticProvenanceFingerprintInput.ACTIVITY_VERSION; }
    private static String outputKind() { return SemanticProvenanceIdentityV1.OUTPUT_KIND; }
    private static String outcome() { return SemanticProvenanceFingerprintInput.DERIVATION_OUTCOME; }
}
