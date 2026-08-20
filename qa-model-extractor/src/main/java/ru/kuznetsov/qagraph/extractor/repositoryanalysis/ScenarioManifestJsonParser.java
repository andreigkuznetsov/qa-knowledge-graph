package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityExactJsonParserV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityJsonParseRejectionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityParsedJsonV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Extractor compatibility projection over the authoritative Evidence Governance parser. */
public final class ScenarioManifestJsonParser {
    public static final String PARSER_CONTRACT_IDENTIFIER = ScenarioAuthorityExactJsonParserV1.CONTRACT_IDENTIFIER;
    private final ScenarioAuthorityExactJsonParserV1 delegate = new ScenarioAuthorityExactJsonParserV1();

    public ScenarioManifestJsonParseResult parse(ScenarioManifestCaptureResult.Completed capture) {
        Objects.requireNonNull(capture, "capture");
        List<ScenarioManifestJsonParseResult.ParsedMember> parsed = new ArrayList<>();
        for (ScenarioManifestCaptureResult.CapturedMember member : capture.members()) {
            try {
                parsed.add(new ScenarioManifestJsonParseResult.ParsedMember(
                        member, parseAuthoritatively(member.bytes()).document()));
            } catch (ScenarioAuthorityJsonParseRejectionV1 rejection) {
                var code = compatibilityCode(rejection.code());
                return new ScenarioManifestJsonParseResult.Failed(new ScenarioManifestJsonParseResult.Failure(
                        code, member.repositoryRelativePath(), messagePrefix(code) + member.repositoryRelativePath()));
            }
        }
        return new ScenarioManifestJsonParseResult.Completed(parsed);
    }

    ScenarioAuthorityParsedJsonV1 parseAuthoritatively(byte[] exactBytes) {
        return delegate.parseExactBytes(exactBytes);
    }

    ExactParseResult parseExactBytes(byte[] exactBytes) {
        try {
            return new ExactParseResult(parseAuthoritatively(exactBytes).document(), null);
        } catch (ScenarioAuthorityJsonParseRejectionV1 rejection) {
            return new ExactParseResult(null, compatibilityCode(rejection.code()));
        }
    }

    private static ScenarioManifestJsonParseResult.Code compatibilityCode(
            ScenarioAuthorityJsonParseRejectionV1.Code code) {
        return switch (code) {
            case INVALID_UTF8 -> ScenarioManifestJsonParseResult.Code.INVALID_UTF8;
            case MALFORMED_JSON -> ScenarioManifestJsonParseResult.Code.MALFORMED_JSON;
            case DUPLICATE_JSON_MEMBER -> ScenarioManifestJsonParseResult.Code.DUPLICATE_JSON_MEMBER;
            case TRAILING_JSON_CONTENT -> ScenarioManifestJsonParseResult.Code.TRAILING_JSON_CONTENT;
        };
    }

    private static String messagePrefix(ScenarioManifestJsonParseResult.Code code) {
        return switch (code) {
            case INVALID_UTF8 -> "Scenario manifest member is not valid UTF-8: ";
            case MALFORMED_JSON -> "Scenario manifest member is not a syntactically valid JSON value: ";
            case DUPLICATE_JSON_MEMBER -> "Scenario manifest member contains a duplicate JSON object member: ";
            case TRAILING_JSON_CONTENT -> "Scenario manifest member contains content after the first JSON value: ";
        };
    }

    record ExactParseResult(com.fasterxml.jackson.databind.JsonNode document,
                            ScenarioManifestJsonParseResult.Code failureCode) {
        ExactParseResult {
            if ((document == null) == (failureCode == null))
                throw new IllegalArgumentException("exact parse result must contain document or failure");
            if (document != null) document = document.deepCopy();
        }
        @Override public com.fasterxml.jackson.databind.JsonNode document() {
            return document == null ? null : document.deepCopy();
        }
    }
}
