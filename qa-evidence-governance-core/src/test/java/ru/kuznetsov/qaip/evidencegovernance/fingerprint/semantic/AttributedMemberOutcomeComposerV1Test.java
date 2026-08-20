package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AttributedMemberOutcomeComposerV1Test {
    private static final HexFormat HEX=HexFormat.of();

    @Test void composedDerivesExactOccurrenceAndFingerprint(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var child=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"one");
        var manifest=ManifestSemanticCompositionAttemptV1Test.verified(f,List.of(child));
        var outcome=ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(
                NormalizedManifestSemanticCompositionInputV1.selectedV1(manifest,List.of(
                        ManifestSemanticCompositionAttemptV1Test.attemptChild(child))));
        var result=AttributedMemberOutcomeComposerV1.composeAdmitted(outcome);
        assertEquals(Optional.of(((ManifestSemanticCompositionOutcomeV1.Composed)outcome).fingerprint()),
                result.input().manifestSemanticFingerprint());
        assertEquals(manifest.occurrenceIdentity().memberPath(),
                result.input().manifestOccurrenceIdentity().orElseThrow().normalizedRepositoryRelativePath());
        assertEquals(AttributedMemberOutcomeFingerprintEncoder.fingerprint(result.input()),result.fingerprint());
    }

    @Test void unavailableReasonsProduceOneCanonicalAttributedOutcome(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var unsupported=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"same","future-source");
        var integrity=ManifestSemanticCompositionAttemptV1Test.integrityInput(
                ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"same"));
        var unsupportedResult=composeUnavailable(f,List.of(unsupported));
        var integrityResult=composeUnavailable(f,List.of(integrity));
        assertTrue(unsupportedResult.input().manifestOccurrenceIdentity().isPresent());
        assertTrue(unsupportedResult.input().manifestSemanticFingerprint().isEmpty());
        assertArrayEquals(AttributedMemberOutcomeFingerprintEncoder.encode(unsupportedResult.input()),
                AttributedMemberOutcomeFingerprintEncoder.encode(integrityResult.input()));
        assertEquals(unsupportedResult.fingerprint(),integrityResult.fingerprint());
        var repeated=composeUnavailable(f,List.of(unsupported));
        assertEquals(unsupportedResult.input(),repeated.input());assertEquals(unsupportedResult.fingerprint(),repeated.fingerprint());
    }

    @Test void admittedUnavailableGoldenBytesAndFingerprint(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var unsupported=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"same","future-source");
        var result=composeUnavailable(f,List.of(unsupported));
        assertEquals("000000000000003451414950005343454e4152494f5f415554484f524954595f415454524942555445445f4d454d4245525f4f5554434f4d4500563100000000000000347363656e6172696f2d617574686f726974792d617474726962757465642d6d656d6265722d6f7574636f6d652d6331346e2d7631000000000000000a7368612d3235362d763100000000000000347363656e6172696f2d617574686f726974792d617474726962757465642d6d656d6265722d6f7574636f6d652d6331346e2d763100000000000000047265706f0000000000000004736e617000000000000000287363656e6172696f2d617574686f726974792d7265706f7369746f72792d636170747572652d763100000000000000697363656e6172696f2d617574686f726974792d7265706f7369746f72792d636170747572652d76313a333934343937633334626665333866623534326530643431383261336266373363386634303335346334663366643863393731626662306533313431383335320000000000000006612e6a736f6e0000000000000001000000000000000a7368612d3235362d7631000000000000004b7368612d3235362d76313a3462663531323266333434353534633533626465326562623863643262376533643136303061643633316333383561356437636365323363373738353435396100000000000000066f726465727300000000000000217363656e6172696f2d617574686f726974792d6a736f6e2d7061727365722d763100000000000000217363656e6172696f2d617574686f726974792d6174747269627574696f6e2d76310000000000000006504152534544000000000000000a4154545249425554454400000000000000002a716169702d7363656e6172696f2d617574686f726974792d6d616e69666573742d736368656d612d763100000000000000155354525543545552414c4c595f41444d4954544544000000000000000001000000000000002d716169702d7363656e6172696f2d6d616e69666573742d6f6363757272656e63652d6964656e746974792d763100000000000000047265706f0000000000000004736e617000000000000000287363656e6172696f2d617574686f726974792d7265706f7369746f72792d636170747572652d763100000000000000697363656e6172696f2d617574686f726974792d7265706f7369746f72792d636170747572652d76313a333934343937633334626665333866623534326530643431383261336266373363386634303335346334663366643863393731626662306533313431383335320000000000000006612e6a736f6e00",HEX.formatHex(AttributedMemberOutcomeFingerprintEncoder.encode(result.input())));
        assertEquals("scenario-authority-attributed-member-outcome-v1:e0a23a5b6aa06c4a91e4f50934c7efbf08aac6bfc591d2237199f64e400a55a0",result.fingerprint().value());
    }

    @Test void mixedUnavailableChildrenRemainFingerprintAbsent(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var composed=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"ok");
        var unsupported=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",1,"u","future-source");
        var integrity=ManifestSemanticCompositionAttemptV1Test.integrityInput(
                ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"i"));
        assertTrue(composeUnavailable(f,List.of(composed,unsupported,integrity)).input()
                .manifestSemanticFingerprint().isEmpty());
    }

    @Test void proofAndStateCannotBeCallerSelected(){
        assertThrows(NullPointerException.class,()->AttributedMemberOutcomeComposerV1.composeAdmitted(null));
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var child=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"one");
        var manifest=ManifestSemanticCompositionAttemptV1Test.verified(f,List.of(child));
        var real=(ScenarioOccurrenceCompositionOutcomeV1.Composed)
                ManifestSemanticCompositionAttemptV1Test.attemptChild(child);
        var other=(ScenarioOccurrenceCompositionOutcomeV1.Composed)ManifestSemanticCompositionAttemptV1Test.attemptChild(
                ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"other"));
        var substituted=new ScenarioOccurrenceCompositionOutcomeV1.Composed(child,real.request(),other.composition());
        var claimed=new ManifestSemanticCompositionOutcomeV1.Composed(manifest,List.of(substituted),
                List.of(other.fingerprint()),ManifestSemanticFingerprintEncoder.fingerprint(
                        ManifestSemanticFingerprintComposerV1.input(manifest,List.of(other.fingerprint()))));
        assertThrows(IllegalArgumentException.class,()->AttributedMemberOutcomeComposerV1.composeAdmitted(claimed));
    }

    @Test void rejectedConstructionCannotCarryManifestEvidence(){
        var admitted=baseInput(AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED,
                List.of(),Optional.of(occurrence()),Optional.empty());
        assertThrows(IllegalArgumentException.class,()->AttributedMemberOutcomeComposerV1.composeRejected(admitted));
    }

    @Test void authoritativeApiExposesNoOptionalManifestStateOrMintableComposition(){
        assertTrue(Arrays.stream(AttributedMemberOutcomeComposerV1.Composition.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        var admitted=Arrays.stream(AttributedMemberOutcomeComposerV1.class.getMethods())
                .filter(m->m.getName().equals("composeAdmitted")).toList();
        assertEquals(1,admitted.size());assertArrayEquals(new Class<?>[]{ManifestSemanticCompositionOutcomeV1.class},
                admitted.getFirst().getParameterTypes());
        assertTrue(Arrays.stream(AttributedMemberOutcomeComposerV1.class.getMethods())
                .noneMatch(m->Arrays.asList(m.getParameterTypes()).contains(Optional.class)));
    }

    private static AttributedMemberOutcomeComposerV1.Composition composeUnavailable(
            ScenarioIdentityGroupComposerV1Test.Fixture f,List<NormalizedScenarioOccurrenceInputV1> children){
        var manifest=ManifestSemanticCompositionAttemptV1Test.verified(f,children);
        var outcomes=children.stream().map(ManifestSemanticCompositionAttemptV1Test::attemptChild).toList();
        var outcome=ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(
                NormalizedManifestSemanticCompositionInputV1.selectedV1(manifest,outcomes));
        assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,outcome);
        return AttributedMemberOutcomeComposerV1.composeAdmitted(outcome);
    }
    private static AttributedMemberOutcomeFingerprintInput baseInput(
            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState state,
            List<ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic> diagnostics,
            Optional<AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity> occurrence,
            Optional<ManifestSemanticFingerprint> fingerprint){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");var p=f.refs().getFirst();
        return new AttributedMemberOutcomeFingerprintInput(AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER,
                p,"orders",AttributedMemberOutcomeFingerprintInput.PARSER_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ATTRIBUTION_CONTRACT_IDENTIFIER,
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,Optional.empty(),
                AttributedMemberOutcomeFingerprintInput.SCHEMA_CONTRACT_IDENTIFIER,state,diagnostics,occurrence,fingerprint);
    }
    private static AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity occurrence(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");var p=f.refs().getFirst();
        return new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(p.parentSourceId(),p.parentSnapshotId(),
                p.parentContentFingerprint(),p.normalizedRepositoryRelativePath(),
                AttributedMemberOutcomeFingerprintInput.MANIFEST_OCCURRENCE_IDENTITY_VERSION);
    }
}
