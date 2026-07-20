package ru.kuznetsov.qaip.simulation.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qaip.simulation.ModelSimulationEngine;

@Configuration
class SimulationConfiguration {
    @Bean
    ModelSimulationEngine modelSimulationEngine() {
        return new ModelSimulationEngine();
    }
}
