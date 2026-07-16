package ru.kuznetsov.qaip.core.metadata;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultAnalysisIdGenerator
        implements AnalysisIdGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock clock;
    private final AtomicLong sequence;

    public DefaultAnalysisIdGenerator() {
        this(Clock.systemUTC(), new AtomicLong());
    }

    DefaultAnalysisIdGenerator(
            Clock clock,
            AtomicLong sequence
    ) {
        this.clock = Objects.requireNonNull(clock);
        this.sequence = Objects.requireNonNull(sequence);
    }

    @Override
    public String nextId() {
        LocalDate date = LocalDate.now(
                clock.withZone(ZoneOffset.UTC)
        );

        return "QAIP-%s-%06d".formatted(
                DATE_FORMAT.format(date),
                sequence.incrementAndGet()
        );
    }
}
