package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, exact-key view of Base Model artifact evidence.
 */
public final class BaseArtifactIndex {

    private final CanonicalQaModelVersion schemaVersion;
    private final List<ArtifactState> artifacts;
    private final Map<Key, List<ArtifactState>> byKey;
    private final List<BaseArtifactDuplicate> duplicates;
    private final List<ArtifactState> unsupportedVersionArtifacts;
    private final List<ArtifactState> incompatibleVersionArtifacts;

    public BaseArtifactIndex(
            CanonicalQaModelVersion schemaVersion,
            Collection<? extends ArtifactState> artifacts
    ) {
        this.schemaVersion = Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
        Objects.requireNonNull(artifacts, "artifacts must not be null");
        if (artifacts.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "artifacts must not contain null members"
            );
        }
        this.artifacts = List.copyOf(artifacts);

        Map<Key, List<ArtifactState>> mutable = new LinkedHashMap<>();
        for (ArtifactState artifact : this.artifacts) {
            Key key = new Key(artifact.category(), artifact.identity());
            mutable.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(artifact);
        }
        Map<Key, List<ArtifactState>> copied = new LinkedHashMap<>();
        mutable.forEach((key, states) ->
                copied.put(key, List.copyOf(states)));
        this.byKey = Map.copyOf(copied);
        this.duplicates = copied.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new BaseArtifactDuplicate(
                        entry.getKey().category(),
                        entry.getKey().identity(),
                        entry.getValue().size()
                ))
                .sorted(BaseArtifactDuplicate.ORDER)
                .toList();
        Comparator<ArtifactState> artifactOrder = Comparator
                .comparingInt((ArtifactState value) ->
                        categoryRank(value.category()))
                .thenComparing(value -> value.identity().value());
        this.unsupportedVersionArtifacts = this.artifacts.stream()
                .filter(value -> !value.schemaVersion().isSupported())
                .sorted(artifactOrder)
                .toList();
        this.incompatibleVersionArtifacts = this.artifacts.stream()
                .filter(value -> !value.schemaVersion().equals(schemaVersion))
                .sorted(artifactOrder)
                .toList();
    }

    public CanonicalQaModelVersion schemaVersion() {
        return schemaVersion;
    }

    public List<ArtifactState> artifacts() {
        return artifacts;
    }

    public List<BaseArtifactDuplicate> duplicates() {
        return duplicates;
    }

    public List<ArtifactState> unsupportedVersionArtifacts() {
        return unsupportedVersionArtifacts;
    }

    public List<ArtifactState> incompatibleVersionArtifacts() {
        return incompatibleVersionArtifacts;
    }

    public BaseArtifactLookup lookup(
            ArtifactCategory category,
            CanonicalIdentity identity
    ) {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        List<ArtifactState> matches = byKey.get(new Key(category, identity));
        if (matches == null) {
            return new BaseArtifactMissing();
        }
        if (matches.size() == 1) {
            return new BaseArtifactFound(matches.getFirst());
        }
        return new BaseArtifactAmbiguous(matches);
    }

    private record Key(
            ArtifactCategory category,
            CanonicalIdentity identity
    ) {
    }

    private static int categoryRank(ArtifactCategory category) {
        return switch (category) {
            case NODE -> 0;
            case RELATIONSHIP -> 1;
        };
    }
}
