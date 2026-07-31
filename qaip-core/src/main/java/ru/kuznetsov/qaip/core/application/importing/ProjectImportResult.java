package ru.kuznetsov.qaip.core.application.importing;

public sealed interface ProjectImportResult permits ProjectImportSuccess, ProjectImportFailure { }
