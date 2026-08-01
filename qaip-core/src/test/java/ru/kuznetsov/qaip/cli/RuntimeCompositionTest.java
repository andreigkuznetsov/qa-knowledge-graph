package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeCompositionTest {
    @Test
    void creates_one_data_source_and_shares_it_between_retained_postgresql_adapters() throws Exception {
        DataSource dataSource = dataSource();
        AtomicInteger dataSourceCalls = new AtomicInteger();
        AtomicInteger bootstrapCalls = new AtomicInteger();
        List<String> order = new ArrayList<>();

        RuntimeComposition composition = RuntimeComposition.create(() -> {
            dataSourceCalls.incrementAndGet();
            order.add("dataSource");
            return dataSource;
        }, value -> {
            bootstrapCalls.incrementAndGet();
            order.add("bootstrap");
            assertSame(dataSource, value);
        });

        PostgreSqlProjectRepository repository = assertInstanceOf(
                PostgreSqlProjectRepository.class, composition.repository());
        PostgreSqlProjectReader reader = assertInstanceOf(PostgreSqlProjectReader.class, composition.reader());
        assertEquals(1, dataSourceCalls.get());
        assertEquals(1, bootstrapCalls.get());
        assertEquals(List.of("dataSource", "bootstrap"), order);
        assertSame(dataSource, composition.dataSource());
        assertSame(dataSource, dataSourceOf(repository));
        assertSame(dataSource, dataSourceOf(reader));
    }

    @Test
    void bootstrap_failure_prevents_composition_from_becoming_available() {
        DataSource dataSource = dataSource();
        IllegalStateException failure = new IllegalStateException("bootstrap failed");
        AtomicInteger bootstrapCalls = new AtomicInteger();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> RuntimeComposition.create(() -> dataSource, value -> {
                    bootstrapCalls.incrementAndGet();
                    assertSame(dataSource, value);
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(1, bootstrapCalls.get());
    }

    private static DataSource dataSourceOf(Object adapter) throws Exception {
        Field field = adapter.getClass().getDeclaredField("dataSource");
        field.setAccessible(true);
        return (DataSource) field.get(adapter);
    }

    private static DataSource dataSource() {
        return (DataSource) Proxy.newProxyInstance(RuntimeCompositionTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) -> null);
    }
}
