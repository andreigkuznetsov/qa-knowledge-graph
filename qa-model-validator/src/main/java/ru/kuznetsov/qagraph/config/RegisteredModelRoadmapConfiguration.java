package ru.kuznetsov.qagraph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qaip.roadmap.service.RoadmapService;

@Configuration
public class RegisteredModelRoadmapConfiguration {

    @Bean
    RoadmapService roadmapService() {
        return new RoadmapService();
    }
}
