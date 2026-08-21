package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityPartitionSourceVerifierV2Test {
    @Test void oneMemberComposedPartitionRetainsEveryAuthoritativeProof() {
        var fixture = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var verified = verify(fixture);
        assertEquals("orders", verified.claimedAuthority());
        assertEquals(fixture.capture, verified.capture());
        assertEquals(List.of(fixture.composition), verified.attributedMemberOutcomes());
        assertEquals(List.of(fixture.manifestOutcome), verified.manifestOutcomes());
        assertEquals(List.of(fixture.group), verified.scenarioIdentityGroups());
        assertEquals(1, verified.composedManifests().size());
        assertEquals(1, verified.scenarioOccurrenceOutcomes().size());
    }

    @Test void unavailableAndRejectedRemainPartitionEvidenceWithoutNormalizedManifestEligibility() {
        var unavailable = admitted("a.json", List.of(
                child("a.json", 0, "same"), child("a.json", 1, "u", "future-source")), true);
        var unavailableVerified = verify(unavailable);
        assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,
                unavailableVerified.manifestOutcomes().getFirst());
        assertTrue(unavailableVerified.composedManifests().isEmpty());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,
                unavailableVerified.scenarioIdentityGroups().getFirst().state());

        var rejected = rejected("a.json");
        var rejectedVerified = ScenarioAuthorityPartitionSourceVerifierV2.verify(
                rejected.capture, rejected.report, List.of(rejected.composition), List.of(), List.of());
        assertTrue(rejectedVerified.manifestOutcomes().isEmpty());
        assertTrue(rejectedVerified.scenarioOccurrenceOutcomes().isEmpty());
    }

    @Test void exactMultipleMemberOrderComesFromDerivationReport() {
        var captureFixture = ScenarioIdentityGroupComposerV1Test.fixture("a.json", "b.json");
        var first = admitted(captureFixture, List.of(child(captureFixture,
                new ChildSpec("a.json", 0, "same", ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))), false);
        var second = admitted(captureFixture, List.of(child(captureFixture,
                new ChildSpec("b.json", 0, "same", ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))), false);
        var compositions = List.of(first.composition, second.composition);
        var manifests = List.of(first.manifestOutcome, second.manifestOutcome);
        var group = ScenarioIdentityGroupComposerV1.compose(
                ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,
                manifests.getFirst().childOutcomes().getFirst().occurrence().claimedIdentity(),
                List.of(manifests.getFirst().childOutcomes().getFirst(),
                        manifests.get(1).childOutcomes().getFirst()));
        var report = report(captureFixture.att(), compositions);
        var verified = ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, compositions, manifests, List.of(group));
        assertEquals(captureFixture.refs(), verified.attributedMemberOutcomes().stream()
                .map(value -> value.input().parentMemberReference()).toList());
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(second.composition, first.composition), manifests, List.of(group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(first.composition), List.of(first.manifestOutcome), List.of(group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(first.composition, first.composition), manifests, List.of(group)));
    }

    @Test void manifestOutcomeMustExactlyMatchAttributedMember() {
        var first = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition),
                List.of(foreign.manifestOutcome), List.of(first.group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition), List.of(), List.of(first.group)));
        var composed = (ManifestSemanticCompositionOutcomeV1.Composed) first.manifestOutcome;
        var substituted = new ManifestSemanticCompositionOutcomeV1.Composed(
                composed.manifest(), composed.childOutcomes(), composed.scenarioFingerprints(),
                new ManifestSemanticFingerprint(ManifestSemanticFingerprint.VALUE_PREFIX + "0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition),
                List.of(substituted), List.of(first.group)));
    }

    @Test void groupsMustCoverExactManifestOccurrencesOnce() {
        var fixture = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(fixture.group, fixture.group)));
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(foreign.group)));
    }

    @Test void everyManifestAndDuplicateCombinationRemainsIndependent() {
        assertCombination(List.of(child("a.json", 0, "same")), false,
                VerifiedScenarioIdentityGroupV1.State.UNIQUE);
        assertCombination(List.of(child("a.json", 0, "same"), child("a.json", 1, "same")), false,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT);
        assertCombination(List.of(child("a.json", 0, "one"), child("a.json", 1, "two")), false,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING);
        assertCombination(List.of(child("a.json", 0, "same"), child("a.json", 1, "u", "future-source")), true,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED);
    }

    @Test void foreignCaptureAndInternallyConsistentForeignPartitionAreRejected() {
        var local = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                local.capture, foreign.report, List.of(foreign.composition),
                List.of(foreign.manifestOutcome), List.of(foreign.group)));
    }

    @Test void crossAuthorityMemberCannotEnterTheDerivedPartition() {
        var fixture = ScenarioIdentityGroupComposerV1Test.fixture("a.json", "b.json");
        var orders = rejectedComposition(fixture.refs().getFirst(), "orders");
        var payments = rejectedComposition(fixture.refs().get(1), "payments");
        var report = report(fixture.att(), List.of(orders, payments));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.att(), report, List.of(orders, payments), List.of(), List.of()));
    }

    @Test void verifiedTypeIsMintedOnlyByTheVerificationBoundary() {
        assertTrue(Arrays.stream(VerifiedScenarioAuthorityPartitionSourceV2.class.getDeclaredConstructors())
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())));
        assertTrue(Arrays.stream(VerifiedScenarioAuthorityPartitionSourceV2.class.getMethods())
                .noneMatch(method -> method.getName().equals("verified") || method.getName().equals("of")));
        assertEquals(1, Arrays.stream(ScenarioAuthorityPartitionSourceVerifierV2.class.getMethods())
                .filter(method -> method.getName().equals("verify")).count());
    }

    private static void assertCombination(List<ChildSpec> specs, boolean unavailable,
                                          VerifiedScenarioIdentityGroupV1.State state) {
        var fixture = admitted("a.json", specs, unavailable);
        var verified = verify(fixture);
        assertEquals(state, verified.scenarioIdentityGroups().getFirst().state());
        assertEquals(unavailable, verified.manifestOutcomes().getFirst()
                instanceof ManifestSemanticCompositionOutcomeV1.Unavailable);
    }

    private static VerifiedScenarioAuthorityPartitionSourceV2 verify(AdmittedFixture fixture) {
        return ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(fixture.group));
    }

    private static AdmittedFixture admitted(String path, List<ChildSpec> specs, boolean unavailable) {
        var capture = ScenarioIdentityGroupComposerV1Test.fixture(path);
        return admitted(capture, specs.stream().map(spec -> child(capture, spec)).toList(), unavailable);
    }

    private static AdmittedFixture admitted(ScenarioIdentityGroupComposerV1Test.Fixture fixture,
                                             List<NormalizedScenarioOccurrenceInputV1> children,
                                             boolean unavailable) {
        var manifest = ManifestSemanticCompositionAttemptV1Test.verified(fixture, children);
        var childOutcomes = children.stream().map(ManifestSemanticCompositionAttemptV1Test::attemptChild).toList();
        var outcome = ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(
                NormalizedManifestSemanticCompositionInputV1.selectedV1(manifest, childOutcomes));
        assertEquals(unavailable, outcome instanceof ManifestSemanticCompositionOutcomeV1.Unavailable);
        var composition = AttributedMemberOutcomeComposerV1.composeAdmitted(outcome);
        var group = ScenarioIdentityGroupComposerV1.compose(
                ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,
                children.getFirst().claimedIdentity(), childOutcomes);
        var report = fixture.refs().size() == 1 ? report(fixture.att(), List.of(composition)) : null;
        return new AdmittedFixture(fixture.att(), report,
                composition, outcome, group);
    }

    private static RejectedFixture rejected(String path) {
        var fixture = ScenarioIdentityGroupComposerV1Test.fixture(path);
        var composition = rejectedComposition(fixture.refs().getFirst(), "orders");
        return new RejectedFixture(fixture.att(), report(fixture.att(), List.of(composition)), composition);
    }

    private static AttributedMemberOutcomeComposerV1.Composition rejectedComposition(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent,
            String authority) {
        var diagnostic = ScenarioSchemaDiagnostic.v1("", "required",
                ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));
        var input = new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER, parent, authority,
                AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(diagnostic), Optional.empty(), Optional.empty());
        return AttributedMemberOutcomeComposerV1.composeRejected(input);
    }

    private static RepositoryDerivationReportFingerprintInput report(
            RepositoryCaptureAttestation capture,
            List<AttributedMemberOutcomeComposerV1.Composition> compositions
    ) {
        var outcomes = compositions.stream().map(composition ->
                new RepositoryDerivationReportFingerprintInput.AttributedMemberRetained(
                        composition.input().parentMemberReference(), composition.input().claimedAuthority(),
                        SemanticProvenanceOutputReference.verified(composition))).toList();
        var provenance = outcomes.stream().map(outcome ->
                SemanticProvenanceAttestation.deriveAttributedMemberOutcome(outcome.attributedMemberOutput())).toList();
        return new RepositoryDerivationReportFingerprintInput(
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION,
                RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture.verified(capture),
                RepositoryDerivationReportFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                new ArrayList<>(outcomes),
                capture.unsupportedMatchingEntries().stream().map(entry ->
                        new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                                entry.normalizedRepositoryRelativePath(), entry.entryKind(),
                                entry.stableDiagnosticCode())).toList(),
                new ArrayList<>(provenance));
    }

    private static ChildSpec child(String path, int index, String title) {
        return new ChildSpec(path, index, title, ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION);
    }
    private static ChildSpec child(String path, int index, String title, String sourceVersion) {
        return new ChildSpec(path, index, title, sourceVersion);
    }
    private static NormalizedScenarioOccurrenceInputV1 child(
            ScenarioIdentityGroupComposerV1Test.Fixture fixture, ChildSpec spec) {
        return ScenarioIdentityGroupComposerV1Test.input(
                fixture, spec.path, spec.index, spec.title, spec.sourceVersion);
    }

    private record ChildSpec(String path, int index, String title, String sourceVersion) {}
    private record AdmittedFixture(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report,
            AttributedMemberOutcomeComposerV1.Composition composition,
            ManifestSemanticCompositionOutcomeV1 manifestOutcome,
            VerifiedScenarioIdentityGroupV1 group) {}
    private record RejectedFixture(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report,
            AttributedMemberOutcomeComposerV1.Composition composition) {}
}
