package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityNormalizationContractsV1;

import java.util.List;
import java.util.Objects;

/** Immutable ADR-014 source-native records. These records make no resolution or canonical claims. */
public final class ScenarioSourceNormalizedRecords {
    public static final String MANIFEST_OCCURRENCE_IDENTITY_VERSION =
            ScenarioAuthorityNormalizationContractsV1.MANIFEST_OCCURRENCE_IDENTITY;
    public static final String DECLARATION_OCCURRENCE_IDENTITY_VERSION =
            ScenarioAuthorityNormalizationContractsV1.SCENARIO_DECLARATION_OCCURRENCE_IDENTITY;
    public static final String CLAIMED_SCENARIO_IDENTITY_VERSION =
            ScenarioAuthorityNormalizationContractsV1.selectedV1().scenarioIdentityScheme();
    public static final String STEP_IDENTITY_VERSION = ScenarioAuthorityNormalizationContractsV1.STEP_IDENTITY;
    public static final String OPERATION_REFERENCE_DATUM_IDENTITY_VERSION =
            ScenarioAuthorityNormalizationContractsV1.selectedV1().operationDatumIdentityVersion();
    public static final String OPERATION_REFERENCE_TARGET_PROFILE =
            ScenarioAuthorityNormalizationContractsV1.selectedV1().operationTargetProfile();
    public static final String BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION =
            ScenarioAuthorityNormalizationContractsV1.selectedV1().businessRuleDatumIdentityVersion();
    public static final String OPERATION_REFERENCE_ROLE = ScenarioAuthorityNormalizationContractsV1.OPERATION_ROLE;

    private ScenarioSourceNormalizedRecords() {
    }

    public record ManifestOccurrenceIdentity(
            String parentSourceId,
            String parentSnapshotId,
            RepositoryCaptureFingerprint parentContentFingerprint,
            String normalizedRepositoryRelativePath,
            String identityVersion
    ) {
        public ManifestOccurrenceIdentity {
            requireText(parentSourceId, "parentSourceId");
            requireText(parentSnapshotId, "parentSnapshotId");
            Objects.requireNonNull(parentContentFingerprint, "parentContentFingerprint");
            requireText(normalizedRepositoryRelativePath, "normalizedRepositoryRelativePath");
            requireExact(identityVersion, MANIFEST_OCCURRENCE_IDENTITY_VERSION, "identityVersion");
        }
    }

    public record ScenarioDeclarationOccurrenceIdentity(
            ManifestOccurrenceIdentity manifestOccurrenceIdentity,
            String structuralPath,
            String identityVersion
    ) {
        public ScenarioDeclarationOccurrenceIdentity {
            Objects.requireNonNull(manifestOccurrenceIdentity, "manifestOccurrenceIdentity");
            if (!Objects.requireNonNull(structuralPath, "structuralPath").matches("/scenarios/(0|[1-9][0-9]*)")) {
                throw new IllegalArgumentException("structuralPath must be /scenarios/<zero-based index>");
            }
            requireExact(identityVersion, DECLARATION_OCCURRENCE_IDENTITY_VERSION, "identityVersion");
        }
    }

    /** Claimed identity intentionally excludes occurrence, presentation, references, and content. */
    public record ClaimedScenarioIdentity(String authority, String scenarioKey, String identityScheme) {
        public ClaimedScenarioIdentity {
            requireText(authority, "authority");
            requireText(scenarioKey, "scenarioKey");
            requireExact(identityScheme, CLAIMED_SCENARIO_IDENTITY_VERSION, "identityScheme");
        }
    }

    public enum StepPhase { GIVEN, WHEN, THEN }

    public record ScenarioStepIdentity(
            ClaimedScenarioIdentity claimedScenarioIdentity,
            StepPhase phase,
            int ordinal,
            String identityVersion
    ) {
        public ScenarioStepIdentity {
            Objects.requireNonNull(claimedScenarioIdentity, "claimedScenarioIdentity");
            Objects.requireNonNull(phase, "phase");
            if (ordinal < 0) throw new IllegalArgumentException("ordinal must be non-negative");
            requireExact(identityVersion, STEP_IDENTITY_VERSION, "identityVersion");
        }
    }

    public record NormalizedScenarioStep(ScenarioStepIdentity identity, String exactAuthoredText) {
        public NormalizedScenarioStep {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(exactAuthoredText, "exactAuthoredText");
        }
    }

    public record OperationReferenceDatumIdentity(
            ClaimedScenarioIdentity claimedScenarioIdentity,
            String role,
            String identityVersion
    ) {
        public OperationReferenceDatumIdentity {
            Objects.requireNonNull(claimedScenarioIdentity, "claimedScenarioIdentity");
            requireExact(role, OPERATION_REFERENCE_ROLE, "role");
            requireExact(identityVersion, OPERATION_REFERENCE_DATUM_IDENTITY_VERSION, "identityVersion");
        }
    }

    public record NormalizedOperationReferenceDatum(
            OperationReferenceDatumIdentity identity,
            String targetProfile,
            String method,
            String path
    ) {
        public NormalizedOperationReferenceDatum {
            Objects.requireNonNull(identity, "identity");
            requireExact(targetProfile, OPERATION_REFERENCE_TARGET_PROFILE, "targetProfile");
            requireText(method, "method");
            requireText(path, "path");
        }
    }

    public record BusinessRuleReferenceDatumIdentity(
            ClaimedScenarioIdentity claimedScenarioIdentity,
            String referencedAuthority,
            String stableRuleKey,
            String identityScheme,
            String identityVersion
    ) {
        public BusinessRuleReferenceDatumIdentity {
            Objects.requireNonNull(claimedScenarioIdentity, "claimedScenarioIdentity");
            requireText(referencedAuthority, "referencedAuthority");
            requireText(stableRuleKey, "stableRuleKey");
            requireText(identityScheme, "identityScheme");
            requireExact(identityVersion, BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION, "identityVersion");
        }
    }

    public record NormalizedBusinessRuleReferenceDatum(
            BusinessRuleReferenceDatumIdentity identity,
            int authoredArrayPosition
    ) {
        public NormalizedBusinessRuleReferenceDatum {
            Objects.requireNonNull(identity, "identity");
            if (authoredArrayPosition < 0) {
                throw new IllegalArgumentException("authoredArrayPosition must be non-negative");
            }
        }
    }

    public record NormalizedScenarioDeclarationOccurrence(
            ScenarioDeclarationOccurrenceIdentity occurrenceIdentity,
            ClaimedScenarioIdentity claimedScenarioIdentity,
            String scenarioKey,
            String title,
            List<String> given,
            List<String> when,
            List<String> then,
            List<NormalizedScenarioStep> steps,
            NormalizedOperationReferenceDatum operationReference,
            List<NormalizedBusinessRuleReferenceDatum> businessRuleReferences
    ) {
        public NormalizedScenarioDeclarationOccurrence {
            Objects.requireNonNull(occurrenceIdentity, "occurrenceIdentity");
            Objects.requireNonNull(claimedScenarioIdentity, "claimedScenarioIdentity");
            requireText(scenarioKey, "scenarioKey");
            requireText(title, "title");
            given = immutableStrings(given, "given");
            when = immutableStrings(when, "when");
            then = immutableStrings(then, "then");
            steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
            Objects.requireNonNull(operationReference, "operationReference");
            businessRuleReferences = List.copyOf(
                    Objects.requireNonNull(businessRuleReferences, "businessRuleReferences"));
            if (!scenarioKey.equals(claimedScenarioIdentity.scenarioKey())) {
                throw new IllegalArgumentException("scenarioKey must equal claimed identity scenarioKey");
            }
        }
    }

    public record NormalizedManifestDatum(
            AttributedMemberSchemaAdmissionOutcome sourceAdmission,
            ManifestOccurrenceIdentity occurrenceIdentity,
            String claimedAuthority,
            String format,
            String schemaVersion,
            String scenarioIdentityScheme,
            List<NormalizedScenarioDeclarationOccurrence> scenarioDeclarations
    ) implements ScenarioSourceNormalizationMemberOutcome {
        public NormalizedManifestDatum {
            Objects.requireNonNull(sourceAdmission, "sourceAdmission");
            if (sourceAdmission.structuralAdmissionState()
                    != AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED) {
                throw new IllegalArgumentException("only structurally admitted members may be normalized");
            }
            Objects.requireNonNull(occurrenceIdentity, "occurrenceIdentity");
            requireText(claimedAuthority, "claimedAuthority");
            requireText(format, "format");
            requireText(schemaVersion, "schemaVersion");
            requireExact(scenarioIdentityScheme, CLAIMED_SCENARIO_IDENTITY_VERSION,
                    "scenarioIdentityScheme");
            scenarioDeclarations = List.copyOf(
                    Objects.requireNonNull(scenarioDeclarations, "scenarioDeclarations"));
            if (!sourceAdmission.claimedAuthority().equals(claimedAuthority)) {
                throw new IllegalArgumentException("claimedAuthority must preserve source admission");
            }
        }

        @Override
        public ParentCapturedMemberRef parentMemberRef() {
            return sourceAdmission.parentMemberRef();
        }
    }

    private static List<String> immutableStrings(List<String> values, String field) {
        List<String> copy = List.copyOf(Objects.requireNonNull(values, field));
        copy.forEach(value -> Objects.requireNonNull(value, field + " value"));
        return copy;
    }

    private static void requireExact(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
    }
}
