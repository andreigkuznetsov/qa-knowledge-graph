package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScenarioManifestJsonParser {
    private static final String STRICT_DUPLICATE_MESSAGE_PREFIX = "Duplicate field '";

    private final ObjectMapper mapper;

    public ScenarioManifestJsonParser() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        mapper = new ObjectMapper(factory)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    public ScenarioManifestJsonParseResult parse(ScenarioManifestCaptureResult.Completed capture) {
        Objects.requireNonNull(capture, "capture");
        List<ScenarioManifestJsonParseResult.ParsedMember> parsed = new ArrayList<>();

        for (ScenarioManifestCaptureResult.CapturedMember member : capture.members()) {
            MemberParse memberParse = parseMember(member);
            if (memberParse.failure() != null) {
                return new ScenarioManifestJsonParseResult.Failed(memberParse.failure());
            }
            parsed.add(new ScenarioManifestJsonParseResult.ParsedMember(member, memberParse.document()));
        }
        return new ScenarioManifestJsonParseResult.Completed(parsed);
    }

    private MemberParse parseMember(ScenarioManifestCaptureResult.CapturedMember member) {
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(member.bytes()))
                    .toString();
        } catch (CharacterCodingException exception) {
            return failed(member, ScenarioManifestJsonParseResult.Code.INVALID_UTF8,
                    "Scenario manifest member is not valid UTF-8: ");
        }

        boolean completeValueRead = false;
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            JsonToken first = parser.nextToken();
            if (first == null) {
                return failed(member, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON,
                        "Scenario manifest member is not a syntactically valid JSON value: ");
            }
            JsonNode document = mapper.readTree(parser);
            completeValueRead = true;
            if (parser.nextToken() != null) {
                return failed(member, ScenarioManifestJsonParseResult.Code.TRAILING_JSON_CONTENT,
                        "Scenario manifest member contains content after the first JSON value: ");
            }
            return new MemberParse(document, null);
        } catch (JsonParseException exception) {
            if (completeValueRead) {
                return failed(member, ScenarioManifestJsonParseResult.Code.TRAILING_JSON_CONTENT,
                        "Scenario manifest member contains content after the first JSON value: ");
            }
            if (exception.getOriginalMessage() != null
                    && exception.getOriginalMessage().startsWith(STRICT_DUPLICATE_MESSAGE_PREFIX)) {
                return failed(member, ScenarioManifestJsonParseResult.Code.DUPLICATE_JSON_MEMBER,
                        "Scenario manifest member contains a duplicate JSON object member: ");
            }
            return failed(member, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON,
                    "Scenario manifest member is not a syntactically valid JSON value: ");
        } catch (IOException exception) {
            return failed(member, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON,
                    "Scenario manifest member could not be parsed as JSON: ");
        }
    }

    private static MemberParse failed(
            ScenarioManifestCaptureResult.CapturedMember member,
            ScenarioManifestJsonParseResult.Code code,
            String messagePrefix
    ) {
        String path = member.repositoryRelativePath();
        return new MemberParse(null,
                new ScenarioManifestJsonParseResult.Failure(code, path, messagePrefix + path));
    }

    private record MemberParse(JsonNode document, ScenarioManifestJsonParseResult.Failure failure) {
    }
}
