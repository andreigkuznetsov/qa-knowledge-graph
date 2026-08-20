package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityNormalizationContractsV1;

import java.util.List;
import java.util.Objects;

/** Untrusted candidate material for positive admitted-Manifest verification. */
public final class AdmittedManifestVerificationRequestV1 {
    public static final String REQUEST_VERSION =
            "scenario-authority-admitted-manifest-verification-request-v1";
    public static final String VERIFIER_VERSION = "scenario-authority-admitted-manifest-verifier-v1";
    public static final String PROFILE_VERSION = "scenario-authority-admitted-manifest-verification-profile-v1";
    private final RepositoryCaptureAttestation capture;
    private final AdmittedManifestParentMemberReferenceV1 parentMember;
    private final byte[] exactRawBytes;
    private final List<String> selectedIdentifiers;

    public AdmittedManifestVerificationRequestV1(RepositoryCaptureAttestation capture,
            AdmittedManifestParentMemberReferenceV1 parentMember, byte[] exactRawBytes,
            List<String> selectedIdentifiers) {
        this.capture = Objects.requireNonNull(capture); this.parentMember = Objects.requireNonNull(parentMember);
        this.exactRawBytes = Objects.requireNonNull(exactRawBytes).clone();
        this.selectedIdentifiers = List.copyOf(Objects.requireNonNull(selectedIdentifiers));
    }
    public static List<String> selectedV1Identifiers() {
        ScenarioAuthorityNormalizationContractsV1 c = ScenarioAuthorityNormalizationContractsV1.selectedV1();
        return List.of(REQUEST_VERSION, VERIFIER_VERSION, PROFILE_VERSION,
                c.parserContractIdentifier(), c.attributionContractIdentifier(), c.schemaContractIdentifier(),
                c.schemaContentIdentity(), c.diagnosticMappingContractIdentifier(), c.sourceNormalizationVersion(),
                c.manifestFormat(), c.manifestSchemaVersion(), c.manifestOccurrenceIdentityVersion(),
                c.scenarioDeclarationOccurrenceIdentityVersion(), c.scenarioIdentityScheme(),
                c.scenarioSemanticVersion(), c.stepIdentityVersion(), c.stepSemanticVersion(), c.operationRole(),
                c.operationTargetProfile(), c.operationDatumIdentityVersion(), c.operationSemanticVersion(),
                c.businessRuleDatumIdentityVersion(), c.businessRuleSemanticVersion());
    }
    public RepositoryCaptureAttestation repositoryCaptureAttestation() { return capture; }
    public AdmittedManifestParentMemberReferenceV1 parentMember() { return parentMember; }
    public byte[] exactRawBytes() { return exactRawBytes.clone(); }
    public List<String> selectedIdentifiers() { return selectedIdentifiers; }
}
