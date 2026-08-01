package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.importing.ProjectImporter;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

public interface ImportProjectUseCase {
    ImportProjectUseCaseResult execute(RawProjectJson source);
}
