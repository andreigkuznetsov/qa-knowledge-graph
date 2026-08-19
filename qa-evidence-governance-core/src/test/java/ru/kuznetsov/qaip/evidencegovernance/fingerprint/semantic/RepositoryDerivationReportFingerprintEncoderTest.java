package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryDerivationReportFingerprintEncoderTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final RepositoryCaptureFingerprint CAPTURE = new RepositoryCaptureFingerprint(
            RepositoryCaptureFingerprint.VALUE_PREFIX + "a".repeat(64));
    private static final ScenarioSchemaDiagnostic REQUIRED = ScenarioSchemaDiagnostic.v1(
            "", "required", ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
            List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));

    @Test
    void locksNormativeGoldenCorpus() {
        var empty = report(List.of(), List.of(), List.of(), List.of());
        var admitted = attributed("a.scenario.json", true, "orders");
        var rejected = attributed("a.scenario.json", false, "orders");
        var invalidUtf8 = unattributable("a.scenario.json", "INVALID_UTF8", "PARSE_UNATTRIBUTABLE", Optional.empty());
        var malformed = unattributable("a.scenario.json", "MALFORMED_JSON", "PARSE_UNATTRIBUTABLE", Optional.empty());
        var missing = unattributable("a.scenario.json", "PARSED", "MISSING_AUTHORITY", Optional.of(""));
        var mixedA = attributed("a.scenario.json", true, "orders");
        var mixedB = unattributable("b.scenario.json", "PARSED", "INVALID_AUTHORITY", Optional.of("/authority"));
        var mixed = report(List.of(mixedA.parentMember(), mixedB.parentMember()),
                List.of(mixedA.outcome(), mixedB.outcome()), List.of(), List.of(mixedA.provenance()));
        var multiA = attributed("a.scenario.json", true, "orders");
        var multiB = attributed("b.scenario.json", true, "payments");
        var multiple = report(List.of(multiA.parentMember(), multiB.parentMember()),
                List.of(multiA.outcome(), multiB.outcome()), List.of(),
                List.of(multiA.provenance(), multiB.provenance()));
        var unsupported = List.of(new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                "z.scenario.json", "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK"));
        assertGolden(empty, "da910082afb4a8cfbec475defbebbd6da12a662c409339eb2da7eee0f3124994");
        assertGolden(report(attributedMembers(admitted), outcomes(admitted), List.of(), provenance(admitted)),
                "17d949febcd3267c0276de543baa061655fb37a4e9c5d21c79d89e060b01e543");
        assertGolden(report(attributedMembers(rejected), outcomes(rejected), List.of(), provenance(rejected)),
                "3a1937348bd3b8398a254331bc591b10216bf883b7ab054febebe1dd6118220b");
        assertGolden(report(unattributableMembers(invalidUtf8), unattributableOutcomes(invalidUtf8), List.of(), List.of()),
                "410de19beb15a7bdd5d119b1b44b9c329846b7accaf2c497b9367f8d2b1e0422");
        assertGolden(report(unattributableMembers(malformed), unattributableOutcomes(malformed), List.of(), List.of()),
                "b2d7791a8586297ef6146665f78d587c32719837ad271008b11ee0a4e8521911");
        assertGolden(report(unattributableMembers(missing), unattributableOutcomes(missing), List.of(), List.of()),
                "032b43470df6b1e122754c8941f7b99506adeb392e4cfdf0fef0b6c6de14f998");
        assertGolden(mixed, "7ee17b313888202cdc56f426b84cfb9256115d062ac67de168f9e87fb8ce1621");
        assertGolden(multiple, "8b8e9d4d7d2c6a2f8caa7dfc9be0fe01b2e6339f65c80c37f41aea2f475e63a3");
        assertGolden(report(List.of(), List.of(), unsupported, List.of()),
                "e4b303ca3f3f18eb11cdba2e73003734cf78a1b802fa1d020b4442449f2c167e");
        assertEquals("51414950005343454e4152494f5f415554484f524954595f5245504f5349544f52595f44455249564154494f4e005631",
                HEX.formatHex(RepositoryDerivationReportFingerprintEncoder.DOMAIN.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsMissingExtraDuplicateAndWrongOrderMembers() {
        var first = unattributable("a.scenario.json", "INVALID_UTF8", "PARSE_UNATTRIBUTABLE", Optional.empty());
        var second = unattributable("b.scenario.json", "MALFORMED_JSON", "PARSE_UNATTRIBUTABLE", Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> report(
                List.of(first.parentMember()), List.of(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> report(
                List.of(first.parentMember()), List.of(first.outcome(), second.outcome()), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> report(
                List.of(first.parentMember(), first.parentMember()),
                List.of(first.outcome(), first.outcome()), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> report(
                List.of(first.parentMember(), second.parentMember()),
                List.of(second.outcome(), first.outcome()), List.of(), List.of()));
        var otherCapture = member("other", "capture:2", "a.scenario.json");
        assertThrows(IllegalArgumentException.class, () -> parent(List.of(otherCapture), List.of()));
    }

    @Test
    void rejectsTagAndUnattributableAuthorityViolations() {
        var parent = member("a.scenario.json");
        assertThrows(IllegalArgumentException.class, () ->
                new RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained(
                        parent, "PARSED", "ATTRIBUTED", Optional.of("/authority")));
        assertThrows(IllegalArgumentException.class, () ->
                new RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained(
                        parent, "INVALID_UTF8", "INVALID_AUTHORITY", Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                new RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained(
                        parent, "PARSED", "MISSING_AUTHORITY", Optional.of("not-a-pointer")));
        var attributed = attributed("a.scenario.json", true, "orders");
        assertThrows(IllegalArgumentException.class, () ->
                new RepositoryDerivationReportFingerprintInput.AttributedMemberRetained(
                        attributed.parentMember(), "guessed-authority",
                        attributed.outcome().attributedMemberOutput()));
    }

    @Test
    void rejectsMissingExtraDuplicateWrongOrUnattributableProvenance() {
        var first = attributed("a.scenario.json", true, "orders");
        var second = attributed("b.scenario.json", true, "payments");
        assertThrows(IllegalArgumentException.class, () -> report(
                attributedMembers(first), outcomes(first), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> report(
                attributedMembers(first), outcomes(first), List.of(),
                List.of(first.provenance(), first.provenance())));
        assertThrows(IllegalArgumentException.class, () -> report(
                List.of(first.parentMember(), second.parentMember()),
                List.of(first.outcome(), second.outcome()), List.of(),
                List.of(second.provenance(), first.provenance())));
        var unattributable = unattributable("a.scenario.json", "INVALID_UTF8", "PARSE_UNATTRIBUTABLE", Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> report(
                unattributableMembers(unattributable), unattributableOutcomes(unattributable), List.of(),
                List.of(first.provenance())));
        assertThrows(IllegalArgumentException.class, () -> report(
                attributedMembers(first), outcomes(first), List.of(), List.of(second.provenance())));
    }

    @Test
    void rejectsUnsupportedEntryChangesAndUnsupportedContracts() {
        var first = new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                "a.scenario.json", "DIRECTORY", "UNSUPPORTED_DIRECTORY");
        var second = new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                "b.scenario.json", "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK");
        var parent = parent(List.of(), List.of(first, second));
        assertThrows(IllegalArgumentException.class, () -> input(parent, List.of(), List.of(first), List.of(),
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION));
        assertThrows(IllegalArgumentException.class, () -> input(parent, List.of(), List.of(first, second,
                new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                        "c.scenario.json", "DIRECTORY", "UNSUPPORTED_DIRECTORY")), List.of(),
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION));
        assertThrows(IllegalArgumentException.class, () -> input(parent, List.of(), List.of(second, first), List.of(),
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION));
        assertThrows(IllegalArgumentException.class, () -> report(List.of(), List.of(), List.of(
                new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                        "a.scenario.json", "DIRECTORY", "UNSUPPORTED_SYMBOLIC_LINK")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> input(parent, List.of(), List.of(first, second), List.of(),
                "scenario-authority-repository-derivation-report-v2"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryDerivationReportFingerprintInput(
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION, parent, "parser-v2",
                RepositoryDerivationReportFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                List.of(), List.of(first, second), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryDerivationReportFingerprintInput(
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION, parent,
                RepositoryDerivationReportFingerprintInput.PARSER_CONTRACT_IDENTIFIER, "attribution-v2",
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                List.of(), List.of(first, second), List.of()));
    }

    @Test
    void semanticChangesAffectFingerprintAndCalculationIsDeterministic() {
        var first = unattributable("a.scenario.json", "PARSED", "MISSING_AUTHORITY", Optional.of(""));
        var base = report(unattributableMembers(first), unattributableOutcomes(first), List.of(), List.of());
        assertArrayEquals(RepositoryDerivationReportFingerprintEncoder.encode(base),
                RepositoryDerivationReportFingerprintEncoder.encode(base));
        assertEquals(fingerprint(base), fingerprint(base));
        var changed = unattributable("a.scenario.json", "PARSED", "INVALID_AUTHORITY", Optional.of("/authority"));
        assertNotEquals(fingerprint(base), fingerprint(report(
                unattributableMembers(changed), unattributableOutcomes(changed), List.of(), List.of())));

        var second = unattributable("b.scenario.json", "MALFORMED_JSON", "PARSE_UNATTRIBUTABLE", Optional.empty());
        var forward = report(List.of(first.parentMember(), second.parentMember()),
                List.of(first.outcome(), second.outcome()), List.of(), List.of());
        var reverseParent = parent(List.of(second.parentMember(), first.parentMember()), List.of());
        var reverse = input(reverseParent, List.of(second.outcome(), first.outcome()), List.of(), List.of(),
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION);
        assertNotEquals(fingerprint(forward), fingerprint(reverse));

        var directory = List.of(new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                "z.scenario.json", "DIRECTORY", "UNSUPPORTED_DIRECTORY"));
        var link = List.of(new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                "z.scenario.json", "SYMBOLIC_LINK", "UNSUPPORTED_SYMBOLIC_LINK"));
        assertNotEquals(fingerprint(report(List.of(), List.of(), directory, List.of())),
                fingerprint(report(List.of(), List.of(), link, List.of())));
    }

    private static void assertGolden(RepositoryDerivationReportFingerprintInput input, String digest) {
        byte[] canonicalBytes = RepositoryDerivationReportFingerprintEncoder.encode(input);
        assertEquals(RepositoryDerivationReportFingerprint.VALUE_PREFIX + digest, fingerprint(input).value());
        assertEquals(digest, CanonicalSha256.lowercaseHexDigest(canonicalBytes));
    }

    private static RepositoryDerivationReportFingerprintInput report(
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> members,
            List<RepositoryDerivationReportFingerprintInput.MemberOutcome> outcomes,
            List<RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry> unsupported,
            List<SemanticProvenanceAttestation> provenance
    ) {
        return input(parent(members, unsupported), outcomes, unsupported, provenance,
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION);
    }

    private static RepositoryDerivationReportFingerprintInput input(
            RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture parent,
            List<RepositoryDerivationReportFingerprintInput.MemberOutcome> outcomes,
            List<RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry> unsupported,
            List<SemanticProvenanceAttestation> provenance,
            String contract
    ) {
        return new RepositoryDerivationReportFingerprintInput(
                contract, parent,
                RepositoryDerivationReportFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                outcomes, unsupported, provenance);
    }

    private static RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture parent(
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> members,
            List<RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry> unsupported
    ) {
        return new RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture(
                "repository:orders", "capture:17", CAPTURE, members, unsupported);
    }

    private static AttributedFixture attributed(String path, boolean admitted, String authority) {
        var parent = member(path);
        AttributedMemberOutcomeFingerprintInput attributedInput;
        if (admitted) {
            var occurrence = new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                    parent.parentSourceId(), parent.parentSnapshotId(), parent.parentContentFingerprint(),
                    parent.normalizedRepositoryRelativePath(),
                    AttributedMemberOutcomeFingerprintInput.MANIFEST_OCCURRENCE_IDENTITY_VERSION);
            attributedInput = attributedInput(parent, authority,
                    AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                    List.of(), Optional.of(occurrence), Optional.of(new ManifestSemanticFingerprint(
                            ManifestSemanticFingerprint.VALUE_PREFIX + "c".repeat(64))));
        } else {
            attributedInput = attributedInput(parent, authority,
                    AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                    List.of(REQUIRED), Optional.empty(), Optional.empty());
        }
        var output = SemanticProvenanceOutputReference.verified(
                attributedInput, AttributedMemberOutcomeFingerprintEncoder.fingerprint(attributedInput));
        var provenance = SemanticProvenanceAttestation.deriveAttributedMemberOutcome(output);
        var outcome = new RepositoryDerivationReportFingerprintInput.AttributedMemberRetained(
                parent, authority, output);
        return new AttributedFixture(parent, outcome, provenance);
    }

    private static UnattributableFixture unattributable(
            String path, String parse, String attribution, Optional<String> location
    ) {
        var parent = member(path);
        return new UnattributableFixture(parent,
                new RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained(
                        parent, parse, attribution, location));
    }

    private static AttributedMemberOutcomeFingerprintInput attributedInput(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent,
            String authority,
            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState state,
            List<ScenarioSchemaDiagnostic> diagnostics,
            Optional<AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity> occurrence,
            Optional<ManifestSemanticFingerprint> manifest
    ) {
        return new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER, parent, authority,
                AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,
                state, diagnostics, occurrence, manifest);
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference member(String path) {
        return member("repository:orders", "capture:17", path);
    }

    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference member(
            String source, String snapshot, String path
    ) {
        return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                source, snapshot, CAPTURE, path, 3,
                RawSourceMemberFingerprint.calculate(path.substring(0, Math.min(3, path.length()))
                        .getBytes(StandardCharsets.UTF_8)));
    }

    private static RepositoryDerivationReportFingerprint fingerprint(
            RepositoryDerivationReportFingerprintInput input
    ) {
        return RepositoryDerivationReportFingerprintEncoder.fingerprint(input);
    }

    private static List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> attributedMembers(AttributedFixture item) { return List.of(item.parentMember()); }
    private static List<RepositoryDerivationReportFingerprintInput.MemberOutcome> outcomes(AttributedFixture item) { return List.of(item.outcome()); }
    private static List<SemanticProvenanceAttestation> provenance(AttributedFixture item) { return List.of(item.provenance()); }
    private static List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> unattributableMembers(UnattributableFixture item) { return List.of(item.parentMember()); }
    private static List<RepositoryDerivationReportFingerprintInput.MemberOutcome> unattributableOutcomes(UnattributableFixture item) { return List.of(item.outcome()); }

    private record AttributedFixture(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            RepositoryDerivationReportFingerprintInput.AttributedMemberRetained outcome,
            SemanticProvenanceAttestation provenance) {}
    private record UnattributableFixture(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained outcome) {}
}
