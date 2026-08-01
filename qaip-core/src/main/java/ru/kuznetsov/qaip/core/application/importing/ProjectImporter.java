package ru.kuznetsov.qaip.core.application.importing;

import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

public interface ProjectImporter {
    ProjectImportResult importProject(RawProjectJson source);
}
