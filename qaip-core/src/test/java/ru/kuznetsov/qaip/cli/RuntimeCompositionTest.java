package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class RuntimeCompositionTest {
    @Test
    void creates_one_data_source_and_shares_it_between_retained_postgresql_adapters() throws Exception {
        DataSource dataSource = dataSource();
        AtomicInteger calls = new AtomicInteger();

        RuntimeComposition composition = RuntimeComposition.create(() -> {
            calls.incrementAndGet();
            return dataSource;
        });

        PostgreSqlProjectRepository repository = assertInstanceOf(
                PostgreSqlProjectRepository.class, composition.repository());
        PostgreSqlProjectReader reader = assertInstanceOf(PostgreSqlProjectReader.class, composition.reader());
        assertEquals(1, calls.get());
        assertSame(dataSource, composition.dataSource());
        assertSame(dataSource, dataSourceOf(repository));
        assertSame(dataSource, dataSourceOf(reader));
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
