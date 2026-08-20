package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Test-side inventory and static evidence for unexpected-failure handling boundaries. */
class ScenarioIdentityGroupV1FailureBoundaryTest {
    enum Kind { NATURALLY_TESTABLE, STRUCTURALLY_CLOSED }
    record Boundary(Kind kind,String evidence){}
    static final Map<String,Boundary> INVENTORY=Map.of(
            "stage-1",natural("unsupportedPrecedesFiniteIntegrityAndUnexpectedIdentityFailuresPropagate"),
            "stage-2",closed("pure final value comparisons; no collaborator or catch surrounds support evaluation"),
            "correspondence",closed("final static deterministic validator; only its typed finite exception is caught"),
            "leaf-attestation",closed("final private construction invoked directly; no injectable collaborator"),
            "scenario-composer",closed("final static composer; exhaustive stable-code conversion has no default"),
            "composed-proof",natural("postOutcomeProofSubstitutionVectors"),
            "unavailable-proof",natural("revalidatesCompleteComposedAndUnavailableProofs"));

    @Test void inventoriesEveryBoundaryExactlyOnce(){assertEquals(Set.of("stage-1","stage-2","correspondence","leaf-attestation","scenario-composer","composed-proof","unavailable-proof"),INVENTORY.keySet());assertEquals(3,INVENTORY.values().stream().filter(v->v.kind()==Kind.NATURALLY_TESTABLE).count());assertEquals(4,INVENTORY.values().stream().filter(v->v.kind()==Kind.STRUCTURALLY_CLOSED).count());INVENTORY.values().forEach(v->assertFalse(v.evidence().isBlank()));}

    @Test void attemptHasOnlyTypedFiniteCatchAndExhaustiveComposerSwitch() throws Exception {
        String source=Files.readString(repositoryRoot().resolve("qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/ScenarioOccurrenceCompositionAttemptV1.java"));
        assertFalse(source.contains("catch (IllegalArgumentException"));
        assertFalse(source.contains("catch (RuntimeException"));
        assertTrue(source.contains("catch (ScenarioOccurrenceIntegrityExceptionV1"));
        assertTrue(source.contains("catch (ScenarioSemanticCompositionException"));
        String mapping=source.substring(source.indexOf("mapComposerRejection"));
        assertFalse(mapping.contains("default->"));assertFalse(mapping.contains("default ->"));
        for(var code:ScenarioSemanticCompositionException.Code.values())assertTrue(mapping.contains("case "+code.name()),code.name());
        assertTrue(mapping.contains("case UNSUPPORTED_CONTRACT->throw e"));
    }
    private static Boundary closed(String e){return new Boundary(Kind.STRUCTURALLY_CLOSED,e);}
    private static Boundary natural(String e){return new Boundary(Kind.NATURALLY_TESTABLE,e);}
    private static Path repositoryRoot(){Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();while(p!=null&&!Files.exists(p.resolve("settings.gradle")))p=p.getParent();return Objects.requireNonNull(p,"repository root");}
}
