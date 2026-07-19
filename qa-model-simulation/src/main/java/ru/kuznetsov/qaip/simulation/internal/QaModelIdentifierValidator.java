package ru.kuznetsov.qaip.simulation.internal;

import java.util.regex.Pattern;

final class QaModelIdentifierValidator {
    private static final int MAX_LENGTH = 120;
    private static final Pattern PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._:-]*$"
    );

    private QaModelIdentifierValidator() { }

    static boolean isValid(String value) {
        return value != null
                && !value.isEmpty()
                && value.length() <= MAX_LENGTH
                && PATTERN.matcher(value).matches();
    }
}
