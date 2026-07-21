package ru.kuznetsov.qagraph.change.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactCategoryTest {

    @Test
    void shouldExposeOnlySupportedArtifactCategories() {
        assertEquals(
                List.of("NODE", "RELATIONSHIP"),
                Arrays.stream(ArtifactCategory.values())
                        .map(Enum::name)
                        .toList()
        );
    }
}
