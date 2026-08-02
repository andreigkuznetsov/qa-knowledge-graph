package example.support;

import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public final class ExternalResultAssertions {
    private ExternalResultAssertions() {
    }

    public static void verifyBody(ResultActions result) throws Exception {
        result.andExpect(content().string("created"));
    }
}
