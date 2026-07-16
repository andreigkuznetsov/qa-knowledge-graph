package ru.kuznetsov.qaip.coverage.traceability.analysis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("smoke") @SpringBootTest @AutoConfigureMockMvc
class TraceabilityCoverageControllerSmokeTest {
    @Autowired private MockMvc mockMvc;
    @Test
    void mixedGraphShouldReturnAllStatuses() throws Exception {
        String request=new ClassPathResource("traceability-mixed-breaks.json").getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(post("/api/v1/coverage/traceability").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analyzed").value(true))
                .andExpect(jsonPath("$.release").value("0.5B"))
                .andExpect(jsonPath("$.summary.totalChains").value(6))
                .andExpect(jsonPath("$.summary.fullyTraceableChains").value(1))
                .andExpect(jsonPath("$.summary.coveragePercentage").value(16.67))
                .andExpect(jsonPath("$.chains[*].status",hasItem("FULLY_TRACEABLE")))
                .andExpect(jsonPath("$.chains[*].status",hasItem("BROKEN_AT_OPERATION")))
                .andExpect(jsonPath("$.chains[*].status",hasItem("BROKEN_AT_CHECK")))
                .andExpect(jsonPath("$.problems.length()").value(5));
    }
}
