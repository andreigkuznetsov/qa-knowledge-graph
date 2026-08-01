package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;
import ru.kuznetsov.qaip.runtime.RuntimeDataSourceFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Supplier;

record RuntimeComposition(DataSource dataSource, ProjectRepository repository, ProjectReader reader) {
    RuntimeComposition {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(reader, "reader");
    }

    static RuntimeComposition create() {
        return create(RuntimeDataSourceFactory::create);
    }

    static RuntimeComposition create(Supplier<DataSource> dataSourceFactory) {
        DataSource dataSource = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory").get();
        return new RuntimeComposition(dataSource, new PostgreSqlProjectRepository(dataSource),
                new PostgreSqlProjectReader(dataSource));
    }
}
