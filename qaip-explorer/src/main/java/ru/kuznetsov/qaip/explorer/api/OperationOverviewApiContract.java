package ru.kuznetsov.qaip.explorer.api;

public final class OperationOverviewApiContract {
    public static final String PATH = "/api/v1/repositories/{repositoryId}/operations/{operationId}/overview";
    public static final int FOUND_STATUS = 200;
    public static final int PROJECT_NOT_FOUND_STATUS = 404;
    public static final int OPERATION_NOT_FOUND_STATUS = 404;
    public static final String PROJECT_NOT_FOUND_CODE = "PROJECT_NOT_FOUND";
    public static final String OPERATION_NOT_FOUND_CODE = "OPERATION_NOT_FOUND";

    private OperationOverviewApiContract() { }
}
