package example.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static example.support.EndpointConstants.IMPORTED_PATH;
import static org.hamcrest.Matchers.equalTo;

import example.support.ApiPath;

public class RegistrationApiIT {
    private static final String LOCAL_PATH = "/auth/local-register";
    private static final String COMPUTED_PATH = "/auth" + "/computed";
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

    @Test
    void localStaticFinalPath() {
        given().when().post(LOCAL_PATH);
    }

    @Test
    void importedStaticFinalPath() {
        given().when().put(IMPORTED_PATH);
    }

    @Test
    void enumFixedPath() {
        given().when().patch(ApiPath.REGISTER.getPath());
    }

    @Test
    void computedPathIgnored() {
        given().when().delete(COMPUTED_PATH);
    }

    @Test
    void missingConstantIgnored() {
        given().when().get(MISSING_PATH);
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
