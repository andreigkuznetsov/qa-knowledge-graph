package example.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class RegistrationApiIT {
    @Test
    @DisplayName("register user through API")
    void registerSuccessfully() {
        given()
                .body("{}")
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(1));
        DatabaseAssertions.assertPersisted(userRepository, "generated-id");
    }

    @ParameterizedTest
    @DisplayName("register supported roles")
    @ValueSource(strings = {"USER", "ADMIN"})
    void registerRoles(String role) {
        given().body(role).when().post("/auth/register").then().statusCode(200);
    }

    @Test
    void dynamicPathIgnored() {
        given().when().get(dynamicPath);
    }

    void helperMethodIsNotATest() {
        given().when().delete("/auth/register");
    }

    @Test
    void mockMvcStyleIsUnsupported() {
        mockMvc.perform(post("/auth/register"));
    }

    @Test
    void sameNamedClientIsUnsupported() {
        RestAssured.get("/not-rest-assured");
    }

    @org.junit.Test
    void junitFourIsUnsupported() {
        given().when().put("/auth/register");
    }
}
