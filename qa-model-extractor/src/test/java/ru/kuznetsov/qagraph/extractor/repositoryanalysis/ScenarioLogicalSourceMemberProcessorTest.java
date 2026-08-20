package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributorV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityExactJsonParserV1;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioLogicalSourceMemberProcessorTest {
    private static final String SOURCE_ID = "repository:orders";
    private static final String SNAPSHOT_ID = "capture:17";
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1",
                    "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1",
                    "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1",
                    RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    private final ScenarioLogicalSourceMemberProcessor processor = new ScenarioLogicalSourceMemberProcessor();

    @Test
    void extractorAttributionIsAnExactProjectionOfEvidenceGovernance() {
        var authoritativeParser = new ScenarioAuthorityExactJsonParserV1();
        var authoritativeAttributor = new ScenarioAuthorityAttributorV1();
        for (String json : List.of("[]", "{}", "{\"authority\":null}", "{\"authority\":\" bad\"}")) {
            var rejection = assertThrows(ScenarioAuthorityAttributionRejectionV1.class,
                    () -> authoritativeAttributor.attribute(authoritativeParser.parseExactBytes(bytes(json))));
            var projected = assertInstanceOf(UnattributableMemberProcessingOutcome.class,
                    onlyOutcome(candidate(member("projection", json))));
            assertEquals(rejection.structuralLocation(), projected.structuralLocation().orElse(""));
            assertEquals(rejection.code().name(), projected.attributionOutcome().name());
        }
        String valid = "{\"authority\":\"Orders/V1\"}";
        String authority = authoritativeAttributor.attribute(
                authoritativeParser.parseExactBytes(bytes(valid))).authority();
        var projected = assertInstanceOf(AttributedMemberProcessingOutcome.class,
                onlyOutcome(candidate(member("projection-valid", valid))));
        assertEquals(authority, projected.claimedAuthority());
    }

    @Test
    void freezesParserAttributionAndStructuralLocationContracts() {
        assertEquals("scenario-authority-json-parser-v1",
                ScenarioLogicalSourceMemberProcessor.PARSER_CONTRACT_IDENTIFIER);
        assertEquals("scenario-authority-attribution-v1",
                ScenarioLogicalSourceMemberProcessor.ATTRIBUTION_CONTRACT_IDENTIFIER);
        assertEquals("rfc-6901-json-pointer-v1",
                ScenarioLogicalSourceMemberProcessor.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER);
    }

    @Test
    void attributesValidManifestAndRetainsSafeParsedRepresentation() {
        ScenarioMemberProcessingOutcome outcome = onlyOutcome(candidate(member("valid", validManifest())));

        AttributedMemberProcessingOutcome attributed = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, outcome);
        assertEquals("order-scenarios", attributed.claimedAuthority());
        assertEquals(ScenarioMemberProcessingOutcome.ParseOutcome.PARSED, attributed.parseOutcome());
        assertEquals(ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED,
                attributed.attributionOutcome());
        assertEquals("qaip-scenario-authority-manifest-v1",
                attributed.parsedSource().path("format").asText());
    }

    @Test
    void invalidUtf8IsParseUnattributable() {
        assertUnattributable(new byte[]{(byte) 0xc3, 0x28},
                ScenarioMemberProcessingOutcome.ParseOutcome.INVALID_UTF8,
                ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                Optional.empty());
    }

    @Test
    void malformedJsonIsParseUnattributable() {
        assertUnattributable(bytes("{\"authority\":"),
                ScenarioMemberProcessingOutcome.ParseOutcome.MALFORMED_JSON,
                ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                Optional.empty());
    }

    @Test
    void duplicateJsonMemberIsParseUnattributable() {
        assertUnattributable(bytes("{\"authority\":\"one\",\"authority\":\"two\"}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.DUPLICATE_JSON_MEMBER,
                ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                Optional.empty());
    }

    @Test
    void trailingJsonIsParseUnattributable() {
        assertUnattributable(bytes("{\"authority\":\"orders\"} {}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.TRAILING_JSON_CONTENT,
                ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                Optional.empty());
    }

    @Test
    void nonObjectRootIsUnattributableAtRootPointer() {
        assertUnattributable(bytes("[]"),
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.NON_OBJECT_ROOT,
                Optional.of(""));
    }

    @Test
    void missingAuthorityIsUnattributableAtAuthorityPointer() {
        assertUnattributable(bytes("{}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.MISSING_AUTHORITY,
                Optional.of("/authority"));
    }

    @Test
    void nonStringAuthorityIsUnattributableAtAuthorityPointer() {
        assertUnattributable(bytes("{\"authority\":17}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.AUTHORITY_NOT_STRING,
                Optional.of("/authority"));
    }

    @Test
    void invalidAuthorityIsNotTrimmedOrReinterpreted() {
        assertUnattributable(bytes("{\"authority\":\" orders \"}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.INVALID_AUTHORITY,
                Optional.of("/authority"));
    }

    @Test
    void authorityLexicalBoundaryMatchesFrozenManifestContract() {
        String maximum = "a".repeat(200);
        AttributedMemberProcessingOutcome attributed = assertInstanceOf(
                AttributedMemberProcessingOutcome.class,
                onlyOutcome(candidate(member("maximum", "{\"authority\":\"" + maximum + "\"}"))));
        assertEquals(maximum, attributed.claimedAuthority());

        assertUnattributable(bytes("{\"authority\":\"" + "a".repeat(201) + "\"}"),
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.INVALID_AUTHORITY,
                Optional.of("/authority"));
    }

    @Test
    void schemaInvalidButSafelyReadableAuthorityIsAttributed() {
        AttributedMemberProcessingOutcome attributed = assertInstanceOf(
                AttributedMemberProcessingOutcome.class,
                onlyOutcome(candidate(member("schema-invalid",
                        "{\"authority\":\"orders\",\"unknown\":true}"))));

        assertEquals("orders", attributed.claimedAuthority());
        assertEquals(ScenarioMemberProcessingOutcome.ParseOutcome.PARSED, attributed.parseOutcome());
    }

    @Test
    void mixedMembersAllProduceOutcomesInExactParentOrderWithoutFailFast() {
        ScenarioRepositoryCaptureSnapshotCandidate parent = candidate(
                member("a-invalid", "not-json"),
                member("b-attributed", "{\"authority\":\"orders\"}"),
                member("c-missing", "{}"));

        ScenarioLogicalSourceProcessingResult result = processor.process(parent);

        assertEquals(List.of(
                        ".qaip/scenarios/a-invalid.scenario.json",
                        ".qaip/scenarios/b-attributed.scenario.json",
                        ".qaip/scenarios/c-missing.scenario.json"),
                result.memberOutcomes().stream()
                        .map(value -> value.parentMemberRef().normalizedRepositoryRelativePath())
                        .toList());
        assertEquals(List.of(
                        ScenarioMemberProcessingOutcome.ParseOutcome.MALFORMED_JSON,
                        ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                        ScenarioMemberProcessingOutcome.ParseOutcome.PARSED),
                result.memberOutcomes().stream().map(ScenarioMemberProcessingOutcome::parseOutcome).toList());
        assertInstanceOf(AttributedMemberProcessingOutcome.class, result.memberOutcomes().get(1));
        assertEquals(parent.identity(), result.parentIdentity());
    }

    @Test
    void parentReferenceContainsAndVerifiesEveryExactBindingField() {
        byte[] exact = {(byte) 0xff, 0x00, 0x0a};
        ScenarioRepositoryCaptureSnapshotCandidate parent = candidate(member("binary", exact));
        ParentCapturedMemberRef reference = onlyOutcome(parent).parentMemberRef();

        assertEquals(parent.sourceId(), reference.parentSourceId());
        assertEquals(parent.snapshotId(), reference.parentSnapshotId());
        assertEquals(parent.contentFingerprint(), reference.parentContentFingerprint());
        assertEquals(".qaip/scenarios/binary.scenario.json", reference.normalizedRepositoryRelativePath());
        assertEquals(exact.length, reference.rawByteLength());
        assertEquals(RawSourceMemberFingerprint.calculate(exact), reference.rawMemberFingerprint());
        reference.verifyAgainst(parent);

        ParentCapturedMemberRef substituted = new ParentCapturedMemberRef(
                reference.parentSourceId(),
                reference.parentSnapshotId(),
                reference.parentContentFingerprint(),
                reference.normalizedRepositoryRelativePath(),
                reference.rawByteLength() + 1,
                reference.rawMemberFingerprint());
        assertThrows(IllegalArgumentException.class, () -> substituted.verifyAgainst(parent));
    }

    @Test
    void processingUsesOnlyDefensivelyRetainedParentBytes() {
        byte[] source = bytes("{\"authority\":\"orders\"}");
        ScenarioRepositoryCaptureSnapshotCandidate parent = candidate(member("retained", source));
        source[0] = '[';

        AttributedMemberProcessingOutcome outcome = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, onlyOutcome(parent));

        assertEquals("orders", outcome.claimedAuthority());
        assertArrayEquals(bytes("{\"authority\":\"orders\"}"), parent.members().getFirst().bytes());
    }

    @Test
    void resultCollectionsAndParsedDocumentsAreImmutable() {
        ScenarioLogicalSourceProcessingResult result = processor.process(candidate(
                member("immutable", "{\"authority\":\"orders\",\"nested\":{\"value\":1}}")));
        AttributedMemberProcessingOutcome attributed = assertInstanceOf(
                AttributedMemberProcessingOutcome.class, result.memberOutcomes().getFirst());
        ObjectNode exposed = (ObjectNode) attributed.parsedSource();
        ((ObjectNode) exposed.path("nested")).put("value", 99);

        assertEquals(1, attributed.parsedSource().path("nested").path("value").asInt());
        assertThrows(UnsupportedOperationException.class, () -> result.memberOutcomes().clear());
    }

    @Test
    void outcomeContractsRejectInconsistentStatesAndInvalidStructuralLocations() {
        ScenarioRepositoryCaptureSnapshotCandidate parent = candidate(member("one", "{}"));
        ParentCapturedMemberRef reference = ParentCapturedMemberRef.from(parent, parent.members().getFirst());

        assertThrows(IllegalArgumentException.class, () -> new UnattributableMemberProcessingOutcome(
                reference,
                ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.INVALID_UTF8,
                ScenarioMemberProcessingOutcome.AttributionOutcome.MISSING_AUTHORITY,
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new UnattributableMemberProcessingOutcome(
                reference,
                ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.MISSING_AUTHORITY,
                Optional.of("$.authority")));
    }

    private void assertUnattributable(
            byte[] content,
            ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome,
            ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome,
            Optional<String> structuralLocation
    ) {
        UnattributableMemberProcessingOutcome outcome = assertInstanceOf(
                UnattributableMemberProcessingOutcome.class,
                onlyOutcome(candidate(member("input", content))));
        assertEquals(parseOutcome, outcome.parseOutcome());
        assertEquals(attributionOutcome, outcome.attributionOutcome());
        assertEquals(structuralLocation, outcome.structuralLocation());
        assertEquals(ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                outcome.parserContractIdentifier());
        assertEquals(ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                outcome.attributionContractIdentifier());
    }

    private ScenarioMemberProcessingOutcome onlyOutcome(
            ScenarioRepositoryCaptureSnapshotCandidate parent
    ) {
        ScenarioLogicalSourceProcessingResult result = processor.process(parent);
        assertEquals(1, result.memberOutcomes().size());
        return result.memberOutcomes().getFirst();
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult.CapturedMember... members
    ) {
        List<ScenarioManifestStableCaptureResult.CapturedMember> ordered =
                java.util.Arrays.stream(members)
                        .sorted(java.util.Comparator.comparing(
                                ScenarioManifestStableCaptureResult.CapturedMember::repositoryRelativePath))
                        .toList();
        ScenarioManifestStableCaptureResult.Completed capture = new ScenarioManifestStableCaptureResult.Completed(
                ordered, List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(
                capture,
                SOURCE_ID,
                SNAPSHOT_ID,
                CONTRACT,
                ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, String json) {
        return member(name, bytes(json));
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, byte[] exactBytes) {
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + name + ".scenario.json",
                exactBytes,
                exactBytes.length,
                RawSourceMemberFingerprint.calculate(exactBytes));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String validManifest() {
        return """
                {
                  "format":"qaip-scenario-authority-manifest-v1",
                  "schemaVersion":"1.0",
                  "authority":"order-scenarios",
                  "scenarioIdentityScheme":"qaip-scenario-identity-v1",
                  "scenarios":[]
                }
                """;
    }
}
