package ru.kuznetsov.qagraph.change.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;
import ru.kuznetsov.qagraph.change.validation.ChangeDiagnostic;
import ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode;
import ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_TARGET_ALREADY_EXISTS;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_MODEL_DUPLICATE_TARGET;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_MODEL_VERSION_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_STATE_COMPARISON_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_STATE_VERSION_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_BEFORE_STATE_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_TARGET_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_BEFORE_STATE_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_TARGET_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNVERIFIABLE;

class BaseChangeVerifierTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntrinsicChangeValidator intrinsic =
            new IntrinsicChangeValidator();

    @Test
    void addedAbsentTargetShouldSucceedWithoutMatchedState()
            throws Exception {
        BaseVerifiedChange result = success(
                index(),
                added(node("N-1", "After"))
        );

        assertTrue(result.matchedBaseState().isEmpty());
    }

    @Test
    void addedExistingTargetShouldFailRegardlessOfStateContent()
            throws Exception {
        DeclaredChange same = added(node("N-1", "Existing"));
        assertFailure(index(node("N-1", "Existing")), same,
                UNVERIFIABLE, ADDED_TARGET_ALREADY_EXISTS);

        DeclaredChange different = added(node("N-1", "After"));
        assertFailure(index(node("N-1", "Different")), different,
                UNVERIFIABLE, ADDED_TARGET_ALREADY_EXISTS);
    }

    @Test
    void addedNodeShouldNotConflictWithRelationshipOfSameIdentity()
            throws Exception {
        BaseVerifiedChange result = success(
                index(relationship("N-1")),
                added(node("N-1", "After"))
        );

        assertTrue(result.matchedBaseState().isEmpty());
    }

    @Test
    void unsupportedBaseVersionShouldPrecedeTargetRules()
            throws Exception {
        BaseArtifactIndex index = new BaseArtifactIndex(
                new CanonicalQaModelVersion("9.9"),
                List.of()
        );

        assertFailure(index, added(node("N-1", "After")),
                UNSUPPORTED, BASE_MODEL_VERSION_UNSUPPORTED);
    }

    @Test
    void unsupportedUnrelatedArtifactShouldInvalidateBaseView()
            throws Exception {
        CanonicalQaModelVersion unsupported =
                new CanonicalQaModelVersion("9.9");
        ArtifactState unrelated = new NodeArtifactState(
                unsupported,
                node("OTHER", "Unrelated").snapshot()
        );

        assertFailure(index(unrelated), added(node("N-1", "After")),
                UNSUPPORTED, BASE_MODEL_VERSION_UNSUPPORTED);
    }

    @Test
    void duplicateBaseTargetShouldBeUnsupported() throws Exception {
        assertFailure(index(
                        node("N-1", "One"),
                        node("N-1", "Two")
                ), added(node("N-1", "After")),
                UNSUPPORTED, BASE_MODEL_DUPLICATE_TARGET);
    }

    @Test
    void removedMatchingBeforeShouldSucceedAndRetainSafeBaseState()
            throws Exception {
        ArtifactState base = state("""
                {"id":"N-1","type":"CHECK","name":"Current",
                 "metadata":{"a":1,"b":true}}
                """);
        ArtifactState before = state("""
                {"metadata":{"b":true,"a":1},"name":"Current",
                 "type":"CHECK","id":"N-1"}
                """);

        BaseVerifiedChange result = success(
                index(base),
                removed(before)
        );
        ArtifactState retained = result.matchedBaseState().orElseThrow();
        retained.snapshot().withObject("/metadata").put("changed", true);

        assertEquals(base.snapshot(), retained.snapshot());
    }

    @Test
    void removedMissingOrMismatchedBeforeShouldBeUnverifiable()
            throws Exception {
        assertFailure(index(), removed(node("N-1", "Before")),
                UNVERIFIABLE, REMOVED_TARGET_NOT_FOUND);
        assertFailure(index(node("N-1", "Current")),
                removed(node("N-1", "Claimed")),
                UNVERIFIABLE, REMOVED_BEFORE_STATE_MISMATCH);
    }

    @Test
    void missingTargetShouldSuppressUnsupportedBeforeComparison()
            throws Exception {
        assertFailure(index(), removed(invalidSteps(
                        CanonicalQaModelVersion.V0_1)),
                UNVERIFIABLE, REMOVED_TARGET_NOT_FOUND);
    }

    @Test
    void removedShouldUseCanonicalCollectionSemantics() throws Exception {
        ArtifactState base = state("""
                {"id":"N-1","type":"CHECK","name":"Current",
                 "tags":["api","security"],
                 "sourceReferences":[
                 {"sourceId":"S-1","location":{"type":"OTHER","value":"A"}},
                 {"sourceId":"S-2","location":{"type":"OTHER","value":"B"}}]}
                """);
        ArtifactState reordered = state("""
                {"id":"N-1","type":"CHECK","name":"Current",
                 "tags":["security","api"],
                 "sourceReferences":[
                 {"location":{"value":"B","type":"OTHER"},"sourceId":"S-2"},
                 {"location":{"value":"A","type":"OTHER"},"sourceId":"S-1"}]}
                """);

        success(index(base), removed(reordered));
    }

    @Test
    void removedOrderedScenarioDifferenceShouldMismatch() throws Exception {
        ArtifactState base = scenario("[\"first\",\"second\"]");
        ArtifactState before = scenario("[\"second\",\"first\"]");

        assertFailure(index(base), removed(before),
                UNVERIFIABLE, REMOVED_BEFORE_STATE_MISMATCH);
    }

    @Test
    void unsupportedComparisonAndVersionMismatchStayUnsupported()
            throws Exception {
        ArtifactState malformedBase = invalidSteps(
                CanonicalQaModelVersion.V0_1);
        ArtifactState validBefore = validSteps("Before");
        assertFailure(index(malformedBase), removed(validBefore),
                UNSUPPORTED, BASE_STATE_COMPARISON_UNSUPPORTED);

        CanonicalQaModelVersion other = new CanonicalQaModelVersion("9.9");
        ArtifactState wrongVersion = new NodeArtifactState(
                other,
                node("N-1", "Current").snapshot()
        );
        assertFailure(index(wrongVersion), removed(node("N-1", "Current")),
                UNSUPPORTED, BASE_MODEL_VERSION_UNSUPPORTED);
    }

    @Test
    void baseContextMismatchShouldBeUnsupportedBeforeLookup()
            throws Exception {
        BaseArtifactIndex index = new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                List.of()
        );
        DeclaredChange declaration = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                new CanonicalQaModelVersion("9.9"),
                Optional.empty(),
                Optional.of(new NodeArtifactState(
                        new CanonicalQaModelVersion("9.9"),
                        node("N-1", "After").snapshot()
                ))
        );
        IntrinsicallyValidChange candidate = ru.kuznetsov.qagraph.change.validation.ValidationTestFixtures.candidate(
                0,
                declaration
        );

        assertFailure(index, candidate,
                UNSUPPORTED, BASE_STATE_VERSION_MISMATCH);
    }

    @Test
    void modifiedMatchingBeforeShouldSucceedWithoutApplyingAfter()
            throws Exception {
        ArtifactState base = node("N-1", "Before");
        ArtifactState before = state("""
                {"name":"Before","type":"CHECK","id":"N-1"}
                """);
        ArtifactState after = node("N-1", "Completely different after");

        BaseVerifiedChange result = success(
                index(base),
                modified(before, after)
        );

        assertEquals("Before", result.matchedBaseState().orElseThrow()
                .snapshot().path("name").asText());
    }

    @Test
    void modifiedMissingAndMismatchShouldBeUnverifiable()
            throws Exception {
        DeclaredChange declaration = modified(
                node("N-1", "Claimed"),
                node("N-1", "After")
        );
        assertFailure(index(), declaration,
                UNVERIFIABLE, MODIFIED_TARGET_NOT_FOUND);
        assertFailure(index(node("N-1", "Current")), declaration,
                UNVERIFIABLE, MODIFIED_BEFORE_STATE_MISMATCH);
    }

    @Test
    void modifiedUnsupportedComparisonShouldStayUnsupported()
            throws Exception {
        DeclaredChange declaration = modified(
                validSteps("Before"),
                validSteps("After")
        );

        assertFailure(index(invalidSteps(CanonicalQaModelVersion.V0_1)),
                declaration,
                UNSUPPORTED, BASE_STATE_COMPARISON_UNSUPPORTED);
    }

    private BaseVerifiedChange success(
            BaseArtifactIndex index,
            DeclaredChange declaration
    ) {
        return assertInstanceOf(
                BaseVerifiedChange.class,
                new BaseChangeVerifier(index).verify(candidate(declaration))
        );
    }

    private void assertFailure(
            BaseArtifactIndex index,
            DeclaredChange declaration,
            ChangeFailureClassification classification,
            ChangeDiagnosticCode code
    ) {
        assertFailure(index, candidate(declaration), classification, code);
    }

    private void assertFailure(
            BaseArtifactIndex index,
            IntrinsicallyValidChange candidate,
            ChangeFailureClassification classification,
            ChangeDiagnosticCode code
    ) {
        BaseVerificationFailure result = assertInstanceOf(
                BaseVerificationFailure.class,
                new BaseChangeVerifier(index).verify(candidate)
        );
        assertEquals(classification, result.classification());
        assertEquals(List.of(code), result.diagnostics().stream()
                .map(ChangeDiagnostic::code)
                .toList());
        assertThrows(UnsupportedOperationException.class,
                () -> result.diagnostics().clear());
    }

    private IntrinsicallyValidChange candidate(DeclaredChange declaration) {
        return assertInstanceOf(
                IntrinsicallyValidChange.class,
                intrinsic.validate(declaration)
        );
    }

    private DeclaredChange added(ArtifactState after) {
        return declaration(
                ChangeKind.ADDED,
                Optional.empty(),
                Optional.of(after)
        );
    }

    private DeclaredChange removed(ArtifactState before) {
        return declaration(
                ChangeKind.REMOVED,
                Optional.of(before),
                Optional.empty()
        );
    }

    private DeclaredChange modified(
            ArtifactState before,
            ArtifactState after
    ) {
        return declaration(
                ChangeKind.MODIFIED,
                Optional.of(before),
                Optional.of(after)
        );
    }

    private DeclaredChange declaration(
            ChangeKind kind,
            Optional<ArtifactState> before,
            Optional<ArtifactState> after
    ) {
        return new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                kind,
                CanonicalQaModelVersion.V0_1,
                before,
                after
        );
    }

    private BaseArtifactIndex index(ArtifactState... states) {
        return new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                List.of(states)
        );
    }

    private ArtifactState node(String id, String name) throws Exception {
        return state("""
                {"id":"%s","type":"CHECK","name":"%s"}
                """.formatted(id, name));
    }

    private ArtifactState state(String json) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree(json)
        );
    }

    private ArtifactState relationship(String id) throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"RELATED_TO",
                         "from":"N-1","to":"N-2"}
                        """.formatted(id))
        );
    }

    private ArtifactState scenario(String preconditions) throws Exception {
        return state("""
                {"id":"N-1","type":"TEST_IMPLEMENTATION","name":"Test",
                 "testImplementation":{"code":"T-1","executionType":"MANUAL",
                 "preconditions":%s,"steps":[]}}
                """.formatted(preconditions));
    }

    private ArtifactState validSteps(String action) throws Exception {
        return state("""
                {"id":"N-1","type":"TEST_IMPLEMENTATION","name":"Test",
                 "testImplementation":{"code":"T-1","executionType":"MANUAL",
                 "preconditions":[],"steps":[{"order":1,"action":"%s"}]}}
                """.formatted(action));
    }

    private ArtifactState invalidSteps(CanonicalQaModelVersion version)
            throws Exception {
        return new NodeArtifactState(version, mapper.readTree("""
                {"id":"N-1","type":"TEST_IMPLEMENTATION","name":"Test",
                 "testImplementation":{"code":"T-1","executionType":"MANUAL",
                 "preconditions":[],"steps":[
                 {"order":1,"action":"One"},
                 {"order":1,"action":"Duplicate"}]}}
                """));
    }
}
