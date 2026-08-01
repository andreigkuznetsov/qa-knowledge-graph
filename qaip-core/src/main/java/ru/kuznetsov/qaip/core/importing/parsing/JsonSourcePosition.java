package ru.kuznetsov.qaip.core.importing.parsing;

/**
 * Physical parser position. Line and column are one-based. Character offset is
 * zero-based, as reported by Jackson, and is always non-negative when present.
 */
public record JsonSourcePosition(long line, long column, long characterOffset) {
    public JsonSourcePosition {
        if (line < 1 || column < 1 || characterOffset < 0) {
            throw new IllegalArgumentException("Source coordinates are outside their documented ranges");
        }
    }
}
