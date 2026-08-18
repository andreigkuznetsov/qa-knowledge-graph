package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** All source declaration occurrences making one exact claimed Scenario identity. */
public record ScenarioIdentityGroup(
        ClaimedScenarioIdentity claimedScenarioIdentity,
        State state,
        List<Occurrence> occurrences
) {
    public ScenarioIdentityGroup {
        Objects.requireNonNull(claimedScenarioIdentity, "claimedScenarioIdentity");
        Objects.requireNonNull(state, "state");
        occurrences = List.copyOf(Objects.requireNonNull(occurrences, "occurrences"));
        if (occurrences.isEmpty()) throw new IllegalArgumentException("group must contain an occurrence");
        if ((occurrences.size() == 1) != (state == State.UNIQUE)) {
            throw new IllegalArgumentException("UNIQUE requires exactly one occurrence");
        }
        for (Occurrence occurrence : occurrences) {
            if (!claimedScenarioIdentity.equals(occurrence.declaration().claimedScenarioIdentity())) {
                throw new IllegalArgumentException("every occurrence must have the group's exact claimed identity");
            }
        }
    }

    public enum State {
        UNIQUE,
        DUPLICATE_UNCLASSIFIED
    }

    /** Occurrence plus its complete parent-member anti-substitution binding. */
    public record Occurrence(
            ParentCapturedMemberRef parentMemberRef,
            NormalizedScenarioDeclarationOccurrence declaration
    ) {
        public Occurrence {
            Objects.requireNonNull(parentMemberRef, "parentMemberRef");
            Objects.requireNonNull(declaration, "declaration");
            ManifestOccurrenceIdentity manifest = declaration.occurrenceIdentity().manifestOccurrenceIdentity();
            if (!parentMemberRef.parentSourceId().equals(manifest.parentSourceId())
                    || !parentMemberRef.parentSnapshotId().equals(manifest.parentSnapshotId())
                    || !parentMemberRef.parentContentFingerprint().equals(manifest.parentContentFingerprint())
                    || !parentMemberRef.normalizedRepositoryRelativePath()
                    .equals(manifest.normalizedRepositoryRelativePath())) {
                throw new IllegalArgumentException("declaration occurrence does not match parent-member binding");
            }
        }

        public ScenarioDeclarationOccurrenceIdentity occurrenceIdentity() {
            return declaration.occurrenceIdentity();
        }

        public String structuralLocation() {
            return occurrenceIdentity().structuralPath();
        }
    }
}
