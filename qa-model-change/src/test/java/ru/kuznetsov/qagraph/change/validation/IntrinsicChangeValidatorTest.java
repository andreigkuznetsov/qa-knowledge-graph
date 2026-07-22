package ru.kuznetsov.qagraph.change.validation;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.model.ArtifactCategory.NODE;
import static ru.kuznetsov.qagraph.change.model.ChangeKind.ADDED;
import static ru.kuznetsov.qagraph.change.model.ChangeKind.MODIFIED;
import static ru.kuznetsov.qagraph.change.model.ChangeKind.REMOVED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_AFTER_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_BEFORE_STATE_PRESENT;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ARTIFACT_CATEGORY_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ARTIFACT_IDENTITY_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.CROSS_VERSION_CHANGE_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_AFTER_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_BEFORE_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_STATE_UNCHANGED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_AFTER_STATE_PRESENT;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_BEFORE_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.STATE_SEMANTICS_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.UNSUPPORTED_SCHEMA_VERSION;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.STRUCTURALLY_INVALID;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNSUPPORTED;

class IntrinsicChangeValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntrinsicChangeValidator validator =
            new IntrinsicChangeValidator();

    @Test
    void validAddedShouldPass() throws Exception {
        assertValid(change(ADDED, Optional.empty(),
                Optional.of(node("N-1", "After"))));
    }

    @Test
    void addedShouldReportAllPresenceFailuresDeterministically()
            throws Exception {
        assertFailure(change(ADDED,
                        Optional.of(node("N-1", "Before")),
                        Optional.of(node("N-1", "After"))),
                STRUCTURALLY_INVALID, ADDED_BEFORE_STATE_PRESENT);
        assertFailure(change(ADDED, Optional.empty(), Optional.empty()),
                STRUCTURALLY_INVALID, ADDED_AFTER_STATE_MISSING);
        assertFailure(change(ADDED,
                        Optional.of(node("N-1", "Before")),
                        Optional.empty()),
                STRUCTURALLY_INVALID,
                ADDED_BEFORE_STATE_PRESENT,
                ADDED_AFTER_STATE_MISSING);
    }

    @Test
    void validRemovedShouldPass() throws Exception {
        assertValid(change(REMOVED,
                Optional.of(node("N-1", "Before")), Optional.empty()));
    }

    @Test
    void removedShouldReportAllPresenceFailuresDeterministically()
            throws Exception {
        assertFailure(change(REMOVED,
                        Optional.of(node("N-1", "Before")),
                        Optional.of(node("N-1", "After"))),
                STRUCTURALLY_INVALID, REMOVED_AFTER_STATE_PRESENT);
        assertFailure(change(REMOVED, Optional.empty(), Optional.empty()),
                STRUCTURALLY_INVALID, REMOVED_BEFORE_STATE_MISSING);
        assertFailure(change(REMOVED, Optional.empty(),
                        Optional.of(node("N-1", "After"))),
                STRUCTURALLY_INVALID,
                REMOVED_BEFORE_STATE_MISSING,
                REMOVED_AFTER_STATE_PRESENT);
    }

    @Test
    void unequalModifiedShouldPass() throws Exception {
        assertValid(change(MODIFIED,
                Optional.of(node("N-1", "Before")),
                Optional.of(node("N-1", "After"))));
    }

    @Test
    void modifiedMissingStatesShouldFailBeforeSemanticComparison()
            throws Exception {
        assertFailure(change(MODIFIED, Optional.empty(), Optional.empty()),
                STRUCTURALLY_INVALID,
                MODIFIED_BEFORE_STATE_MISSING,
                MODIFIED_AFTER_STATE_MISSING);
        assertFailure(change(MODIFIED, Optional.empty(),
                        Optional.of(node("N-1", "After"))),
                STRUCTURALLY_INVALID, MODIFIED_BEFORE_STATE_MISSING);
        assertFailure(change(MODIFIED,
                        Optional.of(node("N-1", "Before")),
                        Optional.empty()),
                STRUCTURALLY_INVALID, MODIFIED_AFTER_STATE_MISSING);
    }

    @Test
    void semanticallyEqualModifiedShouldFailAsNoOp() throws Exception {
        ArtifactState before = state("""
                {"id":"N-1","type":"CHECK","name":"Same",
                 "metadata":{"a":1,"b":true}}
                """);
        ArtifactState after = state("""
                {"metadata":{"b":true,"a":1},"name":"Same",
                 "type":"CHECK","id":"N-1"}
                """);

        assertFailure(change(MODIFIED, Optional.of(before),
                        Optional.of(after)),
                STRUCTURALLY_INVALID, MODIFIED_STATE_UNCHANGED);
    }

    @Test
    void categoryAndIdentityMismatchShouldBeStructural() throws Exception {
        DeclaredChange categoryMismatch = new DeclaredChange(
                ArtifactCategory.RELATIONSHIP,
                new CanonicalIdentity("N-1"),
                ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.of(node("N-1", "After"))
        );
        assertFailure(categoryMismatch, STRUCTURALLY_INVALID,
                ARTIFACT_CATEGORY_MISMATCH);

        assertFailure(change(ADDED, Optional.empty(),
                        Optional.of(node("OTHER", "After"))),
                STRUCTURALLY_INVALID, ARTIFACT_IDENTITY_MISMATCH);
    }

    @Test
    void modifiedIdentityMutationShouldBeStructural() throws Exception {
        assertFailure(change(MODIFIED,
                        Optional.of(node("N-1", "Before")),
                        Optional.of(node("N-2", "After"))),
                STRUCTURALLY_INVALID, ARTIFACT_IDENTITY_MISMATCH);
    }

    @Test
    void removedAndModifiedShouldEnforceCategoryAndIdentity()
            throws Exception {
        assertFailure(change(REMOVED,
                        Optional.of(relationship("N-1")),
                        Optional.empty()),
                STRUCTURALLY_INVALID, ARTIFACT_CATEGORY_MISMATCH);
        assertFailure(change(REMOVED,
                        Optional.of(node("OTHER", "Before")),
                        Optional.empty()),
                STRUCTURALLY_INVALID, ARTIFACT_IDENTITY_MISMATCH);
        assertFailure(change(MODIFIED,
                        Optional.of(relationship("N-1")),
                        Optional.of(relationship("N-1"))),
                STRUCTURALLY_INVALID,
                ARTIFACT_CATEGORY_MISMATCH,
                ARTIFACT_CATEGORY_MISMATCH);
    }

    @Test
    void unsupportedVersionShouldSuppressStructuralFailures() {
        DeclaredChange declaration = new DeclaredChange(
                NODE,
                new CanonicalIdentity("N-1"),
                ADDED,
                new CanonicalQaModelVersion("9.9"),
                Optional.empty(),
                Optional.empty()
        );

        assertFailure(declaration, UNSUPPORTED,
                UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void crossVersionChangeShouldBeUnsupported() throws Exception {
        ArtifactState before = node("N-1", "Before");
        ArtifactState after = new NodeArtifactState(
                new CanonicalQaModelVersion("9.9"),
                mapper.readTree(
                        "{\"id\":\"N-1\",\"type\":\"CHECK\","
                                + "\"name\":\"After\"}")
        );

        assertFailure(change(MODIFIED, Optional.of(before),
                        Optional.of(after)),
                UNSUPPORTED, CROSS_VERSION_CHANGE_UNSUPPORTED);
    }

    @Test
    void unsupportedSemanticEqualityShouldBeUnsupported() throws Exception {
        ArtifactState before = invalidOrderedSteps("Before");
        ArtifactState after = invalidOrderedSteps("After");

        assertFailure(change(MODIFIED, Optional.of(before),
                        Optional.of(after)),
                UNSUPPORTED, STATE_SEMANTICS_UNSUPPORTED);
    }

    @Test
    void ordinaryIntrinsicValidationShouldNeverProduceUnverifiable()
            throws Exception {
        IntrinsicChangeResult result = validator.validate(change(
                ADDED, Optional.empty(), Optional.of(node("N-1", "After"))));

        assertTrue(!(result instanceof IntrinsicallyInvalidChange failure)
                || failure.classification()
                != ChangeFailureClassification.UNVERIFIABLE);
    }

    private void assertValid(DeclaredChange declaration) {
        assertInstanceOf(IntrinsicallyValidChange.class,
                validator.validate(declaration));
    }

    private void assertFailure(
            DeclaredChange declaration,
            ChangeFailureClassification classification,
            ChangeDiagnosticCode... codes
    ) {
        IntrinsicallyInvalidChange result = assertInstanceOf(
                IntrinsicallyInvalidChange.class,
                validator.validate(declaration)
        );
        assertEquals(classification, result.classification());
        assertEquals(List.of(codes), result.diagnostics().stream()
                .map(ChangeDiagnostic::code)
                .toList());
    }

    private DeclaredChange change(
            ChangeKind kind,
            Optional<ArtifactState> before,
            Optional<ArtifactState> after
    ) {
        return new DeclaredChange(
                NODE,
                new CanonicalIdentity("N-1"),
                kind,
                CanonicalQaModelVersion.V0_1,
                before,
                after
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

    private ArtifactState invalidOrderedSteps(String action) throws Exception {
        return state("""
                {"id":"N-1","type":"TEST_IMPLEMENTATION","name":"Test",
                 "testImplementation":{"code":"T-1","executionType":"MANUAL",
                 "preconditions":[],"steps":[
                 {"order":1,"action":"%s"},
                 {"order":1,"action":"Duplicate"}]}}
                """.formatted(action));
    }
}
