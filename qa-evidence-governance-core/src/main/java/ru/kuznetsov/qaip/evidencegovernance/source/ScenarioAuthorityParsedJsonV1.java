package ru.kuznetsov.qaip.evidencegovernance.source;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** Proof-bearing successful parse of one exact raw-byte sequence under the V1 parser. */
public final class ScenarioAuthorityParsedJsonV1 {
    public static final String PARSER_CONTRACT_IDENTIFIER = "scenario-authority-json-parser-v1";

    private final byte[] exactRawBytes;
    private final JsonNode document;

    ScenarioAuthorityParsedJsonV1(byte[] exactRawBytes, JsonNode document) {
        this.exactRawBytes = Objects.requireNonNull(exactRawBytes).clone();
        this.document = Objects.requireNonNull(document).deepCopy();
    }

    public String parserContractIdentifier() { return PARSER_CONTRACT_IDENTIFIER; }
    public byte[] exactRawBytes() { return exactRawBytes.clone(); }
    public JsonNode document() { return document.deepCopy(); }
    JsonNode authoritativeDocument() { return document; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof ScenarioAuthorityParsedJsonV1 that
                && java.util.Arrays.equals(exactRawBytes, that.exactRawBytes)
                && document.equals(that.document);
    }
    @Override public int hashCode() { return 31 * java.util.Arrays.hashCode(exactRawBytes) + document.hashCode(); }
}
