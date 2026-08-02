package example.support;

import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class AmbiguousAssertions {
    public static void verify(Response response) {
        assertNotNull(response.getBody());
    }

    public static void verify(Object response) {
        assertNotNull(response);
    }
}
