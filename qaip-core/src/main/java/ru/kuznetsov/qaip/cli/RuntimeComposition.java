package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.importing.DefaultProjectImporter;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.application.importproject.DefaultImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.application.persistence.DefaultPersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectRepository;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;
import ru.kuznetsov.qaip.runtime.PostgreSqlSchemaBootstrap;
import ru.kuznetsov.qaip.runtime.RuntimeDataSourceFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

record RuntimeComposition(DataSource dataSource, ProjectRepository repository, ProjectReader reader,
                          ImportCliCommand importCommand, ImportTextRenderer importRenderer) {
    RuntimeComposition {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(importCommand, "importCommand");
        Objects.requireNonNull(importRenderer, "importRenderer");
    }

    static RuntimeComposition create() {
        PostgreSqlSchemaBootstrap bootstrap = new PostgreSqlSchemaBootstrap();
        return create(RuntimeDataSourceFactory::create, bootstrap::initialize);
    }

    static RuntimeComposition create(Supplier<DataSource> dataSourceFactory, Consumer<DataSource> bootstrap) {
        DataSource dataSource = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory").get();
        Objects.requireNonNull(bootstrap, "bootstrap").accept(dataSource);
        ProjectRepository repository = new PostgreSqlProjectRepository(dataSource);
        ProjectReader reader = new PostgreSqlProjectReader(dataSource);
        ProjectImporter importer = new DefaultProjectImporter(
                new JacksonProjectJsonParser(),
                new NetworkntProjectSchemaValidator(),
                new DefaultProjectBinder(),
                new DefaultProjectApplicationValidator());
        PersistProject persistence = new DefaultPersistProject(repository);
        ImportProjectUseCase useCase = new DefaultImportProjectUseCase(importer, persistence);
        ImportResultMapper mapper = new ImportResultMapper();
        ImportCliCommand importCommand = new ImportCliCommand(useCase, mapper);
        ImportTextRenderer importRenderer = new ImportTextRenderer();
        return new RuntimeComposition(dataSource, repository, reader, importCommand, importRenderer);
    }
}
