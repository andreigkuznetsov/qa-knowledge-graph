package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityPartitionSourceVerifierV2Test {
    @Test void ONE_MEMBER_PARTITION() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); var v=verify(f); assertEquals(1,v.attributedMemberOutcomes().size()); }
    @Test void MULTIPLE_MEMBER_PARTITION() { var p=twoMembers(); assertEquals(2,p.verified.attributedMemberOutcomes().size()); }
    @Test void EXACT_MEMBER_ORDER() { var p=twoMembers(); assertEquals(p.capture.regularMembers(),p.verified.attributedMemberOutcomes().stream().map(x->x.input().parentMemberReference()).toList()); }
    @Test void STRUCTURALLY_REJECTED_MEMBER() { var f=rejected("a.json"); var v=ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(),List.of()); assertEquals(1,v.attributedMemberOutcomes().size()); assertTrue(v.manifestOutcomes().isEmpty()); }
    @Test void ADMITTED_COMPOSED_MEMBER() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,verify(f).manifestOutcomes().getFirst()); }
    @Test void ADMITTED_UNAVAILABLE_MEMBER() { var f=admitted("a.json",List.of(child("a.json",0,"same"),child("a.json",1,"u","future-source")),true); var v=verify(f); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,v.manifestOutcomes().getFirst()); assertTrue(v.composedNormalizedManifests().isEmpty()); assertEquals(2,v.scenarioOccurrenceOutcomes().size()); assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,v.scenarioIdentityGroups().getFirst().state()); }
    @Test void OMITTED_MEMBER() { var p=twoMembers(); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(p.capture,p.report,List.of(p.compositions.getFirst()),List.of(p.manifests.getFirst()),List.of(p.group))); }
    @Test void EXTRA_MEMBER() { var p=twoMembers(); var extra=rejectedComposition(p.capture.regularMembers().get(1),"orders"); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(p.capture,p.report,List.of(p.compositions.get(0),extra),p.manifests,List.of(p.group))); }
    @Test void DUPLICATE_MEMBER() { var p=twoMembers(); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(p.capture,p.report,List.of(p.compositions.get(0),p.compositions.get(0)),p.manifests,List.of(p.group))); }
    @Test void REORDERED_MEMBER() { var p=twoMembers(); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(p.capture,p.report,List.of(p.compositions.get(1),p.compositions.get(0)),p.manifests,List.of(p.group))); }
    @Test void FOREIGN_CAPTURE_MEMBER() { var local=rejected("a.json"); var foreign=rejected("b.json"); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(local.capture,local.report,List.of(foreign.composition),List.of(),List.of())); }
    @Test void CROSS_AUTHORITY_MEMBER() { var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json","b.json"); var a=rejectedComposition(f.refs().get(0),"orders"); var b=rejectedComposition(f.refs().get(1),"payments"); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.att(),report(f.att(),List.of(a,b)),List.of(a,b),List.of(),List.of())); }
    @Test void MANIFEST_OCCURRENCE_SUBSTITUTION_CLOSED_BY_CONSTRUCTION() throws Exception { var outcome=ManifestSemanticCompositionOutcomeV1.Composed.class.getDeclaredConstructors()[0]; assertFalse(Modifier.isPublic(outcome.getModifiers())); var verifier=Files.readString(repositoryRoot().resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioAuthorityPartitionSourceVerifierV2.java")); assertTrue(verifier.contains("composeAdmitted(outcome)")); assertTrue(verifier.contains("member.fingerprint().equals(revalidated.fingerprint())")); var admitted=Files.readString(repositoryRoot().resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/VerifiedAdmittedManifestV1.java")); assertTrue(admitted.contains("o.memberPath().equals(p.normalizedRepositoryRelativePath())")); assertTrue(admitted.contains("o.parentFingerprint().equals(p.parentContentFingerprint())")); }
    @Test void NORMALIZED_MANIFEST_SUBSTITUTION_CLOSED_BY_PROOF_CHAIN() throws Exception { var root=repositoryRoot(); var verifier=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioAuthorityPartitionSourceVerifierV2.java")); var source=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/VerifiedScenarioAuthorityPartitionSourceV2.java")); assertTrue(Arrays.stream(ScenarioAuthorityPartitionSourceVerifierV2.class.getDeclaredMethods()).filter(m->m.getName().equals("verify")).flatMap(m->Arrays.stream(m.getParameterTypes())).noneMatch(t->t.getSimpleName().contains("Normalized"))); assertTrue(source.contains(".map(VerifiedAdmittedManifestV1::normalizedManifest)")); assertTrue(source.contains("filter(ManifestSemanticCompositionOutcomeV1.Composed.class::isInstance)")); assertTrue(verifier.contains("new VerifiedScenarioAuthorityPartitionSourceV2(")); assertFalse(verifier.contains("NormalizedManifestDatum")); assertFalse(source.contains("NormalizedManifestDatum")); assertFalse(Arrays.stream(VerifiedScenarioAuthorityPartitionSourceV2.class.getDeclaredConstructors()).anyMatch(c->Arrays.stream(c.getParameterTypes()).anyMatch(t->t.getSimpleName().contains("Normalized")))); }
    @Test void COMPOSED_UNIQUE() { var f=admitted("a.json",List.of(child("a.json",0,"same")),false); var v=verify(f); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,v.manifestOutcomes().getFirst()); assertEquals(VerifiedScenarioIdentityGroupV1.State.UNIQUE,v.scenarioIdentityGroups().getFirst().state()); assertEquals(1,v.manifestOutcomes().size()); assertEquals(1,v.scenarioIdentityGroups().size()); }
    @Test void COMPOSED_DUPLICATE_EQUIVALENT() { var f=admitted("a.json",List.of(child("a.json",0,"same"),child("a.json",1,"same")),false); var v=verify(f); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,v.manifestOutcomes().getFirst()); assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT,v.scenarioIdentityGroups().getFirst().state()); assertEquals(2,v.scenarioOccurrenceOutcomes().size()); }
    @Test void COMPOSED_DUPLICATE_CONFLICTING() { var f=admitted("a.json",List.of(child("a.json",0,"one"),child("a.json",1,"two")),false); var v=verify(f); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,v.manifestOutcomes().getFirst()); assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING,v.scenarioIdentityGroups().getFirst().state()); assertEquals(2,v.scenarioOccurrenceOutcomes().size()); }
    @Test void UNAVAILABLE_DUPLICATE_UNCLASSIFIED() { var f=admitted("a.json",List.of(child("a.json",0,"same"),child("a.json",1,"u","future-source")),true); var v=verify(f); assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,v.manifestOutcomes().getFirst()); assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,v.scenarioIdentityGroups().getFirst().state()); assertEquals(2,v.scenarioOccurrenceOutcomes().size()); }
    @Test void GROUP_MISSING_OCCURRENCE() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of())); }
    @Test void GROUP_EXTRA_OCCURRENCE() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); var e=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(ScenarioIdentityGroupComposerV1Test.fixture("a.json"),"a.json",1,"extra")); var g=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,f.group.claimedIdentity(),List.of(f.manifestOutcome.childOutcomes().getFirst(),e)); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of(g))); }
    @Test void GROUP_DUPLICATE_MEMBERSHIP() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of(f.group,f.group))); }
    @Test void GROUP_FOREIGN_GROUP() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); var x=admitted("b.json",List.of(child("b.json",0,"one")),false); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of(x.group))); }
    @Test void GROUP_FOREIGN_OCCURRENCE() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); var e=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(ScenarioIdentityGroupComposerV1Test.fixture("a.json"),"a.json",1,"extra")); var g=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,f.group.claimedIdentity(),List.of(e)); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of(g))); }
    @Test void GROUP_CROSS_CAPTURE_OCCURRENCE() { var f=admitted("a.json",List.of(child("a.json",0,"one")),false); var x=admitted("b.json",List.of(child("b.json",0,"one")),false); var g=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,f.group.claimedIdentity(),List.of(x.manifestOutcome.childOutcomes().getFirst())); assertThrows(IllegalArgumentException.class,()->ScenarioAuthorityPartitionSourceVerifierV2.verify(f.capture,f.report,List.of(f.composition),List.of(f.manifestOutcome),List.of(g))); }
    @Test void GROUP_CROSS_AUTHORITY_OCCURRENCE_CLOSED_BY_CONSTRUCTION() throws Exception { var root=repositoryRoot(); var occurrence=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/NormalizedScenarioOccurrenceInputV1.java")); var group=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioIdentityGroupComposerV1.java")); var verifier=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioAuthorityPartitionSourceVerifierV2.java")); assertTrue(occurrence.contains("ClaimedScenarioIdentity claimedIdentity")); assertTrue(group.contains("identity.equals(o.claimedIdentity())")); assertTrue(verifier.contains("authority.equals(group.claimedIdentity().authority())")); assertTrue(verifier.contains("sameOutcome(expected.get(index), occurrence)")); assertTrue(verifier.contains("sameOccurrence(expected.occurrence(), supplied.occurrence())")); assertTrue(verifier.contains("left.claimedIdentity().equals(right.claimedIdentity())")); }
    @Test void GROUP_CROSS_AUTHORITY_OCCURRENCE_AUTHORITATIVE_MINTING_CLOSURE() throws Exception {
        var root=repositoryRoot(); var production=Files.walk(root).filter(p->p.toString().endsWith(".java"))
                .filter(p->p.toString().contains("src"+java.io.File.separator+"main"+java.io.File.separator+"java"))
                .filter(p->!p.toString().contains("build")).toList();
        var attempt=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioOccurrenceCompositionAttemptV1.java"));
        var outcome=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioOccurrenceCompositionOutcomeV1.java"));
        var input=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/NormalizedScenarioOccurrenceInputV1.java"));
        var group=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioIdentityGroupComposerV1.java"));
        var verifier=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioAuthorityPartitionSourceVerifierV2.java"));
        var concreteMints=production.stream().filter(p->{try{return Files.readString(p).contains("new ScenarioOccurrenceCompositionOutcomeV1.");}catch(Exception e){throw new RuntimeException(e);}}).toList();
        assertEquals(1,concreteMints.size()); assertTrue(concreteMints.getFirst().endsWith("ScenarioOccurrenceCompositionAttemptV1.java"));
        assertFalse(Arrays.stream(ScenarioOccurrenceCompositionOutcomeV1.Composed.class.getDeclaredConstructors()).anyMatch(c->Modifier.isPublic(c.getModifiers())||Modifier.isProtected(c.getModifiers())));
        assertFalse(Arrays.stream(ScenarioOccurrenceCompositionOutcomeV1.Unavailable.class.getDeclaredConstructors()).anyMatch(c->Modifier.isPublic(c.getModifiers())||Modifier.isProtected(c.getModifiers())));
        assertTrue(attempt.contains("validateOccurrence(o)")); assertTrue(attempt.contains("ScenarioOccurrenceCorrespondenceValidatorV1.validateAndConstruct(o)")); assertTrue(attempt.contains("revalidateProof")); assertTrue(attempt.contains("attemptScenarioOccurrenceCompositionV1(claimed.occurrence())"));
        assertTrue(input.contains("ClaimedScenarioIdentity claimedIdentity")); assertTrue(attempt.contains("m.parentFingerprint().equals(p.parentContentFingerprint())")); assertTrue(attempt.contains("a.regularMembers().contains(p)"));
        assertTrue(group.contains("if(!identity.equals(o.claimedIdentity()))throw new IllegalArgumentException")); assertTrue(group.contains("compose(DUPLICATE_OUTCOME_CONTRACT,group.claimedIdentity(),group.occurrences())"));
        assertTrue(verifier.contains("authority.equals(group.claimedIdentity().authority())")); assertTrue(verifier.contains("sameOutcome(expected.get(index), occurrence)")); assertTrue(verifier.contains("left.claimedIdentity().equals(right.claimedIdentity())"));
        assertTrue(production.stream().filter(p->{try{return Files.readString(p).contains("revalidateProof(");}catch(Exception e){throw new RuntimeException(e);}}).allMatch(p->p.endsWith("ManifestSemanticCompositionAttemptV1.java")||p.endsWith("ScenarioIdentityGroupComposerV1.java")||p.endsWith("ScenarioOccurrenceCompositionAttemptV1.java")));
    }

    @Test void AUTHORITY_ORIGIN_IS_VERIFIED_ADMITTED_MANIFEST_AUTHORITY() throws Exception {
        var root=repositoryRoot();
        var admitted=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/AdmittedManifestVerifierV1.java"));
        var verified=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/VerifiedAdmittedManifestV1.java"));
        var mapper=Files.readString(root.resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ManifestSemanticCompositionAttemptV1.java"));
        assertTrue(admitted.contains("for(var scenario:normalized.authoredScenarios()) children.add(child(capture,oldParent,manifest,scenario,contracts))"));
        assertTrue(admitted.contains("VerifiedAdmittedManifestV1.fromVerifier(capture,oldParent,manifest,parsed,attribution,normalized"));
        var normalizedAdmitted=admitted.replaceAll("\\s+","");
        assertTrue(normalizedAdmitted.contains("newScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(s.claimedIdentity().authority(),s.claimedIdentity().scenarioKey(),s.claimedIdentity().identityScheme())"));
        assertTrue(admitted.contains("new NormalizedScenarioOccurrenceInputV1(c.sourceNormalizationVersion()"));
        assertTrue(verified.contains("attribution.authority(),normalized,normalized.format()"));
        assertTrue(verified.contains("!x.claimedIdentity().authority().equals(a)"));
        assertTrue(mapper.contains("var expected=i.manifest().authoredScenarios()"));
        assertTrue(mapper.contains("ScenarioOccurrenceCompositionAttemptV1.revalidateProof(child)"));
    }

    @Test void oneMemberComposedPartitionRetainsEveryAuthoritativeProof() {
        var fixture = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var verified = verify(fixture);
        assertEquals("orders", verified.claimedAuthority());
        assertEquals(fixture.capture, verified.capture());
        assertEquals(List.of(fixture.composition), verified.attributedMemberOutcomes());
        assertEquals(List.of(fixture.manifestOutcome), verified.manifestOutcomes());
        assertEquals(List.of(fixture.group), verified.scenarioIdentityGroups());
        assertEquals(1, verified.composedManifests().size());
        assertEquals(1, verified.scenarioOccurrenceOutcomes().size());
    }

    @Test void unavailableAndRejectedRemainPartitionEvidenceWithoutNormalizedManifestEligibility() {
        var unavailable = admitted("a.json", List.of(
                child("a.json", 0, "same"), child("a.json", 1, "u", "future-source")), true);
        var unavailableVerified = verify(unavailable);
        assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,
                unavailableVerified.manifestOutcomes().getFirst());
        assertTrue(unavailableVerified.composedManifests().isEmpty());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,
                unavailableVerified.scenarioIdentityGroups().getFirst().state());

        var rejected = rejected("a.json");
        var rejectedVerified = ScenarioAuthorityPartitionSourceVerifierV2.verify(
                rejected.capture, rejected.report, List.of(rejected.composition), List.of(), List.of());
        assertTrue(rejectedVerified.manifestOutcomes().isEmpty());
        assertTrue(rejectedVerified.scenarioOccurrenceOutcomes().isEmpty());
    }

    @Test void exactMultipleMemberOrderComesFromDerivationReport() {
        var captureFixture = ScenarioIdentityGroupComposerV1Test.fixture("a.json", "b.json");
        var first = admitted(captureFixture, List.of(child(captureFixture,
                new ChildSpec("a.json", 0, "same", ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))), false);
        var second = admitted(captureFixture, List.of(child(captureFixture,
                new ChildSpec("b.json", 0, "same", ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))), false);
        var compositions = List.of(first.composition, second.composition);
        var manifests = List.of(first.manifestOutcome, second.manifestOutcome);
        var group = ScenarioIdentityGroupComposerV1.compose(
                ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,
                manifests.getFirst().childOutcomes().getFirst().occurrence().claimedIdentity(),
                List.of(manifests.getFirst().childOutcomes().getFirst(),
                        manifests.get(1).childOutcomes().getFirst()));
        var report = report(captureFixture.att(), compositions);
        var verified = ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, compositions, manifests, List.of(group));
        assertEquals(captureFixture.refs(), verified.attributedMemberOutcomes().stream()
                .map(value -> value.input().parentMemberReference()).toList());
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(second.composition, first.composition), manifests, List.of(group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(first.composition), List.of(first.manifestOutcome), List.of(group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                captureFixture.att(), report, List.of(first.composition, first.composition), manifests, List.of(group)));
    }

    @Test void manifestOutcomeMustExactlyMatchAttributedMember() {
        var first = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition),
                List.of(foreign.manifestOutcome), List.of(first.group)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition), List.of(), List.of(first.group)));
        var composed = (ManifestSemanticCompositionOutcomeV1.Composed) first.manifestOutcome;
        var substituted = new ManifestSemanticCompositionOutcomeV1.Composed(
                composed.manifest(), composed.childOutcomes(), composed.scenarioFingerprints(),
                new ManifestSemanticFingerprint(ManifestSemanticFingerprint.VALUE_PREFIX + "0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                first.capture, first.report, List.of(first.composition),
                List.of(substituted), List.of(first.group)));
    }

    @Test void groupsMustCoverExactManifestOccurrencesOnce() {
        var fixture = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(fixture.group, fixture.group)));
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(foreign.group)));
    }

    @Test void everyManifestAndDuplicateCombinationRemainsIndependent() {
        assertCombination(List.of(child("a.json", 0, "same")), false,
                VerifiedScenarioIdentityGroupV1.State.UNIQUE);
        assertCombination(List.of(child("a.json", 0, "same"), child("a.json", 1, "same")), false,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT);
        assertCombination(List.of(child("a.json", 0, "one"), child("a.json", 1, "two")), false,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING);
        assertCombination(List.of(child("a.json", 0, "same"), child("a.json", 1, "u", "future-source")), true,
                VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED);
    }

    @Test void foreignCaptureAndInternallyConsistentForeignPartitionAreRejected() {
        var local = admitted("a.json", List.of(child("a.json", 0, "one")), false);
        var foreign = admitted("b.json", List.of(child("b.json", 0, "one")), false);
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                local.capture, foreign.report, List.of(foreign.composition),
                List.of(foreign.manifestOutcome), List.of(foreign.group)));
    }

    @Test void crossAuthorityMemberCannotEnterTheDerivedPartition() {
        var fixture = ScenarioIdentityGroupComposerV1Test.fixture("a.json", "b.json");
        var orders = rejectedComposition(fixture.refs().getFirst(), "orders");
        var payments = rejectedComposition(fixture.refs().get(1), "payments");
        var report = report(fixture.att(), List.of(orders, payments));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.att(), report, List.of(orders, payments), List.of(), List.of()));
    }

    @Test void verifiedTypeIsMintedOnlyByTheVerificationBoundary() {
        assertTrue(Arrays.stream(VerifiedScenarioAuthorityPartitionSourceV2.class.getDeclaredConstructors())
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())));
        assertTrue(Arrays.stream(VerifiedScenarioAuthorityPartitionSourceV2.class.getMethods())
                .noneMatch(method -> method.getName().equals("verified") || method.getName().equals("of")));
        assertEquals(1, Arrays.stream(ScenarioAuthorityPartitionSourceVerifierV2.class.getMethods())
                .filter(method -> method.getName().equals("verify")).count());
    }

    @Test void soleVerifiedPartitionConstructorCallsiteIsVerifier() throws Exception {
        var root=Path.of("..").toAbsolutePath().normalize(); var files=Files.walk(root).filter(p->p.toString().endsWith(".java")).filter(p->!p.toString().contains("build")).filter(p->!p.toString().contains("src\\test")).toList();
        var verifier=files.stream().filter(p->p.getFileName().toString().equals("ScenarioAuthorityPartitionSourceVerifierV2.java")).findFirst().orElseThrow();
        var count=files.stream().mapToInt(p->{try{return Files.readString(p).split("new\\s+VerifiedScenarioAuthorityPartitionSourceV2\\s*\\(",-1).length-1;}catch(Exception e){throw new RuntimeException(e);}}).sum();
        assertEquals(1,count); assertTrue(Files.readString(verifier).contains("new VerifiedScenarioAuthorityPartitionSourceV2"));
    }

    @Test void verifierBoundaryHasNoExtractorCompatibilityDtos() throws Exception {
        var root=Path.of("..").toAbsolutePath().normalize(); var module=root.resolve("qa-evidence-governance-core");
        var files=Files.walk(module.resolve("src/main")).filter(p->p.toString().endsWith(".java")).toList();
        for(var p:files){var s=Files.readString(p); assertFalse(s.contains("NormalizedManifestDatum")); assertFalse(s.contains("ScenarioAuthorityNormalizedProcessingV1")); assertFalse(s.contains("qa-model-extractor"));}
        assertFalse(Files.readString(module.resolve("build.gradle")).contains("qa-model-extractor"));
    }

    @Test void repositoryProductionEvidenceGovernancePackageIsNotSplit() throws Exception {
        var root=Path.of("..").toAbsolutePath().normalize(); var owner=root.resolve("qa-evidence-governance-core");
        var foreign=Files.walk(root).filter(p->p.toString().endsWith(".java")).filter(p->p.normalize().toString().contains("src"+java.io.File.separator+"main"+java.io.File.separator+"java")).filter(p->!p.normalize().startsWith(owner.resolve("src/main").normalize())).filter(p->{try{return Files.readString(p).contains("package ru.kuznetsov.qaip.evidencegovernance");}catch(Exception e){throw new RuntimeException(e);}}).toList();
        assertTrue(foreign.isEmpty(),foreign::toString);
    }

    private static Path repositoryRoot() {
        var here=Path.of("").toAbsolutePath().normalize();
        for(var p=here;p!=null;p=p.getParent()) if(Files.exists(p.resolve("settings.gradle")) && Files.exists(p.resolve("qa-evidence-governance-core"))) return p;
        throw new IllegalStateException("repository root not found");
    }

    private static void assertCombination(List<ChildSpec> specs, boolean unavailable,
                                          VerifiedScenarioIdentityGroupV1.State state) {
        var fixture = admitted("a.json", specs, unavailable);
        var verified = verify(fixture);
        assertEquals(state, verified.scenarioIdentityGroups().getFirst().state());
        assertEquals(unavailable, verified.manifestOutcomes().getFirst()
                instanceof ManifestSemanticCompositionOutcomeV1.Unavailable);
    }

    private static VerifiedScenarioAuthorityPartitionSourceV2 verify(AdmittedFixture fixture) {
        return ScenarioAuthorityPartitionSourceVerifierV2.verify(
                fixture.capture, fixture.report, List.of(fixture.composition),
                List.of(fixture.manifestOutcome), List.of(fixture.group));
    }

    private static TwoMembers twoMembers() {
        var c=ScenarioIdentityGroupComposerV1Test.fixture("a.json","b.json");
        var a=admitted(c,List.of(child(c,new ChildSpec("a.json",0,"same",ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))),false);
        var b=admitted(c,List.of(child(c,new ChildSpec("b.json",0,"same",ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION))),false);
        var ms=List.of(a.manifestOutcome,b.manifestOutcome); var cs=List.of(a.composition,b.composition);
        var g=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,ms.getFirst().childOutcomes().getFirst().occurrence().claimedIdentity(),List.of(ms.getFirst().childOutcomes().getFirst(),ms.get(1).childOutcomes().getFirst()));
        return new TwoMembers(c.att(),report(c.att(),cs),cs,ms,g,ScenarioAuthorityPartitionSourceVerifierV2.verify(c.att(),report(c.att(),cs),cs,ms,List.of(g)));
    }

    private static AdmittedFixture admitted(String path, List<ChildSpec> specs, boolean unavailable) {
        var capture = ScenarioIdentityGroupComposerV1Test.fixture(path);
        return admitted(capture, specs.stream().map(spec -> child(capture, spec)).toList(), unavailable);
    }

    private static AdmittedFixture admitted(ScenarioIdentityGroupComposerV1Test.Fixture fixture,
                                             List<NormalizedScenarioOccurrenceInputV1> children,
                                             boolean unavailable) {
        var manifest = ManifestSemanticCompositionAttemptV1Test.verified(fixture, children);
        var childOutcomes = children.stream().map(ManifestSemanticCompositionAttemptV1Test::attemptChild).toList();
        var outcome = ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(
                NormalizedManifestSemanticCompositionInputV1.selectedV1(manifest, childOutcomes));
        assertEquals(unavailable, outcome instanceof ManifestSemanticCompositionOutcomeV1.Unavailable);
        var composition = AttributedMemberOutcomeComposerV1.composeAdmitted(outcome);
        var group = ScenarioIdentityGroupComposerV1.compose(
                ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,
                children.getFirst().claimedIdentity(), childOutcomes);
        var report = fixture.refs().size() == 1 ? report(fixture.att(), List.of(composition)) : null;
        return new AdmittedFixture(fixture.att(), report,
                composition, outcome, group);
    }

    private static RejectedFixture rejected(String path) {
        var fixture = ScenarioIdentityGroupComposerV1Test.fixture(path);
        var composition = rejectedComposition(fixture.refs().getFirst(), "orders");
        return new RejectedFixture(fixture.att(), report(fixture.att(), List.of(composition)), composition);
    }

    private static AttributedMemberOutcomeComposerV1.Composition rejectedComposition(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parent,
            String authority) {
        var diagnostic = ScenarioSchemaDiagnostic.v1("", "required",
                ScenarioSchemaDiagnostic.RULE_PREFIX + "/required",
                List.of(new ScenarioSchemaDiagnostic.TextParameter("missingProperty", "authority")));
        var input = new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER, parent, authority,
                AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                Optional.empty(), AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                List.of(diagnostic), Optional.empty(), Optional.empty());
        return AttributedMemberOutcomeComposerV1.composeRejected(input);
    }

    private static RepositoryDerivationReportFingerprintInput report(
            RepositoryCaptureAttestation capture,
            List<AttributedMemberOutcomeComposerV1.Composition> compositions
    ) {
        var outcomes = compositions.stream().map(composition ->
                new RepositoryDerivationReportFingerprintInput.AttributedMemberRetained(
                        composition.input().parentMemberReference(), composition.input().claimedAuthority(),
                        SemanticProvenanceOutputReference.verified(composition))).toList();
        var provenance = outcomes.stream().map(outcome ->
                SemanticProvenanceAttestation.deriveAttributedMemberOutcome(outcome.attributedMemberOutput())).toList();
        return new RepositoryDerivationReportFingerprintInput(
                RepositoryDerivationReportFingerprintInput.REPORT_CONTRACT_VERSION,
                RepositoryDerivationReportFingerprintInput.ParentRepositoryCapture.verified(capture),
                RepositoryDerivationReportFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                RepositoryDerivationReportFingerprintInput.STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER,
                new ArrayList<>(outcomes),
                capture.unsupportedMatchingEntries().stream().map(entry ->
                        new RepositoryDerivationReportFingerprintInput.UnsupportedMatchingEntry(
                                entry.normalizedRepositoryRelativePath(), entry.entryKind(),
                                entry.stableDiagnosticCode())).toList(),
                new ArrayList<>(provenance));
    }

    private static ChildSpec child(String path, int index, String title) {
        return new ChildSpec(path, index, title, ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION);
    }
    private static ChildSpec child(String path, int index, String title, String sourceVersion) {
        return new ChildSpec(path, index, title, sourceVersion);
    }
    private static NormalizedScenarioOccurrenceInputV1 child(
            ScenarioIdentityGroupComposerV1Test.Fixture fixture, ChildSpec spec) {
        return ScenarioIdentityGroupComposerV1Test.input(
                fixture, spec.path, spec.index, spec.title, spec.sourceVersion);
    }

    private record ChildSpec(String path, int index, String title, String sourceVersion) {}
    private record AdmittedFixture(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report,
            AttributedMemberOutcomeComposerV1.Composition composition,
            ManifestSemanticCompositionOutcomeV1 manifestOutcome,
            VerifiedScenarioIdentityGroupV1 group) {}
    private record RejectedFixture(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput report,
            AttributedMemberOutcomeComposerV1.Composition composition) {}
    private record TwoMembers(RepositoryCaptureAttestation capture, RepositoryDerivationReportFingerprintInput report,
                              List<AttributedMemberOutcomeComposerV1.Composition> compositions,
                              List<ManifestSemanticCompositionOutcomeV1> manifests,
                              VerifiedScenarioIdentityGroupV1 group,
                              VerifiedScenarioAuthorityPartitionSourceV2 verified) {}
}
