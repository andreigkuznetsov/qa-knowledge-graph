package ru.kuznetsov.qagraph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qaip.coverage.service.CoverageService;
import ru.kuznetsov.qaip.findings.service.FindingsService;

@Configuration
public class RegisteredModelFindingsConfiguration {

    @Bean
    FindingsService findingsService(CoverageService coverageService) {
        return new FindingsService(coverageService);
    }
}
