package ru.kuznetsov.qagraph.change.model;

import java.util.List;
import java.util.Objects;

/**
 * Input-order-preserving aggregate of untrusted change declarations.
 */
public record DeclaredChangeSet(List<DeclaredChange> changes) {

    public DeclaredChangeSet {
        Objects.requireNonNull(changes, "changes must not be null");
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("changes must not be empty");
        }
        if (changes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "changes must not contain null members"
            );
        }
        changes = List.copyOf(changes);
    }
}
