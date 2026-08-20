package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityExactJsonParserV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityJsonParseRejectionV1;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioManifestJsonParserTest {
    private final ScenarioManifestJsonParser parser = new ScenarioManifestJsonParser();

    @Test
    void parsesValidJsonAndRetainsCapturedSource() {
        ScenarioManifestCaptureResult.CapturedMember source = member(
                "valid.scenario.json", "{\"format\":\"qaip-scenario-authority-manifest-v1\"}");

        ScenarioManifestJsonParseResult.Completed result = completed(capture(source));

        assertEquals(source, result.members().getFirst().source());
        assertEquals("qaip-scenario-authority-manifest-v1",
                result.members().getFirst().document().path("format").asText());
    }

    @Test
    void decodesUtf8ContentExactly() {
        String title = "Заказ готов 🚀";
        ScenarioManifestCaptureResult.CapturedMember source = member(
                "utf8.scenario.json", "{\"title\":\"" + title + "\"}");

        ScenarioManifestJsonParseResult.Completed result = completed(capture(source));

        assertEquals(title, result.members().getFirst().document().path("title").asText());
    }

    @Test
    void preservesCaptureOrderingAcrossMultipleMembers() {
        var first = member("z.scenario.json", "{\"position\":1}");
        var second = member("a.scenario.json", "{\"position\":2}");

        ScenarioManifestJsonParseResult.Completed result = completed(capture(first, second));

        assertEquals(List.of(
                        ".qaip/scenarios/z.scenario.json",
                        ".qaip/scenarios/a.scenario.json"),
                result.members().stream()
                        .map(value -> value.source().repositoryRelativePath())
                        .toList());
        assertEquals(List.of(1, 2), result.members().stream()
                .map(value -> value.document().path("position").asInt())
                .toList());
    }

    @Test
    void malformedJsonReturnsDeterministicMemberFailure() {
        ScenarioManifestJsonParseResult.Failed result = failed(capture(
                member("broken.scenario.json", "{\"title\":")));

        assertEquals(ScenarioManifestJsonParseResult.Code.MALFORMED_JSON, result.failure().code());
        assertEquals(".qaip/scenarios/broken.scenario.json", result.failure().repositoryRelativePath());
        assertEquals("Scenario manifest member is not a syntactically valid JSON value: "
                        + ".qaip/scenarios/broken.scenario.json",
                result.failure().message());
    }

    @Test
    void invalidUtf8ReturnsDeterministicMemberFailure() {
        var source = new ScenarioManifestCaptureResult.CapturedMember(
                Path.of("invalid.scenario.json"),
                ".qaip/scenarios/invalid.scenario.json",
                new byte[]{(byte) 0xC3, 0x28});

        ScenarioManifestJsonParseResult.Failed result = failed(capture(source));

        assertEquals(ScenarioManifestJsonParseResult.Code.INVALID_UTF8, result.failure().code());
        assertEquals(".qaip/scenarios/invalid.scenario.json", result.failure().repositoryRelativePath());
    }

    @Test
    void emptyCaptureProducesCompletedEmptyParseResult() {
        ScenarioManifestJsonParseResult.Completed result = completed(capture());

        assertEquals(List.of(), result.members());
    }

    @Test
    void failureOnLaterMemberDoesNotExposePartialSuccess() {
        ScenarioManifestJsonParseResult result = parser.parse(capture(
                member("first.scenario.json", "{\"valid\":true}"),
                member("second.scenario.json", "not-json")));

        ScenarioManifestJsonParseResult.Failed failed = assertInstanceOf(
                ScenarioManifestJsonParseResult.Failed.class, result);
        assertEquals(".qaip/scenarios/second.scenario.json",
                failed.failure().repositoryRelativePath());
    }

    @Test
    void parsedStructureAndCollectionsAreImmutable() {
        ScenarioManifestJsonParseResult.Completed result = completed(capture(
                member("immutable.scenario.json", "{\"nested\":{\"value\":1}}")));

        ObjectNode exposed = (ObjectNode) result.members().getFirst().document();
        ((ObjectNode) exposed.path("nested")).put("value", 99);

        assertEquals(1, result.members().getFirst().document().path("nested").path("value").asInt());
        assertThrows(UnsupportedOperationException.class, () -> result.members().clear());
    }

    @Test
    void extractorRejectionCodesExactlyProjectEvidenceGovernanceForIdenticalBytes() {
        var authorityParser = new ScenarioAuthorityExactJsonParserV1();
        List<byte[]> fixtures = List.of(
                new byte[]{(byte) 0xC3, 0x28},
                "{".getBytes(StandardCharsets.UTF_8),
                "{\"value\":1,\"value\":2}".getBytes(StandardCharsets.UTF_8),
                "{} {}".getBytes(StandardCharsets.UTF_8));

        for (byte[] fixture : fixtures) {
            ScenarioAuthorityJsonParseRejectionV1 authoritative = assertThrows(
                    ScenarioAuthorityJsonParseRejectionV1.class,
                    () -> authorityParser.parseExactBytes(fixture));
            ScenarioManifestJsonParseResult.Failed projection = failed(capture(
                    new ScenarioManifestCaptureResult.CapturedMember(
                            Path.of("fixture.scenario.json"),
                            ".qaip/scenarios/fixture.scenario.json",
                            fixture)));

            assertEquals(authoritative.code().name(), projection.failure().code().name());
        }
    }

    private ScenarioManifestCaptureResult.Completed capture(
            ScenarioManifestCaptureResult.CapturedMember... members) {
        return new ScenarioManifestCaptureResult.Completed(List.of(members));
    }

    private ScenarioManifestCaptureResult.CapturedMember member(String fileName, String json) {
        return new ScenarioManifestCaptureResult.CapturedMember(
                Path.of(fileName),
                ".qaip/scenarios/" + fileName,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private ScenarioManifestJsonParseResult.Completed completed(
            ScenarioManifestCaptureResult.Completed capture) {
        return assertInstanceOf(ScenarioManifestJsonParseResult.Completed.class, parser.parse(capture));
    }

    private ScenarioManifestJsonParseResult.Failed failed(
            ScenarioManifestCaptureResult.Completed capture) {
        return assertInstanceOf(ScenarioManifestJsonParseResult.Failed.class, parser.parse(capture));
    }
}
