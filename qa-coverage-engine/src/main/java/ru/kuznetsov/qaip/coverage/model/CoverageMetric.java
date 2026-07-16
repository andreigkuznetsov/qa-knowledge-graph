package ru.kuznetsov.qaip.coverage.model;
public record CoverageMetric(String code,String name,int total,int covered,int uncovered,double percentage){}
