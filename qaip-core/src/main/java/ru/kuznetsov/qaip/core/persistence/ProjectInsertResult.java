package ru.kuznetsov.qaip.core.persistence;

/** Result of one atomic {@link ProjectRepository#insertIfAbsent} invocation. */
public sealed interface ProjectInsertResult permits ProjectInserted, ProjectAlreadyExists { }
