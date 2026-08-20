package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributorV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityJsonParseRejectionV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** ADR-014 per-member parser and safe claimed-authority attribution boundary. */
public final class ScenarioLogicalSourceMemberProcessor {
    public static final String PARSER_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER;
    public static final String ATTRIBUTION_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER;
    public static final String STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER =
            ScenarioMemberProcessingOutcome.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER;

    private final ScenarioManifestJsonParser parser;
    private final ScenarioAuthorityAttributorV1 attributor = new ScenarioAuthorityAttributorV1();

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
        ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityParsedJsonV1 parsed;
        try {
            parsed = parser.parseAuthoritatively(exactParentBytes);
        } catch (ScenarioAuthorityJsonParseRejectionV1 rejection) {
            return new UnattributableMemberProcessingOutcome(
                    parentRef,
                    PARSER_CONTRACT_IDENTIFIER,
                    ATTRIBUTION_CONTRACT_IDENTIFIER,
                    parseOutcome(rejection.code()),
                    ScenarioMemberProcessingOutcome.AttributionOutcome.PARSE_UNATTRIBUTABLE,
                    Optional.empty());
        }
        ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionV1 attribution;
        try {
            attribution = attributor.attribute(parsed);
        } catch (ScenarioAuthorityAttributionRejectionV1 rejection) {
            return unattributable(parentRef, attributionOutcome(rejection.code()), rejection.structuralLocation());
        }

        return new AttributedMemberProcessingOutcome(
                parentRef,
                attribution.authority(),
                PARSER_CONTRACT_IDENTIFIER,
                ATTRIBUTION_CONTRACT_IDENTIFIER,
                ScenarioMemberProcessingOutcome.ParseOutcome.PARSED,
                ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED,
                Optional.empty(),
                parsed,
                attribution,
                parsed.document());
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
            ScenarioAuthorityJsonParseRejectionV1.Code code
    ) {
        return switch (code) {
            case INVALID_UTF8 -> ScenarioMemberProcessingOutcome.ParseOutcome.INVALID_UTF8;
            case MALFORMED_JSON -> ScenarioMemberProcessingOutcome.ParseOutcome.MALFORMED_JSON;
            case DUPLICATE_JSON_MEMBER -> ScenarioMemberProcessingOutcome.ParseOutcome.DUPLICATE_JSON_MEMBER;
            case TRAILING_JSON_CONTENT -> ScenarioMemberProcessingOutcome.ParseOutcome.TRAILING_JSON_CONTENT;
        };
    }

    private static ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome(
            ScenarioAuthorityAttributionRejectionV1.Code code) {
        return switch (code) {
            case NON_OBJECT_ROOT -> ScenarioMemberProcessingOutcome.AttributionOutcome.NON_OBJECT_ROOT;
            case MISSING_AUTHORITY -> ScenarioMemberProcessingOutcome.AttributionOutcome.MISSING_AUTHORITY;
            case AUTHORITY_NOT_STRING -> ScenarioMemberProcessingOutcome.AttributionOutcome.AUTHORITY_NOT_STRING;
            case INVALID_AUTHORITY -> ScenarioMemberProcessingOutcome.AttributionOutcome.INVALID_AUTHORITY;
        };
    }
}
