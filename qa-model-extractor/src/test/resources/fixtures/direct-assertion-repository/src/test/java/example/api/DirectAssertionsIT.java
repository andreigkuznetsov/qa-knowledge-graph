package example.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.assertj.core.api.AssertionsForClassTypes;
import org.hamcrest.MatcherAssert;

import static io.restassured.RestAssured.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static example.support.ExternalAssertions.verifyExternal;

class DirectAssertionsIT {
    @Test
    void junitAssertions() {
        post("/items");
        assertEquals(1, 1);
        Assertions.assertNotEquals(1, 2);
        assertTrue(true);
        assertFalse(false);
        assertNull(null);
        assertNotNull("value");
        assertThrows(IllegalArgumentException.class, () -> { throw new IllegalArgumentException(); });
        assertDoesNotThrow(() -> "value");
        assertIterableEquals(java.util.List.of(1), java.util.List.of(1));
        assertArrayEquals(new int[]{1}, new int[]{1});
        assertEquals(201, response.getStatusCode());
    }

    @Test
    void assertJAssertions() {
        post("/items");
        org.assertj.core.api.Assertions.assertThat("value")
                .isNotNull()
                .isEqualTo("value");
        assertThat(java.util.List.of("a", "b")).contains("a").hasSize(2);
        assertThat(java.util.Map.of("a", 1)).containsEntry("a", 1).isNotEmpty();
        assertThat(true).isTrue();
        assertThat(false).isFalse();
        assertThat("").isEmpty();
        assertThat("abc").matches("a.*");
        assertThat("abc").satisfies(value -> assertNotNull(value));
        assertThat("value").unsupportedFluentMethod();
        AssertionsForClassTypes.assertThat("foreign entry").isEqualTo("foreign entry");
    }

    @Test
    void hamcrestAssertions() {
        post("/items");
        assertThat("value", is(equalTo("value")));
        MatcherAssert.assertThat("value", not(nullValue()));
        assertThat("value", notNullValue());
        assertThat("value", containsString("alu"));
        assertThat(java.util.List.of("a"), hasItem("a"));
        assertThat(java.util.List.of("a"), hasSize(1));
        assertThat("value", customMatcher());
    }

    @Test
    void helperAssertion() {
        post("/items");
        verifyValue("value");
    }

    @Test
    void externalHelperAssertion() {
        post("/items");
        verifyExternal(false);
    }

    @Test
    void assertionWithoutRestInteraction() {
        assertEquals("value", "value");
    }

    private static void verifyValue(String value) {
        assertThat(value).isEqualTo("value");
        assertNotNull(value);
    }

    void nonJunitMethodIgnored() {
        assertEquals(1, 1);
    }

    private static Object customMatcher() {
        return null;
    }

    private static final Response response = new Response();

    private static final class Response {
        int getStatusCode() {
            return 201;
        }
    }
}
