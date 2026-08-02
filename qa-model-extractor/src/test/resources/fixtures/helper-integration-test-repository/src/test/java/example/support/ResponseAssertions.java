package example.support;

import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ResponseAssertions {
    public static void verifyBody(Response response, String expectedValue) {
        assertEquals(expectedValue, response.jsonPath().getString("value"));
    }
}
