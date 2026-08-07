package ru.kuznetsov.qaip.explorer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationTestsOpenApiTest {
    private static final String OPERATION =
            "$.paths['/api/v1/repositories/{repositoryId}/operations/{operationId}/tests'].get";

    @Autowired
    MockMvc mvc;

    @Test
    void documents_success_empty_success_and_typed_http_failures() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(OPERATION + ".summary").value("Get qualified operation tests"))
                .andExpect(jsonPath(OPERATION + ".description").value(
                        org.hamcrest.Matchers.containsString("empty list")))
                .andExpect(jsonPath(OPERATION + ".responses['200'].description").value(
                        org.hamcrest.Matchers.containsString("empty")))
                .andExpect(jsonPath(OPERATION + ".responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/OperationTestsView"))
                .andExpect(jsonPath(OPERATION + ".responses['404'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ExplorerErrorResponse"))
                .andExpect(jsonPath(OPERATION + ".responses['409'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ExplorerErrorResponse"));
    }
}
