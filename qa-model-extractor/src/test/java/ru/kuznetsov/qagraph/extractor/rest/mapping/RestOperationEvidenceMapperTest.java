package ru.kuznetsov.qagraph.extractor.rest.mapping;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.rest.RestHttpMethod;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;
import ru.kuznetsov.qagraph.model.NodeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestOperationEvidenceMapperTest {
    private static final String REGISTER_IDENTITY =
            "D73C690C07E2E205BAAB91B1FA6C54081AC5695C69AED9E5A57D1192CE269363";
    private static final String CONTROLLER_IDENTITY =
            "F2069E00D5ADD5F1001BB36903FD8DCB9851CFF8D424F61F9D1BFCD6BD72701E";

    private final RestOperationEvidenceMapper mapper = new RestOperationEvidenceMapper();

    @Test
    void mapsPostAuthRegisterToExactContractFields() {
        BusinessOperationProjection projection = mapper.map(registerEvidence("/auth/register"));

        assertEquals("BO-REST-" + REGISTER_IDENTITY, projection.id());
        assertEquals(NodeType.BUSINESS_OPERATION, projection.type());
        assertEquals("POST /auth/register", projection.name());
        assertEquals("Spring MVC REST operation POST /auth/register handled by "
                + "bookShop.controller.AuthController.register.", projection.description());
        assertEquals("REST-" + REGISTER_IDENTITY, projection.operation().code());
        assertEquals("bookShop.controller", projection.operation().domain());
        assertNull(projection.operation().businessOutcome());
    }

    @Test
    void normalizedEquivalentPathsHaveTheSameIdentityAndCode() {
        BusinessOperationProjection canonical = mapper.map(registerEvidence("/auth/register"));
        BusinessOperationProjection equivalent = mapper.map(registerEvidence(" auth//register/ "));

        assertEquals(canonical, equivalent);
        assertEquals(canonical.id(), equivalent.id());
        assertEquals(canonical.operation().code(), equivalent.operation().code());
        assertEquals(canonical.name(), equivalent.name());
    }

    @Test
    void differentHttpMethodsOnTheSamePathRemainDistinct() {
        RestOperationEvidence post = registerEvidence("/auth/register");
        RestOperationEvidence get = new RestOperationEvidence(
                RestHttpMethod.GET, post.endpointPath(), post.controllerClass(), post.controllerMethod(),
                post.javaPackage(), post.sourceLocation());

        BusinessOperationProjection postProjection = mapper.map(post);
        BusinessOperationProjection getProjection = mapper.map(get);

        assertNotEquals(postProjection.id(), getProjection.id());
        assertNotEquals(postProjection.operation().code(), getProjection.operation().code());
    }

    @Test
    void pathVariablesRemainInTheProjection() {
        RestOperationEvidence evidence = new RestOperationEvidence(
                RestHttpMethod.GET,
                "/books/{id}",
                "BookController",
                "getBookById",
                "bookShop.controller",
                new SourceLocation("src/main/java/bookShop/controller/BookController.java", 349, 5));

        BusinessOperationProjection projection = mapper.map(evidence);

        assertEquals("GET /books/{id}", projection.name());
        assertEquals("BO-REST-3D083030885E25513F6D6DF2503E1D7FDF6E290A321B39DE82F863C8A9594CC0",
                projection.id());
    }

    @Test
    void preservesControllerPackageAndSourceLocationInSourceReference() {
        BusinessOperationProjection.SourceReferenceProjection sourceReference =
                mapper.map(registerEvidence("/auth/register")).sourceReferences().getFirst();

        assertEquals("SRC-JAVA-" + CONTROLLER_IDENTITY, sourceReference.sourceId());
        assertEquals(BusinessOperationProjection.LocationType.OTHER, sourceReference.location().type());
        assertEquals("src/main/java/bookShop/controller/AuthController.java:68:5"
                + "#bookShop.controller.AuthController.register", sourceReference.location().value());
        assertEquals("Controller method bookShop.controller.AuthController.register", sourceReference.text());
        assertEquals(1.0, sourceReference.confidence());
        assertEquals(BusinessOperationProjection.EvidenceType.OBSERVED, sourceReference.evidenceType());
    }

    @Test
    void repeatedMappingIsEqualAndDoesNotChangeInputEvidence() {
        RestOperationEvidence evidence = registerEvidence("/auth/register");
        RestOperationEvidence unchanged = new RestOperationEvidence(
                evidence.httpMethod(), evidence.endpointPath(), evidence.controllerClass(),
                evidence.controllerMethod(), evidence.javaPackage(), evidence.sourceLocation());

        BusinessOperationProjection first = mapper.map(evidence);
        BusinessOperationProjection second = mapper.map(evidence);

        assertEquals(first, second);
        assertEquals(unchanged, evidence);
    }

    @Test
    void rejectsNullAndDoesNotInventBusinessOutcome() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
        assertNull(mapper.map(registerEvidence("/auth/register")).operation().businessOutcome());
    }

    private static RestOperationEvidence registerEvidence(String path) {
        return new RestOperationEvidence(
                RestHttpMethod.POST,
                path,
                "AuthController",
                "register",
                "bookShop.controller",
                new SourceLocation("src/main/java/bookShop/controller/AuthController.java", 68, 5));
    }
}
