package example.api;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ForeignClientIT {
    private ForeignClient mockMvc;

    @Test
    void foreignClientIgnored() {
        mockMvc.perform(post("/api/items"));
    }

    private static final class ForeignClient {
        void perform(Object request) {
        }
    }
}
