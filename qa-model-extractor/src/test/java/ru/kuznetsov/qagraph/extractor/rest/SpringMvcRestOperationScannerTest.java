package ru.kuznetsov.qagraph.extractor.rest;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringMvcRestOperationScannerTest {
    private final SpringMvcRestOperationScanner scanner = new SpringMvcRestOperationScanner();

    @Test
    void combinesControllerAndMethodMappings() throws Exception {
        List<RestOperationEvidence> operations = scanner.scan(fixtureRepository());

        assertTrue(operations.stream().anyMatch(operation ->
                operation.httpMethod() == RestHttpMethod.POST
                        && operation.endpointPath().equals("/api/books/create")
                        && operation.controllerClass().equals("CatalogController")
                        && operation.controllerMethod().equals("create")
                        && operation.javaPackage().equals("example.api")
                        && operation.sourceLocation().repositoryRelativePath()
                        .equals("src/main/java/example/api/CatalogController.java")));
    }

    @Test
    void supportsEmptyMethodPathAndMultipleMethodPaths() throws Exception {
        List<String> endpoints = scanner.scan(fixtureRepository()).stream()
                .map(operation -> operation.httpMethod() + " " + operation.endpointPath())
                .toList();

        assertTrue(endpoints.contains("GET /api/books"));
        assertTrue(endpoints.contains("GET /api/books/search/author"));
        assertTrue(endpoints.contains("GET /api/books/search/title"));
    }

    @Test
    void expandsRequestMappingWithMultipleHttpMethods() throws Exception {
        List<RestOperationEvidence> exportOperations = scanner.scan(fixtureRepository()).stream()
                .filter(operation -> operation.endpointPath().equals("/api/books/export"))
                .toList();

        assertEquals(List.of(RestHttpMethod.GET, RestHttpMethod.OPTIONS, RestHttpMethod.POST),
                exportOperations.stream().map(RestOperationEvidence::httpMethod).toList());
    }

    @Test
    void emptyAndUnsupportedControllersContributeNoOperations() throws Exception {
        List<RestOperationEvidence> operations = scanner.scan(fixtureRepository());

        assertTrue(operations.stream().noneMatch(operation ->
                operation.controllerClass().equals("EmptyController")));
        assertTrue(operations.stream().noneMatch(operation ->
                operation.controllerClass().equals("UnsupportedController")));
        assertTrue(operations.stream().noneMatch(operation ->
                operation.controllerClass().equals("WrongMappingController")));
        assertTrue(operations.stream().noneMatch(operation ->
                operation.controllerMethod().equals("noHttpMethod")));
    }

    @Test
    void detectsEverySupportedFixtureOperationOnceInStableOrder() throws Exception {
        List<RestOperationEvidence> first = scanner.scan(fixtureRepository());
        List<RestOperationEvidence> second = scanner.scan(fixtureRepository());

        assertEquals(first, second);
        assertEquals(10, first.size());
        assertEquals(10, first.stream().distinct().count());
        assertEquals(List.of(
                "GET /api/books",
                "POST /api/books/create",
                "GET /api/books/export",
                "OPTIONS /api/books/export",
                "POST /api/books/export",
                "GET /api/books/search/author",
                "GET /api/books/search/title",
                "DELETE /api/books/{id}",
                "PATCH /api/books/{id}",
                "PUT /api/books/{id}"),
                first.stream().map(operation -> operation.httpMethod() + " " + operation.endpointPath()).toList());
    }

    private static Path fixtureRepository() throws URISyntaxException {
        return Path.of(SpringMvcRestOperationScannerTest.class.getResource(
                "/fixtures/spring-mvc-rest-repository").toURI());
    }
}
