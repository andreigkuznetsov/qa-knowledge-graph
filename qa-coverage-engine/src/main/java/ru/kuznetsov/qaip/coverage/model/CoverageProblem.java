package ru.kuznetsov.qaip.coverage.model;
public record CoverageProblem(CoverageProblemType type,CoverageSeverity severity,String objectId,String objectType,String objectName,String message,String explanation,String path){}
