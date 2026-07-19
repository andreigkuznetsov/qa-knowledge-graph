package ru.kuznetsov.qaip.execution.service;

public class DuplicateRoadmapTaskIdException extends RuntimeException {

    public DuplicateRoadmapTaskIdException(String taskId) {
        super("Roadmap contains duplicate task ID: " + taskId);
    }
}
