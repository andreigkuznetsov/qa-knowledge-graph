package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.Objects;
/** Closed finite Manifest V1 processing/compatibility/integrity rejection taxonomy. */
public final class ManifestCompositionRejectionV1 extends IllegalArgumentException {
    private final Code code;
    public ManifestCompositionRejectionV1(Code code){super(Objects.requireNonNull(code).name());this.code=code;}
    public Code code(){return code;}
    public enum Code { STRUCTURALLY_UNADMITTED_MANIFEST, SCENARIO_SEMANTIC_UNAVAILABLE,
        SCENARIO_ATTESTATION_MISMATCH, SCENARIO_MANIFEST_OCCURRENCE_MISMATCH,
        SCENARIO_AUTHORITY_MISMATCH, SCENARIO_COUNT_MISMATCH, SCENARIO_POSITION_MISMATCH,
        SCENARIO_DECLARATION_SUBSTITUTION, UNSUPPORTED_MANIFEST_CONTRACT }
}
