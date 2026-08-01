package ru.kuznetsov.qaip.core.application.query.trace;

public sealed interface TraceQueryResult
        permits TraceFound, TraceProjectNotFound, TraceNodeNotFound { }
