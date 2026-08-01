package ru.kuznetsov.qaip.cli;

enum CliExitCode {
    SUCCESS(0),
    INVALID_USAGE(2),
    PROJECT_NOT_FOUND(3),
    APPLICATION_FAILURE(4);

    private final int value;

    CliExitCode(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }
}
