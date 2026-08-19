package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Complete immutable ADR-015 Repository Derivation Report input with total parent accounting. */
public record RepositoryDerivationReportFingerprintInput(
        String reportContractVersion,
        ParentRepositoryCapture parentCapture,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        String structuralLocationContractIdentifier,
        List<MemberOutcome> memberOutcomes,
        List<UnsupportedMatchingEntry> unsupportedMatchingEntries,
        List<SemanticProvenanceAttestation> provenance
) {
    public static final String REPORT_CONTRACT_VERSION =
            "scenario-authority-repository-derivation-report-v1";
    public static final String PARSER_CONTRACT_IDENTIFIER = "scenario-authority-json-parser-v1";
    public static final String ATTRIBUTION_CONTRACT_IDENTIFIER = "scenario-authority-attribution-v1";
    public static final String STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER = "rfc-6901-json-pointer-v1";

    public RepositoryDerivationReportFingerprintInput {
        requireExact(reportContractVersion, REPORT_CONTRACT_VERSION, "reportContractVersion");
        Objects.requireNonNull(parentCapture, "parentCapture");
        requireExact(parserContractIdentifier, PARSER_CONTRACT_IDENTIFIER, "parserContractIdentifier");
        requireExact(attributionContractIdentifier, ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        requireExact(structuralLocationContractIdentifier, STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                "structuralLocationContractIdentifier");
        memberOutcomes = List.copyOf(Objects.requireNonNull(memberOutcomes, "memberOutcomes"));
        unsupportedMatchingEntries = List.copyOf(
                Objects.requireNonNull(unsupportedMatchingEntries, "unsupportedMatchingEntries"));
        provenance = List.copyOf(Objects.requireNonNull(provenance, "provenance"));

        requireTotalMemberAccounting(parentCapture, memberOutcomes);
        if (!parentCapture.unsupportedMatchingEntries.equals(unsupportedMatchingEntries)) {
            throw new IllegalArgumentException(
                    "unsupported matching entries must exactly preserve parent membership and order");
        }
        requireProvenance(memberOutcomes, provenance);
    }

    public sealed interface MemberOutcome permits AttributedMemberRetained, UnattributableMemberRetained {
        AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember();
    }

    public record AttributedMemberRetained(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            String claimedAuthority,
            SemanticProvenanceOutputReference attributedMemberOutput
    ) implements MemberOutcome {
        public static final String TAG = "ATTRIBUTED_MEMBER_RETAINED";

        public AttributedMemberRetained {
            Objects.requireNonNull(parentMember, "parentMember");
            claimedAuthority = requireNonBlank(claimedAuthority, "claimedAuthority");
            Objects.requireNonNull(attributedMemberOutput, "attributedMemberOutput");
            if (!parentMember.equals(attributedMemberOutput.outputIdentity())) {
                throw new IllegalArgumentException("attributed output identity must equal parent member");
            }
            if (!claimedAuthority.equals(attributedMemberOutput.claimedAuthority())) {
                throw new IllegalArgumentException("claimed authority must equal attributed output authority");
            }
        }
    }

    public record UnattributableMemberRetained(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember,
            String parseOutcome,
            String attributionOutcome,
            Optional<String> structuralLocation
    ) implements MemberOutcome {
        public static final String TAG = "UNATTRIBUTABLE_MEMBER_RETAINED";
        private static final List<String> PARSE_FAILURES = List.of(
                "INVALID_UTF8", "MALFORMED_JSON", "DUPLICATE_JSON_MEMBER", "TRAILING_JSON_CONTENT");
        private static final List<String> ATTRIBUTION_FAILURES = List.of(
                "NON_OBJECT_ROOT", "MISSING_AUTHORITY", "AUTHORITY_NOT_STRING", "INVALID_AUTHORITY");

        public UnattributableMemberRetained {
            Objects.requireNonNull(parentMember, "parentMember");
            Objects.requireNonNull(parseOutcome, "parseOutcome");
            Objects.requireNonNull(attributionOutcome, "attributionOutcome");
            structuralLocation = Objects.requireNonNull(structuralLocation, "structuralLocation");
            structuralLocation.ifPresent(RepositoryDerivationReportFingerprintInput::requireJsonPointer);
            boolean parseFailure = PARSE_FAILURES.contains(parseOutcome);
            if (parseFailure != "PARSE_UNATTRIBUTABLE".equals(attributionOutcome)) {
                throw new IllegalArgumentException(
                        "parse failures require PARSE_UNATTRIBUTABLE and parsed inputs require attribution failure");
            }
            if (!parseFailure && (!"PARSED".equals(parseOutcome)
                    || !ATTRIBUTION_FAILURES.contains(attributionOutcome))) {
                throw new IllegalArgumentException("unsupported unattributable outcome combination");
            }
        }
    }

    public record ParentRepositoryCapture(
            String sourceId,
            String snapshotId,
            RepositoryCaptureFingerprint contentFingerprint,
            List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> regularMembers,
            List<UnsupportedMatchingEntry> unsupportedMatchingEntries
    ) {
        public ParentRepositoryCapture {
            sourceId = requireNonBlank(sourceId, "sourceId");
            snapshotId = requireNonBlank(snapshotId, "snapshotId");
            Objects.requireNonNull(contentFingerprint, "contentFingerprint");
            regularMembers = List.copyOf(Objects.requireNonNull(regularMembers, "regularMembers"));
            unsupportedMatchingEntries = List.copyOf(
                    Objects.requireNonNull(unsupportedMatchingEntries, "unsupportedMatchingEntries"));
            if (new HashSet<>(regularMembers).size() != regularMembers.size()) {
                throw new IllegalArgumentException("parent regular members must be unique");
            }
            for (var member : regularMembers) {
                if (!sourceId.equals(member.parentSourceId())
                        || !snapshotId.equals(member.parentSnapshotId())
                        || !contentFingerprint.equals(member.parentContentFingerprint())) {
                    throw new IllegalArgumentException("regular member belongs to another parent capture");
                }
            }
            requireCanonicalUnsupportedOrder(unsupportedMatchingEntries);
            var regularPaths = regularMembers.stream()
                    .map(AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference::normalizedRepositoryRelativePath)
                    .collect(java.util.stream.Collectors.toSet());
            if (unsupportedMatchingEntries.stream()
                    .map(UnsupportedMatchingEntry::normalizedRepositoryRelativePath)
                    .anyMatch(regularPaths::contains)) {
                throw new IllegalArgumentException("a path cannot be both regular and unsupported");
            }
        }
    }

    public record UnsupportedMatchingEntry(
            String normalizedRepositoryRelativePath,
            String entryKind,
            String stableDiagnosticCode
    ) {
        public UnsupportedMatchingEntry {
            normalizedRepositoryRelativePath = requireNormalizedPath(
                    normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
            entryKind = requireNonBlank(entryKind, "entryKind");
            stableDiagnosticCode = requireNonBlank(stableDiagnosticCode, "stableDiagnosticCode");
            boolean supported = (entryKind.equals("SYMBOLIC_LINK")
                    && stableDiagnosticCode.equals("UNSUPPORTED_SYMBOLIC_LINK"))
                    || (entryKind.equals("DIRECTORY")
                    && stableDiagnosticCode.equals("UNSUPPORTED_DIRECTORY"))
                    || (entryKind.equals("OTHER_NON_REGULAR")
                    && stableDiagnosticCode.equals("UNSUPPORTED_OTHER_NON_REGULAR_ENTRY"));
            if (!supported) throw new IllegalArgumentException("unsupported matching-entry kind/code pair");
        }
    }

    private static void requireTotalMemberAccounting(
            ParentRepositoryCapture parent,
            List<MemberOutcome> outcomes
    ) {
        if (outcomes.size() != parent.regularMembers.size()) {
            throw new IllegalArgumentException("exactly one outcome is required for every regular parent member");
        }
        for (int index = 0; index < outcomes.size(); index++) {
            if (!parent.regularMembers.get(index).equals(outcomes.get(index).parentMember())) {
                throw new IllegalArgumentException("member outcomes must preserve exact parent-member order");
            }
        }
        if (new HashSet<>(outcomes.stream().map(MemberOutcome::parentMember).toList()).size()
                != outcomes.size()) {
            throw new IllegalArgumentException("duplicate member outcome");
        }
    }

    private static void requireProvenance(
            List<MemberOutcome> outcomes,
            List<SemanticProvenanceAttestation> provenance
    ) {
        List<AttributedMemberRetained> attributed = outcomes.stream()
                .filter(AttributedMemberRetained.class::isInstance)
                .map(AttributedMemberRetained.class::cast)
                .toList();
        if (provenance.size() != attributed.size()) {
            throw new IllegalArgumentException("exactly one provenance reference is required per attributed member");
        }
        for (int index = 1; index < provenance.size(); index++) {
            byte[] previous = SemanticProvenanceFingerprintEncoder.identityBytes(
                    provenance.get(index - 1).input().provenanceIdentity());
            byte[] current = SemanticProvenanceFingerprintEncoder.identityBytes(
                    provenance.get(index).input().provenanceIdentity());
            if (Arrays.compareUnsigned(previous, current) >= 0) {
                throw new IllegalArgumentException("provenance must use unique stable identity order");
            }
        }
        for (AttributedMemberRetained outcome : attributed) {
            List<SemanticProvenanceAttestation> matches = provenance.stream()
                    .filter(item -> item.input().provenanceIdentity().outputDatumIdentity()
                            .equals(outcome.parentMember))
                    .toList();
            if (matches.size() != 1) {
                throw new IllegalArgumentException("attributed member must have exactly one matching provenance");
            }
            SemanticProvenanceOutputReference provenanceOutput = matches.getFirst().input().outputReference();
            if (!provenanceOutput.outputIdentity().equals(outcome.parentMember)
                    || !provenanceOutput.fingerprint().equals(outcome.attributedMemberOutput.fingerprint())) {
                throw new IllegalArgumentException("provenance output does not match attributed member outcome");
            }
        }
    }

    private static void requireCanonicalUnsupportedOrder(List<UnsupportedMatchingEntry> entries) {
        for (int index = 1; index < entries.size(); index++) {
            if (compareCodePoints(entries.get(index - 1).normalizedRepositoryRelativePath,
                    entries.get(index).normalizedRepositoryRelativePath) >= 0) {
                throw new IllegalArgumentException("unsupported entries must use unique canonical parent order");
            }
        }
    }

    private static int compareCodePoints(String left, String right) {
        int li = 0;
        int ri = 0;
        while (li < left.length() && ri < right.length()) {
            int lcp = left.codePointAt(li);
            int rcp = right.codePointAt(ri);
            if (lcp != rcp) return Integer.compare(lcp, rcp);
            li += Character.charCount(lcp);
            ri += Character.charCount(rcp);
        }
        return Integer.compare(left.codePointCount(0, left.length()), right.codePointCount(0, right.length()));
    }

    private static String requireJsonPointer(String value) {
        Objects.requireNonNull(value, "structuralLocation");
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException("structuralLocation must be an RFC 6901 JSON Pointer");
        }
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '~'
                    && (++index >= value.length()
                    || (value.charAt(index) != '0' && value.charAt(index) != '1'))) {
                throw new IllegalArgumentException("structuralLocation has invalid RFC 6901 escape");
            }
        }
        return value;
    }

    private static String requireNormalizedPath(String value, String field) {
        requireNonBlank(value, field);
        if (value.startsWith("/") || value.endsWith("/") || value.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(field + " must be a normalized repository-relative path");
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(field + " contains a noncanonical segment");
            }
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
