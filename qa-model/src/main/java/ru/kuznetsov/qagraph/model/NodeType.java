package ru.kuznetsov.qagraph.model;

public enum NodeType {
    USER_STORY,
    BUSINESS_OPERATION,
    BUSINESS_RULE,
    SCENARIO,
    TECHNICAL_IMPLEMENTATION,
    TEST_IMPLEMENTATION,
    CHECK;

    public static NodeType from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
