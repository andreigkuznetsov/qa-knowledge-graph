package ru.kuznetsov.qaip.core.application.query.nodedetails;

public sealed interface NodeDetailsQueryResult
        permits NodeDetailsFound, NodeDetailsProjectNotFound, NodeDetailsNodeNotFound { }
