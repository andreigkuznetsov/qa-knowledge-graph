package ru.kuznetsov.qaip.runtime;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public final class RuntimeDataSourceFactory {
    private RuntimeDataSourceFactory() { }

    public static DataSource create() {
        return create(System::getenv);
    }

    static DataSource create(Function<String, String> environment) {
        String url = required(environment, "QAIP_DB_URL");
        String user = required(environment, "QAIP_DB_USER");
        String password = required(environment, "QAIP_DB_PASSWORD");

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static String required(Function<String, String> environment, String variable) {
        String value = environment.apply(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable " + variable + " is missing or blank");
        }
        return value;
    }
}
