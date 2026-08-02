package example.api;

import example.support.ExternalResultAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MockMvcReferenceIT {
    private static final String CREATE_PATH = "/api/items";
    private MockMvc mockMvc;

    @Test
    void directGetWithMultipleAssertions() throws Exception {
        mockMvc.perform(get(ApiPath.ITEM.getPath(), "42")
                        .queryParam("view", "full")
                        .header("X-Trace", "trace-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(content().contentType("application/json"))
                .andExpect(header().string("X-Trace", "trace-id"));
    }

    @Test
    void qualifiedPostWithConstantAndPersistenceCheck() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(CREATE_PATH)
                        .content("{\"name\":\"item\"}")
                        .header("Content-Type", "application/json"))
                .andExpect(status().is(201));
        DatabaseAssertions.assertPersisted(repository, "item");
    }

    @Test
    void sameClassHelper() throws Exception {
        ResultActions result = mockMvc.perform(post(CREATE_PATH));
        verifyCreated(result);
    }

    @Test
    void externalStaticHelper() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/items"));
        ExternalResultAssertions.verifyBody(result);
    }

    @Test
    void dynamicEndpointIgnored() throws Exception {
        mockMvc.perform(post(pathFromConfiguration()))
                .andExpect(status().isOk());
    }

    @Test
    void unsupportedMatcherIgnored() throws Exception {
        mockMvc.perform(post(CREATE_PATH))
                .andExpect(customMatcher());
    }

    void nonMockMvcClientIgnored() {
        client.perform(post(CREATE_PATH));
    }

    private void verifyCreated(ResultActions result) throws Exception {
        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.created").value(true));
    }

    private String pathFromConfiguration() {
        return "/dynamic";
    }
}
