package ru.kuznetsov.qagraph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qaip.execution.service.ExecutionPlanner;

@Configuration
public class RegisteredModelExecutionPlanConfiguration {

    @Bean
    ExecutionPlanner executionPlanner() {
        return new ExecutionPlanner();
    }
}
