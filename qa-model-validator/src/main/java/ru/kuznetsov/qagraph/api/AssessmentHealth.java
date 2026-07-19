package ru.kuznetsov.qagraph.api;

public enum AssessmentHealth {
    PASS,
    WARNING,
    FAIL;

    public static AssessmentHealth from(
            boolean valid,
            int highFindings,
            int mediumFindings
    ) {
        if (!valid) {
            return FAIL;
        }
        if (highFindings > 0 || mediumFindings > 0) {
            return WARNING;
        }
        return PASS;
    }
}
