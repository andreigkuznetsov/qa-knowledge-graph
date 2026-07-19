package ru.kuznetsov.qaip.execution.service;

public class SelfDependentTaskException extends RuntimeException {

    public SelfDependentTaskException(String taskId) {
        super("Roadmap task depends on itself: " + taskId);
    }
}
