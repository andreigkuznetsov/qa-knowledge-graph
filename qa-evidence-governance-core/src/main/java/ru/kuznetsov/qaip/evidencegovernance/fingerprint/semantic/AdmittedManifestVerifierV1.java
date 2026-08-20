package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioAuthoritySchemaDiagnosticMapperV1;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.source.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AdmittedManifestVerificationRejectionV1.Code.*;

/** Sole positive verifier for the Scenario Authority admitted-Manifest proof V1. */
public final class AdmittedManifestVerifierV1 {
    public static final String VERIFIER_IDENTIFIER = AdmittedManifestVerificationRequestV1.VERIFIER_VERSION;

    public VerifiedAdmittedManifestV1 verifyAdmittedManifestV1(AdmittedManifestVerificationRequestV1 request) {
        if (request == null) throw new NullPointerException("request");
        if (!request.selectedIdentifiers().equals(AdmittedManifestVerificationRequestV1.selectedV1Identifiers()))
            throw reject(UNSUPPORTED_VERIFICATION_CONTRACT);

        RepositoryCaptureAttestation capture=request.repositoryCaptureAttestation();
        AdmittedManifestParentMemberReferenceV1 selected=request.parentMember(); byte[] bytes=request.exactRawBytes();
        if (!matches(capture,selected,bytes)) throw reject(CAPTURE_MEMBER_RAW_BYTES_MISMATCH);

        ScenarioAuthorityParsedJsonV1 parsed;
        try { parsed=new ScenarioAuthorityExactJsonParserV1().parseExactBytes(bytes); }
        catch (ScenarioAuthorityJsonParseRejectionV1 x) {
            throw new AdmittedManifestVerificationRejectionV1(PARSE_REJECTED,x.code(),null,null,List.of());
        }
        ScenarioAuthorityAttributionV1 attribution;
        try { attribution=new ScenarioAuthorityAttributorV1().attribute(parsed); }
        catch (ScenarioAuthorityAttributionRejectionV1 x) {
            throw new AdmittedManifestVerificationRejectionV1(AUTHORITY_ATTRIBUTION_UNAVAILABLE,null,x.code(),
                    x.structuralLocation(),List.of());
        }
        var diagnostics=new ScenarioAuthoritySchemaDiagnosticMapperV1().validate(parsed.document());
        if(!diagnostics.isEmpty()) throw new AdmittedManifestVerificationRejectionV1(
                STRUCTURAL_SCHEMA_REJECTED,null,null,null,diagnostics);

        ScenarioAuthorityNormalizationContractsV1 contracts=ScenarioAuthorityNormalizationContractsV1.selectedV1();
        EvidenceGovernanceNormalizedManifestV1 normalized=
                new ScenarioAuthorityNormalizerV1().normalizeStructurallyAdmitted(parsed,attribution,contracts);
        var oldParent=new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                selected.sourceId(),selected.snapshotId(),selected.repositoryCaptureFingerprint(),
                selected.normalizedRepositoryRelativePath(),selected.rawByteLength(),selected.rawSourceMemberFingerprint());
        var manifest=new NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity(selected.sourceId(),
                selected.snapshotId(),selected.repositoryCaptureFingerprint(),selected.normalizedRepositoryRelativePath(),
                contracts.manifestOccurrenceIdentityVersion());
        List<NormalizedScenarioOccurrenceInputV1> children=new ArrayList<>();
        for(var scenario:normalized.authoredScenarios()) children.add(child(capture,oldParent,manifest,scenario,contracts));
        return VerifiedAdmittedManifestV1.fromVerifier(capture,oldParent,manifest,parsed,attribution,normalized,
                request.selectedIdentifiers(),children);
    }

    private static boolean matches(RepositoryCaptureAttestation c,AdmittedManifestParentMemberReferenceV1 p,byte[] bytes){
        if(!c.sourceId().equals(p.sourceId())||!c.snapshotId().equals(p.snapshotId())
                ||!c.contentFingerprint().equals(p.repositoryCaptureFingerprint())
                ||p.orderedPosition()>=c.regularMembers().size()) return false;
        var attested=c.regularMembers().get(p.orderedPosition());
        return attested.parentSourceId().equals(p.sourceId())&&attested.parentSnapshotId().equals(p.snapshotId())
                &&attested.parentContentFingerprint().equals(p.repositoryCaptureFingerprint())
                &&attested.normalizedRepositoryRelativePath().equals(p.normalizedRepositoryRelativePath())
                &&attested.rawByteLength().equals(p.rawByteLength())
                &&attested.rawMemberFingerprint().equals(p.rawSourceMemberFingerprint())
                &&p.rawByteLength().equals(BigInteger.valueOf(bytes.length))
                &&p.rawSourceMemberFingerprint().equals(RawSourceMemberFingerprint.calculate(bytes));
    }

    private static NormalizedScenarioOccurrenceInputV1 child(RepositoryCaptureAttestation capture,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent,
            NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity manifest,
            EvidenceGovernanceNormalizedManifestV1.Scenario s,ScenarioAuthorityNormalizationContractsV1 c){
        var claimed=new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(s.claimedIdentity().authority(),
                s.claimedIdentity().scenarioKey(),s.claimedIdentity().identityScheme());
        var occurrence=new NormalizedScenarioOccurrenceInputV1.ScenarioDeclarationOccurrenceIdentity(manifest,
                s.structuralPath(),s.declarationOccurrenceIdentityVersion());
        List<NormalizedScenarioOccurrenceInputV1.NormalizedStep> given=new ArrayList<>(),when=new ArrayList<>(),then=new ArrayList<>();
        for(var step:s.steps()){
            var value=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(claimed,
                    StepSemanticFingerprintInput.Phase.valueOf(step.phase().name()),BigInteger.valueOf(step.ordinal()),
                    step.identityVersion(),step.exactAuthoredText(),step.semanticVersion());
            switch(step.phase()){case GIVEN->given.add(value);case WHEN->when.add(value);case THEN->then.add(value);}
        }
        var op=s.operationReference();
        var operation=new NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference(claimed,op.role(),
                op.datumIdentityVersion(),op.targetProfile(),op.method(),op.path(),op.semanticVersion());
        var rules=s.businessRuleReferences().stream().map(r->new NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference(
                BigInteger.valueOf(r.authoredPosition()),claimed,r.referencedAuthority(),r.stableRuleKey(),r.identityScheme(),
                r.datumIdentityVersion(),r.semanticVersion())).toList();
        return new NormalizedScenarioOccurrenceInputV1(c.sourceNormalizationVersion(),c.scenarioSemanticVersion(),
                ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION,occurrence,parent,capture,
                s.structuralPath(),claimed,s.title(),s.given(),given,s.when(),when,s.then(),then,operation,rules);
    }
    private static AdmittedManifestVerificationRejectionV1 reject(AdmittedManifestVerificationRejectionV1.Code code){
        return new AdmittedManifestVerificationRejectionV1(code,null,null,null,List.of());
    }
}
