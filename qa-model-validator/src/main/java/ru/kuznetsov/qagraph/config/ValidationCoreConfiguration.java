package ru.kuznetsov.qagraph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;

@Configuration
public class ValidationCoreConfiguration {

    @Bean
    QaModelValidationEngine qaModelValidationEngine() {
        return new QaModelValidationEngine();
    }
}
