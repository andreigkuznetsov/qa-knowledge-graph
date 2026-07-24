package ru.kuznetsov.qaip.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.model.NodeType;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidence.EvidenceTestFixtures.*;

class CorrectiveChangeDomainTest {
    private final ImpactEvidenceAnalyzer analyzer = new ImpactEvidenceAnalyzer();

    @Test void compatibilityDiagnosticFormattingIsLocaleIndependent() throws Exception {
        var accepted = verifiedTransition("N-1", true);
        var evidenceManifest = manifest(List.of(resolved("subject", "N-1")), List.of());
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            var usDiagnostics = failed(analyzer.analyze(request(accepted, evidenceManifest, "subject")))
                    .failure().diagnostics();
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            var arabicDiagnostics = failed(analyzer.analyze(request(accepted, evidenceManifest, "subject")))
                    .failure().diagnostics();
            assertEquals(usDiagnostics, arabicDiagnostics);
            assertEquals("00000000:N-1", usDiagnostics.getFirst().objectId());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test void duplicateCanonicalMappingFailsDeterministically() throws Exception {
        var accepted = verified("BR-ROOT");
        var evidenceManifest = manifest(
                List.of(resolved("first", "BR-ROOT"), resolved("second", "BR-ROOT")), List.of());
        var result = failed(analyzer.analyze(request(accepted, evidenceManifest, "first")));
        assertEquals(FailureCode.CHANGE_MANIFEST_MISMATCH, result.failure().code());
        assertEquals(List.of("SUPPORTED_CHANGE_ASSERTION_COUNT"), result.failure().diagnostics().stream()
                .map(AnalysisDiagnostic::code).toList());
    }

    @Test void mixedRootsIgnoreCheckAndPreserveBusinessRuleDeclarationOrder() throws Exception {
        var mapper = new ObjectMapper();
        var accepted = verifiedAdded(
                businessRule(mapper, "BR-FIRST", "first"),
                check(mapper, "CHECK-1", "check"),
                businessRule(mapper, "BR-SECOND", "second"));
        var evidenceManifest = manifest(
                List.of(
                        resolved("first", "BR-FIRST"),
                        resolved("second", "BR-SECOND"),
                        resolved("subject", "SUBJECT")),
                List.of(
                        relation("from-first", "subject", "first"),
                        relation("from-second", "subject", "second")));
        var conclusion = completed(analyzer.analyze(request(accepted, evidenceManifest, "subject")));
        var proof = assertInstanceOf(RelationshipPathProof.class, conclusion.proof().orElseThrow());
        assertEquals("BR-FIRST", proof.changedIdentity().value());
    }

    @Test void genuineBusinessRuleKindsRemainDirectlyAffected() throws Exception {
        for (ChangeKind kind : ChangeKind.values()) {
            var accepted = kind == ChangeKind.ADDED ? verified("BR-X") : verifiedExisting("BR-X", kind);
            var result = completed(analyzer.analyze(request(accepted,
                    manifest(List.of(resolved("subject", "BR-X")), List.of()), "subject")));
            assertEquals(kind, ((DirectChangeProof) result.proof().orElseThrow()).changeKind());
        }
    }

    @Test void manifestCannotRelabelAcceptedCheck() throws Exception {
        var accepted = verifiedAdded(check(new ObjectMapper(), "N-1", "check"));
        var result = failed(analyzer.analyze(request(accepted,
                manifest(List.of(resolved("subject", "N-1")), List.of()), "subject")));
        assertEquals(FailureCode.CHANGE_MANIFEST_MISMATCH, result.failure().code());
    }

    @Test void businessRuleTypeEntryAndExitFail() throws Exception {
        for (boolean into : List.of(true, false)) {
            var result = failed(analyzer.analyze(request(verifiedTransition("N-1", into),
                    manifest(List.of(resolved("subject", "N-1")), List.of()), "subject")));
            assertEquals(FailureCode.INCOMPATIBLE_CHANGE_DOMAIN, result.failure().code());
        }
    }

    @Test void hybridPolicyIgnoresWhollyUnrelatedChange() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var accepted = verifiedAdded(businessRule(mapper, "BR-1", "rule"), check(mapper, "C-1", "check"));
        var result = completed(analyzer.analyze(request(accepted,
                manifest(List.of(resolved("subject", "BR-1")), List.of()), "subject")));
        assertEquals(ImpactClassification.AFFECTED, result.classification());
    }

    @Test void supportedChangedRootRequiresAgreeingManifestAssertion() throws Exception {
        var m = manifest(List.of(resolved("subject", "BR-S")), List.of());
        var result = failed(analyzer.analyze(request(verified("BR-ROOT"), m, "subject")));
        assertEquals(FailureCode.CHANGE_MANIFEST_MISMATCH, result.failure().code());
    }

    @Test void relationshipCategoryIdentityCollisionFailsCompatibility() throws Exception {
        var accepted = verifiedWithRelationshipCollision();
        var m = manifest(List.of(resolved("a", "BR-A"), resolved("b", "BR-B")), List.of());
        var result = failed(analyzer.analyze(request(accepted, m, "a")));
        assertEquals(FailureCode.CHANGE_MANIFEST_MISMATCH, result.failure().code());
        assertTrue(result.failure().diagnostics().stream()
                .anyMatch(d -> d.code().equals("UNSUPPORTED_CHANGE_IDENTITY_COLLISION")));
    }

    @Test void authoritativeModelsHaveNoPublicConstructionPath() {
        assertTrue(List.of(DirectChangeProof.class, RelationshipPathProof.class,
                        QualifiedPathStep.class, ImpactConclusion.class).stream()
                .flatMap(type -> List.of(type.getDeclaredConstructors()).stream())
                .noneMatch(c -> Modifier.isPublic(c.getModifiers())));
    }

    @Test void conclusionRejectsProofBoundToAnotherSubjectOrSnapshot() throws Exception {
        var accepted = verified("BR-A");
        var assertionA = resolved("a", "BR-A");
        var direct = new DirectChangeProof(accepted, 0, assertionA);
        var assertionB = resolved("b", "BR-B");
        assertThrows(IllegalArgumentException.class, () -> new ImpactConclusion(assertionB,
                ImpactClassification.AFFECTED, Optional.of(direct), List.of(),
                SliceAnalysisContext.supported(), List.of()));
        var edge = new QualifiedRelationship(relation("r", "b", "a"),
                ((ResolvedIdentity) assertionA.resolution()).identity(),
                ((ResolvedIdentity) assertionB.resolution()).identity());
        var path = new RelationshipPathProof(((ResolvedIdentity) assertionA.resolution()).identity(),
                ((ResolvedIdentity) assertionB.resolution()).identity(), SNAPSHOT,
                List.of(new QualifiedPathStep(edge)));
        var otherSnapshot = new ArtifactIdentityAssertion("a-b2",
                new EvidenceSnapshotRef("jira", "s-2", H1), "b2", NodeType.BUSINESS_RULE,
                assertionB.resolution(), H1, "p-1");
        assertThrows(IllegalArgumentException.class, () -> new ImpactConclusion(otherSnapshot,
                ImpactClassification.AFFECTED, Optional.of(path), List.of(),
                SliceAnalysisContext.supported(), List.of()));
    }

    @Test void unresolvedConclusionRetainsExactAssertionEvidence() throws Exception {
        var assertion = unresolved("subject");
        var result = completed(analyzer.analyze(request(verified("BR-C"),
                manifest(List.of(resolved("changed", "BR-C"), assertion), List.of()), "subject")));
        assertSame(assertion, result.subjectAssertion());
        assertEquals("NO_MAPPING", ((UnresolvedIdentity) result.subjectAssertion().resolution()).reasonCode());
    }

    private ImpactConclusion completed(ImpactEvidenceResult result) {
        return assertInstanceOf(ImpactEvidenceCompleted.class, result).conclusion();
    }
    private ImpactEvidenceFailed failed(ImpactEvidenceResult result) {
        return assertInstanceOf(ImpactEvidenceFailed.class, result);
    }
}
