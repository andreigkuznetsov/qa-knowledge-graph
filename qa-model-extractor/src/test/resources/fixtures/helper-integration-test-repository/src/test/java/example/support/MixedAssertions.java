package example.support;

import example.persistence.UserRepository;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class MixedAssertions {
    public static void verify(
            Response response,
            UserRepository repository,
            int expectedStatus,
            String expectedValue) {
        assertEquals(expectedStatus, response.getStatusCode());
        response.then().body("value", equalTo(expectedValue));
        assertNotNull(repository.findByValue(expectedValue));
    }
}
