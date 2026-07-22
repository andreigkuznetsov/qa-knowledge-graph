package ru.kuznetsov.qagraph.change.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclaredChangeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldPreserveSuppliedValuesAndExplicitAbsence() throws Exception {
        ArtifactState after = node("N-1", "After");
        DeclaredChange declaration = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.of(after)
        );

        assertEquals(ArtifactCategory.NODE, declaration.category());
        assertEquals(new CanonicalIdentity("N-1"), declaration.identity());
        assertEquals(ChangeKind.ADDED, declaration.kind());
        assertEquals(CanonicalQaModelVersion.V0_1,
                declaration.schemaVersion());
        assertTrue(declaration.beforeState().isEmpty());
        assertSame(after, declaration.afterState().orElseThrow());
    }

    @Test
    void shouldAllowAbsentAfterState() {
        DeclaredChange declaration = declaration(
                ChangeKind.REMOVED,
                Optional.empty(),
                Optional.empty()
        );

        assertTrue(declaration.afterState().isEmpty());
    }

    @Test
    void shouldRepresentExpectedSemanticInvalidity() throws Exception {
        ArtifactState state = node("OTHER", "State");

        DeclaredChange declaration = new DeclaredChange(
                ArtifactCategory.RELATIONSHIP,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                new CanonicalQaModelVersion("9.9"),
                Optional.of(state),
                Optional.empty()
        );

        assertEquals(ChangeKind.ADDED, declaration.kind());
        assertTrue(declaration.beforeState().isPresent());
        assertTrue(declaration.afterState().isEmpty());
    }

    @Test
    void shouldRejectNullRequiredContainers() {
        assertThrows(NullPointerException.class, () -> new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                null,
                Optional.empty()
        ));
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

    private ArtifactState node(String id, String name) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"CHECK","name":"%s"}
                        """.formatted(id, name))
        );
    }
}
