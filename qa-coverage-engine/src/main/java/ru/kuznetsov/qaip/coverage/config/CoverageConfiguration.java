package ru.kuznetsov.qaip.coverage.config; import org.springframework.context.annotation.*; import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
@Configuration public class CoverageConfiguration { @Bean QaModelValidationEngine qaModelValidationEngine(){return new QaModelValidationEngine();} }
