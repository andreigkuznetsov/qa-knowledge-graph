package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** ADR-014 per-member parser and safe claimed-authority attribution boundary. */
public final class ScenarioLogicalSourceMemberProcessor {
    public static final String PARSER_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER;
    public static final String ATTRIBUTION_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER;
    public static final String STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER;

    private static final String ROOT_POINTER = "";
    private static final String AUTHORITY_POINTER = "/authority";
    private static final Pattern AUTHORITY = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:/-]*$");
    private static final int MAX_AUTHORITY_LENGTH = 200;

    private final ScenarioManifestJsonParser parser;

    public ScenarioLogicalSourceMemberProcessor() {
        this(new ScenarioManifestJsonParser());
    }

    ScenarioLogicalSourceMemberProcessor(ScenarioManifestJsonParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public ScenarioLogicalSourceProcessingResult process(
            ScenarioRepositoryCaptureSnapshotCandidate parent
    ) {
        Objects.requireNonNull(parent, "parent");
        List<ScenarioMemberProcessingOutcome> outcomes = new ArrayList<>();
        for (ScenarioManifestStableCaptureResult.CapturedMember member : parent.members()) {
            ParentCapturedMemberRef parentRef = ParentCapturedMemberRef.from(parent, member);
            outcomes.add(processMember(parentRef, member.bytes()));
        }
        return new ScenarioLogicalSourceProcessingResult(parent, outcomes);
    }

    private ScenarioMemberProcessingOutcome processMember(
            ParentCapturedMemberRef parentRef,
            byte[] exactParentBytes
    ) {
        ScenarioManifestJsonParser.ExactParseResult parsed = parser.parseExactBytes(exactParentBytes);
        if (parsed.failureCode() != null) {
            return new UnattributableMemberProcessingOutcome(
                    parentRef,
                    PARSER_CONTRACT_IDENTIFIER,
                    ATTRIBUTION_CONTRACT_IDENTIFIER,
                    parseOutcome(parsed.failureCode()),
                    ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                    Optional.empty());
        }

        JsonNode document = parsed.document();
        if (!document.isObject()) {
            return unattributable(parentRef,
                    ScenarioMemberProcessingOutcome.AttributionOutcome.NON_OBJECT_ROOT,
                    ROOT_POINTER);
        }
        if (!document.has("authority")) {
            return unattributable(parentRef,
                    ScenarioMemberProcessingOutcome.AttributionOutcome.MISSING_AUTHORITY,
                    AUTHORITY_POINTER);
        }
        JsonNode authorityNode = document.get("authority");
        if (!authorityNode.isTextual()) {
            return unattributable(parentRef,
                    ScenarioMemberProcessingOutcome.AttributionOutcome.AUTHORITY_NOT_STRING,
                    AUTHORITY_POINTER);
        }
        String authority = authorityNode.textValue();
        if (!validAuthority(authority)) {
            return unattributable(parentRef,
                    ScenarioMemberProcessingOutcome.AttributionOutcome.INVALID_AUTHORITY,
                    AUTHORITY_POINTER);
        }

        return new AttributedMemberProcessingOutcome(
                parentRef,
                authority,
                PARSER_CONTRACT_IDENTIFIER,
                ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED,
                Optional.empty(),
                document);
    }

    private static UnattributableMemberProcessingOutcome unattributable(
            ParentCapturedMemberRef parentRef,
            ScenarioMemberProcessingOutcome.AttributionOutcome outcome,
            String structuralLocation
    ) {
        return new UnattributableMemberProcessingOutcome(
                parentRef,
                PARSER_CONTRACT_IDENTIFIER,
                ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                outcome,
                Optional.of(structuralLocation));
    }

    private static ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome(
            ScenarioManifestJsonParseResult.Code code
    ) {
        return switch (code) {
            case INVALID_UTF8 -> ScenarioMemberProcessingOutcome.ParseOutcome.INVALID_UTF8;
            case MALFORMED_JSON -> ScenarioMemberProcessingOutcome.ParseOutcome.MALFORMED_JSON;
            case DUPLICATE_JSON_MEMBER -> ScenarioMemberProcessingOutcome.ParseOutcome.DUPLICATE_JSON_MEMBER;
            case TRAILING_JSON_CONTENT -> ScenarioMemberProcessingOutcome.ParseOutcome.TRAILING_JSON_CONTENT;
        };
    }

    private static boolean validAuthority(String authority) {
        return authority.length() <= MAX_AUTHORITY_LENGTH && AUTHORITY.matcher(authority).matches();
    }
}
