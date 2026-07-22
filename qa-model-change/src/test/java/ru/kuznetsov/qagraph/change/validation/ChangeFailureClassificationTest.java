package ru.kuznetsov.qagraph.change.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ChangeFailureClassificationTest {

    @Test
    void shouldExposeExactlyTheFrozenVocabularyAndPrecedence() {
        assertArrayEquals(
                new String[]{
                        "UNSUPPORTED",
                        "STRUCTURALLY_INVALID",
                        "AMBIGUOUS",
                        "UNVERIFIABLE"
                },
                Arrays.stream(ChangeFailureClassification.values())
                        .sorted((left, right) -> Integer.compare(
                                left.precedence(), right.precedence()))
                        .map(Enum::name)
                        .toArray(String[]::new)
        );
    }
}
