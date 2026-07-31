package ru.kuznetsov.qaip.core.persistence;

public sealed interface ProjectInsertResult permits ProjectInserted, ProjectAlreadyExists { }
