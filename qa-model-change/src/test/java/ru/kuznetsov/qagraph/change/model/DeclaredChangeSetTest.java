package ru.kuznetsov.qagraph.change.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeclaredChangeSetTest {

    @Test
    void shouldCopyInputExposeUnmodifiableListAndPreserveOrder() {
        DeclaredChange first = declaration("N-1");
        DeclaredChange second = declaration("N-2");
        List<DeclaredChange> source = new ArrayList<>(List.of(first, second));

        DeclaredChangeSet changeSet = new DeclaredChangeSet(source);
        source.clear();

        assertEquals(List.of(first, second), changeSet.changes());
        assertThrows(UnsupportedOperationException.class,
                () -> changeSet.changes().add(first));
    }

    @Test
    void shouldRejectNullEmptyAndNullMembers() {
        assertThrows(NullPointerException.class,
                () -> new DeclaredChangeSet(null));
        assertThrows(IllegalArgumentException.class,
                () -> new DeclaredChangeSet(List.of()));
        List<DeclaredChange> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new DeclaredChangeSet(withNull));
    }

    private DeclaredChange declaration(String id) {
        return new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity(id),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.empty()
        );
    }
}
