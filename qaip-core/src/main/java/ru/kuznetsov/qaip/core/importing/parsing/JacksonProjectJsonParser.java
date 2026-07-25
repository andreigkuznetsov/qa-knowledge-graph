package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe Jackson implementation configured once and used for tree parsing only. */
public final class JacksonProjectJsonParser implements ProjectJsonParser {
    private static final String STRICT_DUPLICATE_MESSAGE_PREFIX = "Duplicate field '";
    private static final String MALFORMED_MESSAGE = "The input is not a syntactically valid JSON value.";
    private static final String DUPLICATE_MESSAGE = "The input contains a duplicate JSON object member.";
    private static final String TRAILING_MESSAGE = "The input contains content after the first JSON value.";

    private final ObjectMapper mapper;

    public JacksonProjectJsonParser() {
        this.mapper = createConfiguredMapper();
    }

    private static ObjectMapper createConfiguredMapper() {
        JsonFactory strictJsonFactory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        return new ObjectMapper(strictJsonFactory)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @Override
    public ProjectParseResult parse(RawProjectJson source) {
        Objects.requireNonNull(source, "source");
        boolean completeValueHasBeenRead = false;
        try (JsonParser parser = mapper.getFactory().createParser(source.value())) {
            JsonToken firstToken = parser.nextToken();
            if (firstToken == null) {
                return rejected(ProjectParseFindingCode.MALFORMED_JSON, parser.currentLocation(), MALFORMED_MESSAGE);
            }
            JsonNode tree = mapper.readTree(parser);
            completeValueHasBeenRead = true;
            if (parser.nextToken() != null) {
                return rejected(ProjectParseFindingCode.TRAILING_JSON_CONTENT, parser.currentTokenLocation(), TRAILING_MESSAGE);
            }
            return new ProjectParseAccepted(new ParsedProjectDocument(tree));
        } catch (JsonParseException exception) {
            if (completeValueHasBeenRead) {
                return rejected(ProjectParseFindingCode.TRAILING_JSON_CONTENT, exception.getLocation(), TRAILING_MESSAGE);
            }
            if (isStrictDuplicateDetectionFailure(exception)) {
                return rejected(ProjectParseFindingCode.DUPLICATE_JSON_MEMBER, exception.getLocation(), DUPLICATE_MESSAGE);
            }
            return rejected(ProjectParseFindingCode.MALFORMED_JSON, exception.getLocation(), MALFORMED_MESSAGE);
        } catch (IOException exception) {
            throw new ProjectJsonParsingContractException("Unexpected failure while reading an in-memory JSON source", exception);
        }
    }

    /**
     * Jackson 2.19.2 reports strict duplicate detection as JsonParseException
     * and exposes no dedicated public subtype or reason code. This compatibility
     * boundary uses the smallest specific prefix emitted by DupDetector; tests
     * pin the adapter to the configured Jackson version.
     */
    static boolean isStrictDuplicateDetectionFailure(JsonParseException exception) {
        return exception.getOriginalMessage() != null
                && exception.getOriginalMessage().startsWith(STRICT_DUPLICATE_MESSAGE_PREFIX);
    }

    private static ProjectParseRejected rejected(
            ProjectParseFindingCode code, JsonLocation sourceLocation, String message) {
        Optional<JsonSourcePosition> position = sourcePosition(sourceLocation);
        return new ProjectParseRejected(List.of(
                new ProjectParseFinding(code, JsonInstanceLocation.ROOT, position, message)));
    }

    private static Optional<JsonSourcePosition> sourcePosition(JsonLocation location) {
        if (location == null || location.getLineNr() < 1 || location.getColumnNr() < 1 || location.getCharOffset() < 0) {
            return Optional.empty();
        }
        return Optional.of(new JsonSourcePosition(
                location.getLineNr(), location.getColumnNr(), location.getCharOffset()));
    }
}
