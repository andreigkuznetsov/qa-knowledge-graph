package ru.kuznetsov.qaip.runtime;

import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeDataSourceFactoryTest {
    private static final String SECRET = "do-not-disclose";

    @Test
    void creates_a_new_postgresql_data_source_with_exact_configuration() {
        Map<String, String> environment = validEnvironment();
        environment.put("QAIP_DB_URL", "jdbc:postgresql://DbHost:5544/ExactDb");
        environment.put("QAIP_DB_USER", " exact-user ");
        environment.put("QAIP_DB_PASSWORD", " exact-password ");

        PGSimpleDataSource first = assertInstanceOf(PGSimpleDataSource.class,
                RuntimeDataSourceFactory.create(environment::get));
        PGSimpleDataSource second = assertInstanceOf(PGSimpleDataSource.class,
                RuntimeDataSourceFactory.create(environment::get));

        assertTrue(first.getURL().startsWith("jdbc:postgresql://DbHost:5544/ExactDb?"));
        assertEquals(" exact-user ", first.getUser());
        assertEquals(" exact-password ", first.getPassword());
        assertNotSame(first, second);
    }

    @Test
    void reads_exactly_the_three_required_environment_variables() {
        Map<String, String> environment = validEnvironment();
        Map<String, Integer> reads = new HashMap<>();

        RuntimeDataSourceFactory.create(name -> {
            reads.merge(name, 1, Integer::sum);
            return environment.get(name);
        });

        assertEquals(Map.of(
                "QAIP_DB_URL", 1,
                "QAIP_DB_USER", 1,
                "QAIP_DB_PASSWORD", 1), reads);
    }

    @Test
    void rejects_each_missing_or_blank_required_variable_without_disclosing_password() {
        for (String variable : validEnvironment().keySet()) {
            assertInvalid(variable, null);
            assertInvalid(variable, " \t");
        }
    }

    private static void assertInvalid(String variable, String value) {
        Map<String, String> environment = validEnvironment();
        environment.put(variable, value);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RuntimeDataSourceFactory.create(environment::get));

        assertTrue(failure.getMessage().contains(variable));
        assertTrue(failure.getMessage().contains("missing or blank"));
        assertTrue(!failure.getMessage().contains(SECRET));
    }

    private static Map<String, String> validEnvironment() {
        return new HashMap<>(Map.of(
                "QAIP_DB_URL", "jdbc:postgresql://localhost/qaip",
                "QAIP_DB_USER", "qaip",
                "QAIP_DB_PASSWORD", SECRET));
    }
}
