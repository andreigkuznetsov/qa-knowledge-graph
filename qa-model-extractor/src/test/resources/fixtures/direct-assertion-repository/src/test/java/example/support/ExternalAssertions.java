package example.support;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class ExternalAssertions {
    private ExternalAssertions() {
    }

    public static void verifyExternal(boolean value) {
        assertFalse(value);
    }
}
