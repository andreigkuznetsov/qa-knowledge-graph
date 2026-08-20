package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintComposerV1;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintInput;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ScenarioSemanticFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.kuznetsov.qagraph.extractor.repositoryanalysis.ManifestSemanticCompositionException.Code;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Authoritative production boundary for composing one admitted Manifest semantic fingerprint. */
public final class ManifestSemanticFingerprintComposer {
    private ManifestSemanticFingerprintComposer() {
    }

    public static ManifestSemanticCompositionResult compose(
            NormalizedManifestDatum manifest,
            List<NormalizedScenarioSemanticAttestation> attestations
    ) {
        Objects.requireNonNull(manifest, "manifest");
        List<NormalizedScenarioSemanticAttestation> ordered = List.copyOf(
                Objects.requireNonNull(attestations, "attestations"));
        requireStructurallyAdmitted(manifest);
        requireSupportedContracts(manifest);
        if (ordered.size() != manifest.scenarioDeclarations().size()) fail(Code.SCENARIO_COUNT_MISMATCH);

        List<ScenarioSemanticFingerprint> fingerprints = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            NormalizedScenarioSemanticAttestation attestation = ordered.get(index);
            if (!attestation.isAuthoritativelyBound()) fail(Code.SCENARIO_ATTESTATION_MISMATCH);
            NormalizedScenarioDeclarationOccurrence declaration = attestation.declaration();
            if (!manifest.occurrenceIdentity().equals(
                    declaration.occurrenceIdentity().manifestOccurrenceIdentity())) {
                fail(Code.SCENARIO_MANIFEST_OCCURRENCE_MISMATCH);
            }
            if (!manifest.claimedAuthority().equals(declaration.claimedScenarioIdentity().authority())) {
                fail(Code.SCENARIO_AUTHORITY_MISMATCH);
            }
            if (!declaration.occurrenceIdentity().structuralPath().equals("/scenarios/" + index)) {
                fail(Code.SCENARIO_POSITION_MISMATCH);
            }
            if (!manifest.scenarioDeclarations().get(index).equals(declaration)) {
                fail(Code.SCENARIO_DECLARATION_SUBSTITUTION);
            }
            fingerprints.add(attestation.fingerprint());
        }

        ManifestSemanticFingerprintInput accepted = new ManifestSemanticFingerprintInput(
                ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                manifest.claimedAuthority(), manifest.format(), manifest.schemaVersion(),
                manifest.scenarioIdentityScheme(),
                ManifestSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                fingerprints);
        return new ManifestSemanticCompositionResult(
                accepted, ManifestSemanticFingerprintComposerV1.fingerprintAcceptedInput(accepted));
    }

    private static void requireStructurallyAdmitted(NormalizedManifestDatum manifest) {
        if (manifest.sourceAdmission().structuralAdmissionState()
                != AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED) {
            fail(Code.STRUCTURALLY_UNADMITTED_MANIFEST);
        }
    }

    private static void requireSupportedContracts(NormalizedManifestDatum manifest) {
        if (!ManifestSemanticFingerprintEncoder.MANIFEST_FORMAT_IDENTIFIER.equals(manifest.format())
                || !ManifestSemanticFingerprintEncoder.SCHEMA_VERSION.equals(manifest.schemaVersion())
                || !ManifestSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION.equals(
                manifest.scenarioIdentityScheme())) {
            fail(Code.UNSUPPORTED_MANIFEST_CONTRACT);
        }
    }

    private static void fail(Code code) {
        throw new ManifestSemanticCompositionException(code);
    }
}
