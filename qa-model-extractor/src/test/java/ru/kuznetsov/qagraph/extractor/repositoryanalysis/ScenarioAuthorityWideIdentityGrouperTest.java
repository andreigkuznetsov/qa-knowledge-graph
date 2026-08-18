package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;

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
    void oneScenarioProducesOneUniqueExactIdentity() {
        ScenarioIdentityGroup group = group(member("one", manifest("orders", scenario("CREATE", "Create", "g"))))
                .identityGroups().getFirst();

        assertEquals(UNIQUE, group.state());
        assertEquals("orders", group.claimedScenarioIdentity().authority());
        assertEquals("CREATE", group.claimedScenarioIdentity().scenarioKey());
        assertEquals("qaip-scenario-identity-v1", group.claimedScenarioIdentity().identityScheme());
        assertEquals(1, group.occurrences().size());
    }

    @Test
    void duplicatesWithinManifestRetainEveryOccurrenceWithoutContentClassification() {
        ScenarioIdentityGroup group = group(member("one", manifest("orders",
                scenario("SAME", "First", "same") + ','
                        + scenario("SAME", "Second", "same") + ','
                        + scenario("SAME", "Different", "different"))))
                .identityGroups().getFirst();

        assertEquals(DUPLICATE_UNCLASSIFIED, group.state());
        assertEquals(3, group.occurrences().size());
        assertEquals(List.of("/scenarios/0", "/scenarios/1", "/scenarios/2"),
                group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::structuralLocation).toList());
        assertEquals(3, group.occurrences().stream().map(ScenarioIdentityGroup.Occurrence::occurrenceIdentity)
                .distinct().count());
    }

    @Test
    void duplicateAcrossManifestsRetainsExactParentBindingsInParentOrder() {
        ScenarioIdentityGroup group = group(
                member("a", manifest("orders", scenario("SAME", "First", "g"))),
                member("b", manifest("orders", scenario("SAME", "Second", "other"))))
                .identityGroups().getFirst();

        assertEquals(DUPLICATE_UNCLASSIFIED, group.state());
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
        assertTrue(result.identityGroups().stream().allMatch(value -> value.state() == DUPLICATE_UNCLASSIFIED));
        assertTrue(result.identityGroups().stream().allMatch(value -> value.occurrences().size() == 2));
    }

    @Test
    void rejectedAndUnattributableOutcomesRemainExactAndDoNotParticipateAndResultIsImmutable() {
        String rejected = manifest("orders", scenarioWithGiven("REJECTED", "[]"));
        ScenarioSourceNormalizationResult normalization = normalize(
                member("admitted", manifest("orders", scenario("GOOD", "Good", "g"))),
                member("rejected", rejected), member("broken", "not-json"));

        ScenarioAuthorityWideIdentityGroupingResult result = grouper.group(normalization);

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

    private static ScenarioSourceNormalizationResult normalize(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        var processor = new ScenarioLogicalSourceMemberProcessor();
        var admission = new ScenarioLogicalSourceSchemaAdmission();
        var normalizer = new ScenarioSourceDeclarationNormalizer();
        return normalizer.normalize(admission.admit(processor.process(candidate(members))));
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
        return scenarioWithGiven(key, "[\"" + given + "\"]").replace("\"Title\"", "\"" + title + "\"");
    }

    private static String scenarioWithGiven(String key, String given) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"Title\",\"given\":" + given +
                ",\"when\":[\"act\"],\"then\":[\"done\"],\"operationRef\":{" +
                "\"identityScheme\":\"qaip-http-operation-reference-v1\",\"method\":\"POST\"," +
                "\"path\":\"/api/test\"},\"ruleRefs\":[]}";
    }
}
