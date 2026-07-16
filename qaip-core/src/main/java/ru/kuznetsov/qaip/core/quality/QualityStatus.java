package ru.kuznetsov.qaip.core.quality;

/**
 * Итоговый продуктовый статус качества.
 * Расчёт появится в следующих сборках Sprint 06.
 */
public enum QualityStatus {
    VALID,
    GOOD,
    NEEDS_ATTENTION,
    CRITICAL,
    INVALID
}
