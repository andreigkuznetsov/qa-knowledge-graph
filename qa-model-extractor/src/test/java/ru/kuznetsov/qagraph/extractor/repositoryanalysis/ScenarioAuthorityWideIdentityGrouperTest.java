package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioIdentityGroup.State.*;

class ScenarioAuthorityWideIdentityGrouperTest {
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    private final ScenarioAuthorityWideIdentityGrouper grouper = new ScenarioAuthorityWideIdentityGrouper();

    @Test
    void verifiedHandoffRejectsForeignCaptureAndAlteredOccurrenceMembership() {
        var first=member("a",manifest("orders",scenario("SAME","One","g")+','+scenario("SAME","Two","g")));
        var parentA=candidate(first); var normalization=normalizeAgainst(parentA);
        var parentB=ScenarioRepositoryCaptureSnapshotCandidate.create(new ScenarioManifestStableCaptureResult.Completed(
                parentA.members(),List.of(),RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION),
                parentA.sourceId(),"capture:foreign",CONTRACT,ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
        assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityNormalizedProcessingV1.verified(parentB,normalization,
                ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1()));
        var valid=ScenarioAuthorityNormalizedProcessingV1.verified(parentA,normalization,
                ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1());
        assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityNormalizedProcessingV1.verified(parentA,normalization,valid.contracts(),valid.occurrences().subList(0,1)));
        var reversed=new java.util.ArrayList<>(valid.occurrences());java.util.Collections.reverse(reversed);
        assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityNormalizedProcessingV1.verified(parentA,normalization,valid.contracts(),reversed));
        var extra=new java.util.ArrayList<>(valid.occurrences());extra.add(valid.occurrences().getFirst());
        assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityNormalizedProcessingV1.verified(parentA,normalization,valid.contracts(),extra));
        var original=valid.occurrences().getFirst();var p=original.parentMemberRef();
        var relabeled=new ParentCapturedMemberRef("repository:foreign",p.parentSnapshotId(),p.parentContentFingerprint(),p.normalizedRepositoryRelativePath(),p.rawByteLength(),p.rawMemberFingerprint());
        assertThrows(IllegalArgumentException.class,()->new ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding(relabeled,original.declaration()));
    }

    @Test
    void mapperPreservesUnsupportedHandoffIdentifierIntoStageTwo() {
        var parent=candidate(member("a",manifest("orders",scenarioDetailed("ONLY","One","g","POST","/api/test","["+rule("policy","one")+"]"))));var normalization=normalizeAgainst(parent);
        var future=new ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers("future-normalization-v2","future-scenario-c14n-v2","future-scenario-contract-v2","future-step-c14n-v2","future-operation-c14n-v2","future-rule-c14n-v2");
        var handoff=ScenarioAuthorityNormalizedProcessingV1.verified(parent,normalization,future);var mapper=new ScenarioOccurrenceInputMapperV1();
        var mapped=mapper.map(handoff,handoff.occurrences().getFirst());assertEquals("future-normalization-v2",mapped.sourceNormalizationVersion());
        assertEquals("future-scenario-c14n-v2",mapped.scenarioSemanticCanonicalizationVersion());
        assertEquals("future-scenario-contract-v2",mapped.scenarioSemanticContractVersion());
        assertEquals("future-step-c14n-v2",mapped.givenSteps().getFirst().semanticCanonicalizationVersion());
        assertEquals("future-operation-c14n-v2",mapped.operationReference().semanticCanonicalizationVersion());
        assertEquals("future-rule-c14n-v2",mapped.businessRuleReferences().getFirst().semanticCanonicalizationVersion());
        var outcome=mapper.attempt(handoff,handoff.occurrences().getFirst());
        assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)outcome).reason());
    }

    @Test
    void productionAuthorityAndLegacyProjectionHaveOneClassificationTruth() {
        var handoff=normalize(member("a",manifest("orders",scenario("SAME","One","g"))),member("b",manifest("orders",scenario("SAME","Two","different"))));
        var result=grouper.group(handoff);var authoritative=result.verifiedIdentityGroups().getFirst();var legacy=result.identityGroups().getFirst();
        assertEquals(authoritative.state().name(),legacy.state().name());
        var direct=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,authoritative.claimedIdentity(),authoritative.occurrences());
        assertEquals(direct.fingerprint(),authoritative.fingerprint());assertEquals(direct.state(),authoritative.state());
    }

    @Test
    void oneScenarioProducesOneUniqueExactIdentity() {
        ScenarioIdentityGroup group = group(member("one", manifest("orders", scenario("CREATE", "Create", "g"))))
                .identityGroups().getFirst();

        assertEquals(UNIQUE, group.state());
        assertEquals("orders", group.claimedScenarioIdentity().authority());
        assertEquals("CREATE", group.claimedScenarioIdentity().scenarioKey());
        assertEquals("qaip-scenario-identity-v1", group.claimedScenarioIdentity().identityScheme());
        assertEquals(1, group.occurrences().size());
        assertTrue(group.admissibleAsUniqueClaim());
        assertNotNull(group.occurrences().getFirst().semanticFingerprint());
        assertNull(group.occurrences().getFirst().unavailableReason());
    }

    @Test
    void conflictingDuplicatesWithinManifestRetainEveryOccurrenceWithoutWinner() {
        ScenarioIdentityGroup group = group(member("one", manifest("orders",
                scenario("SAME", "First", "same") + ','
                        + scenario("SAME", "Second", "same") + ','
                        + scenario("SAME", "Different", "different"))))
                .identityGroups().getFirst();

        assertEquals(DUPLICATE_CONFLICTING, group.state());
        assertEquals(3, group.occurrences().size());
        assertEquals(List.of("/scenarios/0", "/scenarios/1", "/scenarios/2"),
                group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::structuralLocation).toList());
        assertEquals(3, group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::occurrenceIdentity)
                .distinct().count());
        assertTrue(group.occurrences().stream().allMatch(ScenarioIdentityGroup.Occurrence::hasComparableSemanticFingerprint));
    }

    @Test
    void duplicateAcrossManifestsRetainsExactParentBindingsInParentOrder() {
        ScenarioIdentityGroup group = group(
                member("a", manifest("orders", scenario("SAME", "First", "g"))),
                member("b", manifest("orders", scenario("SAME", "Second", "other"))))
                .identityGroups().getFirst();

        assertEquals(DUPLICATE_CONFLICTING, group.state());
        assertEquals(List.of(".qaip/scenarios/a.scenario.json", ".qaip/scenarios/b.scenario.json"),
                group.occurrences().stream().map(value -> value.parentMemberRef()
                        .normalizedRepositoryRelativePath()).toList());
        group.occurrences().forEach(value -> {
            assertEquals(value.parentMemberRef().parentContentFingerprint(), value.occurrenceIdentity()
                    .manifestOccurrenceIdentity().parentContentFingerprint());
            assertEquals(value.parentMemberRef().normalizedRepositoryRelativePath(), value.occurrenceIdentity()
                    .manifestOccurrenceIdentity().normalizedRepositoryRelativePath());
            assertNotNull(value.parentMemberRef().rawMemberFingerprint());
        });
    }

    @Test
    void sameKeyUnderDifferentAuthoritiesIsNotDuplicateAndGroupsHaveDeterministicIdentityOrder() {
        ScenarioAuthorityWideIdentityGroupingResult result = group(
                member("z", manifest("zeta", scenario("SAME", "Z", "g"))),
                member("a2", manifest("alpha", scenario("Z", "Z", "g"))),
                member("a1", manifest("alpha", scenario("A", "A", "g"))),
                member("other", manifest("alpha", scenario("SAME", "A", "g"))));

        assertEquals(List.of("alpha:A", "alpha:SAME", "alpha:Z", "zeta:SAME"),
                result.identityGroups().stream().map(value -> value.claimedScenarioIdentity().authority()
                        + ':' + value.claimedScenarioIdentity().scenarioKey()).toList());
        assertTrue(result.identityGroups().stream().allMatch(value -> value.state() == UNIQUE));
    }

    @Test
    void multipleDuplicateGroupsAreIndependentAndHaveNoWinner() {
        ScenarioAuthorityWideIdentityGroupingResult result = group(member("one", manifest("orders",
                scenario("B", "First B", "g") + ',' + scenario("A", "First A", "g") + ','
                        + scenario("B", "Second B", "changed") + ','
                        + scenario("A", "Second A", "changed"))));

        assertEquals(List.of("A", "B"), result.identityGroups().stream()
                .map(value -> value.claimedScenarioIdentity().scenarioKey()).toList());
        assertTrue(result.identityGroups().stream().allMatch(value -> value.state() == DUPLICATE_CONFLICTING));
        assertTrue(result.identityGroups().stream().allMatch(value -> value.occurrences().size() == 2));
    }

    @Test
    void identicalDuplicatesAreEquivalentWithoutWinnerOrStrengthIncrease() {
        String declaration = scenario("SAME", "Same", "same");
        ScenarioIdentityGroup group = group(
                member("b", manifest("orders", declaration)),
                member("a", manifest("orders", declaration))).identityGroups().getFirst();

        assertEquals(DUPLICATE_EQUIVALENT, group.state());
        assertEquals(2, group.occurrences().size());
        assertEquals(1, group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::semanticFingerprint)
                .distinct().count());
        assertFalse(group.admissibleAsUniqueClaim());
        assertEquals(List.of(".qaip/scenarios/a.scenario.json", ".qaip/scenarios/b.scenario.json"),
                group.occurrences().stream().map(value -> value.parentMemberRef()
                        .normalizedRepositoryRelativePath()).toList());
    }

    @Test
    void titleStepOperationAndRuleMeaningDifferencesAreClassifiedOnlyByFingerprints() {
        assertConflicting(
                scenario("SAME", "Title one", "same"),
                scenario("SAME", "Title two", "same"));
        assertConflicting(
                scenario("SAME", "Same", "first"),
                scenario("SAME", "Same", "second"));
        assertConflicting(
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/one", "[]"),
                scenarioDetailed("SAME", "Same", "same", "PUT", "/api/two", "[]"));
        assertConflicting(
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("policy", "one") + "," + rule("fraud", "two") + "]"),
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("fraud", "two") + "," + rule("policy", "one") + "]"));
        assertConflicting(
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("policy", "one") + "]"),
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("policy", "changed") + "]"));
    }

    @Test
    void unsupportedLeafContractMakesDuplicateUnclassifiedAndRetainsStableReason() {
        ScenarioIdentityGroup group = group(member("one", manifest("orders",
                scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("policy", "one") + "]") + ','
                        + scenarioDetailed("SAME", "Same", "same", "POST", "/api/test",
                        "[" + rule("policy", "one", "future-rule-identity-v2") + "]"))))
                .identityGroups().getFirst();

        assertEquals(DUPLICATE_UNCLASSIFIED, group.state());
        assertEquals(2, group.occurrences().size());
        assertEquals(1, group.occurrences().stream().filter(
                ScenarioIdentityGroup.Occurrence::hasComparableSemanticFingerprint).count());
        assertEquals(ScenarioNormalizedSemanticFingerprinter.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,
                group.occurrences().stream().filter(value -> !value.hasComparableSemanticFingerprint())
                        .findFirst().orElseThrow().unavailableReason());
    }

    @Test
    void rejectedAndUnattributableOutcomesRemainExactAndDoNotParticipateAndResultIsImmutable() {
        String rejected = manifest("orders", scenarioWithGiven("REJECTED", "[]"));
        ScenarioAuthorityNormalizedProcessingV1 handoff = normalize(
                member("admitted", manifest("orders", scenario("GOOD", "Good", "g"))),
                member("rejected", rejected), member("broken", "not-json"));

        ScenarioAuthorityWideIdentityGroupingResult result = grouper.group(handoff);
        ScenarioSourceNormalizationResult normalization=handoff.normalizationResult();

        assertSame(normalization, result.normalizationResult());
        for (int index = 0; index < normalization.memberOutcomes().size(); index++) {
            if (!(normalization.memberOutcomes().get(index)
                    instanceof ScenarioSourceNormalizedRecords.NormalizedManifestDatum)) {
                assertSame(normalization.memberOutcomes().get(index), result.memberOutcomes().get(index));
            }
        }
        assertEquals(List.of("GOOD"), result.identityGroups().stream()
                .map(value -> value.claimedScenarioIdentity().scenarioKey()).toList());
        assertThrows(UnsupportedOperationException.class, () -> result.identityGroups().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.identityGroups().getFirst().occurrences().clear());
    }

    private ScenarioAuthorityWideIdentityGroupingResult group(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        return grouper.group(normalize(members));
    }

    private static ScenarioAuthorityNormalizedProcessingV1 normalize(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        var processor = new ScenarioLogicalSourceMemberProcessor();
        var admission = new ScenarioLogicalSourceSchemaAdmission();
        var normalizer = new ScenarioSourceDeclarationNormalizer();
        var parent=candidate(members);
        var normalization=normalizer.normalize(admission.admit(processor.process(parent)));
        return ScenarioAuthorityNormalizedProcessingV1.verified(parent,normalization,
                ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1());
    }

    private static ScenarioSourceNormalizationResult normalizeAgainst(ScenarioRepositoryCaptureSnapshotCandidate parent){
        return new ScenarioSourceDeclarationNormalizer().normalize(new ScenarioLogicalSourceSchemaAdmission().admit(new ScenarioLogicalSourceMemberProcessor().process(parent)));
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        var orderedMembers = List.of(members).stream()
                .sorted((left, right) -> left.repositoryRelativePath().compareTo(right.repositoryRelativePath()))
                .toList();
        var capture = new ScenarioManifestStableCaptureResult.Completed(
                orderedMembers, List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(capture, "repository:test", "capture:18",
                CONTRACT, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + name + ".scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
    }

    private static String manifest(String authority, String scenarios) {
        return "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\"," +
                "\"authority\":\"" + authority + "\",\"scenarioIdentityScheme\":" +
                "\"qaip-scenario-identity-v1\",\"scenarios\":[" + scenarios + "]}";
    }

    private static String scenario(String key, String title, String given) {
        return scenarioDetailed(key, title, given, "POST", "/api/test", "[]");
    }

    private void assertConflicting(String first, String second) {
        ScenarioIdentityGroup group = group(member("one", manifest("orders", first + ',' + second)))
                .identityGroups().getFirst();
        assertEquals(DUPLICATE_CONFLICTING, group.state());
        assertEquals(2, group.occurrences().size());
        assertEquals(2, group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::semanticFingerprint)
                .distinct().count());
    }

    private static String scenarioDetailed(String key, String title, String given,
                                           String method, String path, String rules) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"" + title
                + "\",\"given\":[\"" + given + "\"],\"when\":[\"act\"],\"then\":[\"done\"],"
                + "\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\","
                + "\"method\":\"" + method + "\",\"path\":\"" + path + "\"},\"ruleRefs\":" + rules + "}";
    }

    private static String rule(String authority, String key) {
        return rule(authority, key, "qaip-business-rule-identity-v1");
    }

    private static String rule(String authority, String key, String identityScheme) {
        return "{\"authority\":\"" + authority + "\",\"stableRuleKey\":\"" + key
                + "\",\"identityScheme\":\"" + identityScheme + "\"}";
    }

    private static String scenarioWithGiven(String key, String given) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"Title\",\"given\":" + given +
                ",\"when\":[\"act\"],\"then\":[\"done\"],\"operationRef\":{" +
                "\"identityScheme\":\"qaip-http-operation-reference-v1\",\"method\":\"POST\"," +
                "\"path\":\"/api/test\"},\"ruleRefs\":[]}";
    }
}
