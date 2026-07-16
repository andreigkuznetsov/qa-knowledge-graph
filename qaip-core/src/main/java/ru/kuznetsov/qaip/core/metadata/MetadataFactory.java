package ru.kuznetsov.qaip.core.metadata;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class MetadataFactory {

    public static final String PLATFORM_NAME =
            "QA Intelligence Platform";

    private final String release;
    private final String build;
    private final Clock clock;
    private final AnalysisIdGenerator idGenerator;

    public MetadataFactory(
            String release,
            String build,
            AnalysisIdGenerator idGenerator
    ) {
        this(
                release,
                build,
                Clock.systemUTC(),
                idGenerator
        );
    }

    MetadataFactory(
            String release,
            String build,
            Clock clock,
            AnalysisIdGenerator idGenerator
    ) {
        this.release = Objects.requireNonNull(release);
        this.build = Objects.requireNonNull(build);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator =
                Objects.requireNonNull(idGenerator);
    }

    public AnalysisMetadata create(
            String schemaVersion
    ) {
        return new AnalysisMetadata(
                PLATFORM_NAME,
                release,
                build,
                idGenerator.nextId(),
                schemaVersion,
                Instant.now(clock)
        );
    }
}
