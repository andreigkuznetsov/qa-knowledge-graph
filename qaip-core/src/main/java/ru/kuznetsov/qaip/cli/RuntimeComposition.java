package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;
import ru.kuznetsov.qaip.runtime.PostgreSqlSchemaBootstrap;
import ru.kuznetsov.qaip.runtime.RuntimeDataSourceFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

record RuntimeComposition(DataSource dataSource, ProjectRepository repository, ProjectReader reader) {
    RuntimeComposition {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(reader, "reader");
    }

    static RuntimeComposition create() {
        PostgreSqlSchemaBootstrap bootstrap = new PostgreSqlSchemaBootstrap();
        return create(RuntimeDataSourceFactory::create, bootstrap::initialize);
    }

    static RuntimeComposition create(Supplier<DataSource> dataSourceFactory, Consumer<DataSource> bootstrap) {
        DataSource dataSource = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory").get();
        Objects.requireNonNull(bootstrap, "bootstrap").accept(dataSource);
        return new RuntimeComposition(dataSource, new PostgreSqlProjectRepository(dataSource),
                new PostgreSqlProjectReader(dataSource));
    }
}
