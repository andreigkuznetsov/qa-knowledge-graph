package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Authoritative proof-bound construction of ADR-015 attributed-member outcomes. */
public final class AttributedMemberOutcomeComposerV1 {
    private AttributedMemberOutcomeComposerV1() {}

    public static Composition composeAdmitted(ManifestSemanticCompositionOutcomeV1 claimedOutcome) {
        ManifestSemanticCompositionOutcomeV1 outcome = revalidate(claimedOutcome);
        VerifiedAdmittedManifestV1 manifest = outcome.manifest();
        var parent = manifest.parentMember();
        var occurrence = manifest.occurrenceIdentity();
        Optional<ManifestSemanticFingerprint> fingerprint = switch (outcome) {
            case ManifestSemanticCompositionOutcomeV1.Composed composed -> Optional.of(composed.fingerprint());
            case ManifestSemanticCompositionOutcomeV1.Unavailable unavailable -> {
                if (unavailable.reason() != ManifestSemanticCompositionOutcomeV1.UnavailableReason
                        .SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE) throw new IllegalArgumentException("unsupported unavailable reason");
                yield Optional.empty();
            }
        };
        var input = new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER, parent,
                manifest.claimedAuthority(), AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(), Optional.of(new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                        occurrence.parentSourceId(), occurrence.parentSnapshotId(), occurrence.parentFingerprint(),
                        occurrence.memberPath(), occurrence.identityVersion())), fingerprint);
        return new Composition(input, AttributedMemberOutcomeFingerprintEncoder.fingerprint(input));
    }

    public static Composition composeRejected(AttributedMemberOutcomeFingerprintInput input) {
        Objects.requireNonNull(input);
        if (input.structuralAdmissionState()
                != AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED)
            throw new IllegalArgumentException("rejected construction requires STRUCTURALLY_REJECTED input");
        return new Composition(input, AttributedMemberOutcomeFingerprintEncoder.fingerprint(input));
    }

    private static ManifestSemanticCompositionOutcomeV1 revalidate(ManifestSemanticCompositionOutcomeV1 claimed) {
        Objects.requireNonNull(claimed);
        ManifestSemanticCompositionOutcomeV1 verified = ManifestSemanticCompositionAttemptV1
                .attemptManifestSemanticCompositionV1(NormalizedManifestSemanticCompositionInputV1.selectedV1(
                        claimed.manifest(), claimed.childOutcomes()));
        if (claimed instanceof ManifestSemanticCompositionOutcomeV1.Composed composed) {
            if (!(verified instanceof ManifestSemanticCompositionOutcomeV1.Composed authoritative)
                    || !composed.fingerprint().equals(authoritative.fingerprint()))
                throw new IllegalArgumentException("Manifest COMPOSED proof or fingerprint was substituted");
        } else if (!(verified instanceof ManifestSemanticCompositionOutcomeV1.Unavailable)) {
            throw new IllegalArgumentException("Manifest UNAVAILABLE proof was substituted");
        }
        return verified;
    }

    public static final class Composition {
        private final AttributedMemberOutcomeFingerprintInput input;
        private final AttributedMemberOutcomeFingerprint fingerprint;
        private Composition(AttributedMemberOutcomeFingerprintInput input,
                            AttributedMemberOutcomeFingerprint fingerprint) {
            this.input=Objects.requireNonNull(input);this.fingerprint=Objects.requireNonNull(fingerprint);
        }
        public AttributedMemberOutcomeFingerprintInput input(){return input;}
        public AttributedMemberOutcomeFingerprint fingerprint(){return fingerprint;}
    }
}
