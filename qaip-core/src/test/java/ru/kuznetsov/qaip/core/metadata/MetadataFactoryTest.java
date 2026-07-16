package ru.kuznetsov.qaip.core.metadata;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataFactoryTest {

    @Test
    void shouldCreateDeterministicMetadata() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-16T12:00:00Z"),
                ZoneOffset.UTC
        );

        AnalysisIdGenerator generator =
                new DefaultAnalysisIdGenerator(
                        clock,
                        new AtomicLong()
                );

        MetadataFactory factory =
                new MetadataFactory(
                        "0.6",
                        "RC1-Build-01",
                        clock,
                        generator
                );

        AnalysisMetadata metadata =
                factory.create("0.1");

        assertEquals(
                "QA Intelligence Platform",
                metadata.platform()
        );
        assertEquals("0.6", metadata.release());
        assertEquals(
                "RC1-Build-01",
                metadata.build()
        );
        assertEquals(
                "QAIP-20260716-000001",
                metadata.analysisId()
        );
        assertEquals(
                Instant.parse("2026-07-16T12:00:00Z"),
                metadata.generatedAt()
        );
    }
}
