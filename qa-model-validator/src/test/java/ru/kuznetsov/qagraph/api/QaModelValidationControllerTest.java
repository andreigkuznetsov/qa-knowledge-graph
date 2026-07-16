package ru.kuznetsov.qagraph.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QaModelValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldValidateExampleDocument() throws Exception {
        String request = new ClassPathResource("valid-qa-model.json")
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/v1/qa-model/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.summary.errors").value(0));
    }

    @Test
    void shouldReturnSchemaErrorsForIncompleteDocument() throws Exception {
        mockMvc.perform(post("/api/v1/qa-model/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaVersion\":\"0.1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.summary.errors").isNumber());
    }
}
