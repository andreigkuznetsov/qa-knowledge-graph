package ru.kuznetsov.qaip.evidencegovernance.source;

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
import java.util.Objects;

import static ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityJsonParseRejectionV1.Code;

/** Sole authoritative Scenario Authority V1 exact-byte JSON parser. */
public final class ScenarioAuthorityExactJsonParserV1 {
    public static final String CONTRACT_IDENTIFIER = ScenarioAuthorityParsedJsonV1.PARSER_CONTRACT_IDENTIFIER;
    private static final String STRICT_DUPLICATE_MESSAGE_PREFIX = "Duplicate field '";
    private final ObjectMapper mapper;

    public ScenarioAuthorityExactJsonParserV1() {
        JsonFactory factory = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        mapper = new ObjectMapper(factory)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    public ScenarioAuthorityParsedJsonV1 parseExactBytes(byte[] exactRawBytes) {
        byte[] bytes = Objects.requireNonNull(exactRawBytes, "exactRawBytes").clone();
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException rejection) {
            throw reject(Code.INVALID_UTF8);
        }

        boolean completeValueRead = false;
        try (JsonParser parser = mapper.getFactory().createParser(json)) {
            JsonToken first = parser.nextToken();
            if (first == null) throw reject(Code.MALFORMED_JSON);
            JsonNode document = mapper.readTree(parser);
            completeValueRead = true;
            if (parser.nextToken() != null) throw reject(Code.TRAILING_JSON_CONTENT);
            return new ScenarioAuthorityParsedJsonV1(bytes, document);
        } catch (ScenarioAuthorityJsonParseRejectionV1 rejection) {
            throw rejection;
        } catch (JsonParseException rejection) {
            if (completeValueRead) throw reject(Code.TRAILING_JSON_CONTENT);
            if (rejection.getOriginalMessage() != null
                    && rejection.getOriginalMessage().startsWith(STRICT_DUPLICATE_MESSAGE_PREFIX))
                throw reject(Code.DUPLICATE_JSON_MEMBER);
            throw reject(Code.MALFORMED_JSON);
        } catch (IOException rejection) {
            throw reject(Code.MALFORMED_JSON);
        }
    }

    private static ScenarioAuthorityJsonParseRejectionV1 reject(Code code) {
        return new ScenarioAuthorityJsonParseRejectionV1(code);
    }
}
