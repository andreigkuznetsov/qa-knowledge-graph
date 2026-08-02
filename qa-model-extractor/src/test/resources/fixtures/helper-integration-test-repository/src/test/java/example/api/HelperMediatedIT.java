package example.api;

import example.support.AmbiguousAssertions;
import example.support.MixedAssertions;
import example.support.ResponseAssertions;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelperMediatedIT {
    @Test
    void sameClassHelper() {
        Response response = given().post("/same");
        verifyStatus(response, expectedStatus);
    }

    @Test
    void externalStaticHelper() {
        Response response = given().post("/external");
        ResponseAssertions.verifyBody(response, expectedValue);
    }

    @Test
    void helperWithMultipleCategories() {
        Response response = given().post("/mixed");
        MixedAssertions.verify(response, repository, expectedStatus, expectedValue);
    }

    @Test
    void helperChainIgnored() {
        Response response = given().post("/chain");
        firstLevel(response);
    }

    @Test
    void recursiveHelperIgnored() {
        Response response = given().post("/recursive");
        recursive(response);
    }

    @Test
    void ambiguousHelperIgnored() {
        Response response = given().post("/ambiguous");
        AmbiguousAssertions.verify(response);
    }

    private void verifyStatus(Response response, int expectedStatus) {
        assertEquals(expectedStatus, response.getStatusCode());
        unsupportedAssertion(response);
    }

    private void firstLevel(Response response) {
        secondLevel(response);
    }

    private void secondLevel(Response response) {
        assertEquals(200, response.getStatusCode());
    }

    private void recursive(Response response) {
        assertEquals(200, response.getStatusCode());
        recursive(response);
    }
}
