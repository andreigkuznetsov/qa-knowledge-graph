package ru.kuznetsov.qaip.explorer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExplorerApplicationTest {

    @Test
    void application_starts() {
        try (var context = new SpringApplicationBuilder(ExplorerApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {
            assertNotNull(context);
        }
    }

    @Test
    void spring_context_loads() {
    }
}
