package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityJsonParseRejectionV1;

import java.util.List;

/** Exhaustive finite rejection taxonomy for positive admitted-Manifest verification. */
public final class AdmittedManifestVerificationRejectionV1 extends IllegalArgumentException {
    public enum Code { UNSUPPORTED_VERIFICATION_CONTRACT, CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
        PARSE_REJECTED, AUTHORITY_ATTRIBUTION_UNAVAILABLE, STRUCTURAL_SCHEMA_REJECTED }
    private final Code code; private final ScenarioAuthorityJsonParseRejectionV1.Code parserCause;
    private final ScenarioAuthorityAttributionRejectionV1.Code attributionCause;
    private final String attributionLocation; private final List<ScenarioSchemaDiagnostic> diagnostics;
    AdmittedManifestVerificationRejectionV1(Code code, ScenarioAuthorityJsonParseRejectionV1.Code parserCause,
            ScenarioAuthorityAttributionRejectionV1.Code attributionCause, String attributionLocation,
            List<ScenarioSchemaDiagnostic> diagnostics) {
        super(code.name()); this.code=code; this.parserCause=parserCause; this.attributionCause=attributionCause;
        this.attributionLocation=attributionLocation; this.diagnostics=List.copyOf(diagnostics);
    }
    public Code code(){return code;} public ScenarioAuthorityJsonParseRejectionV1.Code parserCause(){return parserCause;}
    public ScenarioAuthorityAttributionRejectionV1.Code attributionCause(){return attributionCause;}
    public String attributionLocation(){return attributionLocation;}
    public List<ScenarioSchemaDiagnostic> canonicalDiagnostics(){return diagnostics;}
}
