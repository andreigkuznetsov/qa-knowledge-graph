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
    public static final String PARSER_CONTRACT_IDENTIFIER = "scenario-authority-json-parser-v1";

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
        ExactParseResult result = parseExactBytes(member.bytes());
        if (result.failureCode() != null) {
            return failed(member, result.failureCode(), messagePrefix(result.failureCode()));
        }
        return new MemberParse(result.document(), null);
    }

    ExactParseResult parseExactBytes(byte[] exactBytes) {
        Objects.requireNonNull(exactBytes, "exactBytes");
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(exactBytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.INVALID_UTF8);
        }

        boolean completeValueRead = false;
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            JsonToken first = parser.nextToken();
            if (first == null) {
                return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON);
            }
            JsonNode document = mapper.readTree(parser);
            completeValueRead = true;
            if (parser.nextToken() != null) {
                return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.TRAILING_JSON_CONTENT);
            }
            return new ExactParseResult(document, null);
        } catch (JsonParseException exception) {
            if (completeValueRead) {
                return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.TRAILING_JSON_CONTENT);
            }
            if (exception.getOriginalMessage() != null
                    && exception.getOriginalMessage().startsWith(STRICT_DUPLICATE_MESSAGE_PREFIX)) {
                return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.DUPLICATE_JSON_MEMBER);
            }
            return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON);
        } catch (IOException exception) {
            return new ExactParseResult(null, ScenarioManifestJsonParseResult.Code.MALFORMED_JSON);
        }
    }

    private static String messagePrefix(ScenarioManifestJsonParseResult.Code code) {
        return switch (code) {
            case INVALID_UTF8 -> "Scenario manifest member is not valid UTF-8: ";
            case MALFORMED_JSON -> "Scenario manifest member is not a syntactically valid JSON value: ";
            case DUPLICATE_JSON_MEMBER ->
                    "Scenario manifest member contains a duplicate JSON object member: ";
            case TRAILING_JSON_CONTENT ->
                    "Scenario manifest member contains content after the first JSON value: ";
        };
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

    record ExactParseResult(JsonNode document, ScenarioManifestJsonParseResult.Code failureCode) {
        ExactParseResult {
            if ((document == null) == (failureCode == null)) {
                throw new IllegalArgumentException("exact parse result must contain document or failure");
            }
            if (document != null) document = document.deepCopy();
        }

        @Override
        public JsonNode document() {
            return document == null ? null : document.deepCopy();
        }
    }
}
