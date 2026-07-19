package ru.kuznetsov.qagraph.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.kuznetsov.qaip.coverage.analyzer.CheckCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.RuleCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.analyzer.ScenarioCoverageAnalyzer;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

@Configuration
@Import({
        RuleCoverageAnalyzer.class,
        ScenarioCoverageAnalyzer.class,
        CheckCoverageAnalyzer.class,
        CoverageService.class
})
public class RegisteredModelCoverageConfiguration {
}
