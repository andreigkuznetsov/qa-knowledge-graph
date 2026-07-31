package ru.kuznetsov.qaip.core.application.persistence;

public sealed interface PersistProjectResult permits PersistProjectAccepted, PersistProjectRejected { }
