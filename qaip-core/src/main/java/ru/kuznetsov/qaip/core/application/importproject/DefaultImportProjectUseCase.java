package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.importing.ProjectImportFailure;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportResult;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.Objects;

public final class DefaultImportProjectUseCase implements ImportProjectUseCase {
    private static final String PERSISTENCE_FAILURE_MESSAGE = "Project persistence failed.";

    private final ProjectImporter importer;
    private final PersistProject persistence;

    public DefaultImportProjectUseCase(ProjectImporter importer, PersistProject persistence) {
        this.importer = Objects.requireNonNull(importer, "importer");
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    @Override
    public ImportProjectUseCaseResult execute(RawProjectJson source) {
        Objects.requireNonNull(source, "source");
        ProjectImportResult importResult = Objects.requireNonNull(
                importer.importProject(source), "import result");
        if (importResult instanceof ProjectImportFailure failure) {
            return new ImportProjectRejected(failure);
        }

        ProjectImportSuccess success = (ProjectImportSuccess) importResult;
        try {
            var persistenceResult = Objects.requireNonNull(
                    persistence.execute(success.document()), "persistence result");
            if (persistenceResult instanceof PersistProjectAccepted accepted) {
                return new ImportProjectCompleted(success, accepted);
            }
            return new ImportProjectPersistenceRejected((PersistProjectRejected) persistenceResult);
        } catch (ProjectPersistenceException exception) {
            return new ImportProjectPersistenceFailed(PERSISTENCE_FAILURE_MESSAGE);
        }
    }
}
