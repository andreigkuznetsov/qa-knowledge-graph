package ru.kuznetsov.qaip.coverage.traceability;

import org.junit.jupiter.api.Tag;
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

@Tag("smoke")
@SpringBootTest
@AutoConfigureMockMvc
class TraceabilityChainControllerSmokeTest {

    private static final String ENDPOINT =
            "/api/v1/coverage/traceability/chains";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllCompleteChains()
            throws Exception {

        String request =
                new ClassPathResource(
                        "traceability-complete-branches.json"
                ).getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.built").value(true))
                .andExpect(jsonPath("$.release").value("0.5B"))
                .andExpect(jsonPath("$.totalChains").value(3))
                .andExpect(jsonPath("$.chains.length()").value(3))
                .andExpect(jsonPath("$.chains[0].depth").value(6));
    }
}
