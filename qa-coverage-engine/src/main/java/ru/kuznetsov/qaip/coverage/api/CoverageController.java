package ru.kuznetsov.qaip.coverage.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.coverage.model.CoverageReport;
import ru.kuznetsov.qaip.coverage.service.CoverageService;

@RestController
@RequestMapping("/api/v1/coverage")
public class CoverageController {

    private final CoverageService coverageService;

    public CoverageController(
            CoverageService coverageService
    ) {
        this.coverageService = coverageService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Рассчитать покрытие QA-модели",
            description = """
                    QAIP 0.4 рассчитывает:
                    Rule Coverage,
                    Scenario Coverage,
                    Check Coverage.
                    """
    )
    public ResponseEntity<CoverageReport> analyze(
            @RequestBody JsonNode qaModel
    ) {
        return ResponseEntity.ok(
                coverageService.analyze(qaModel)
        );
    }

    @PostMapping(
            value = "/rules",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Совместимый endpoint QAIP 0.2",
            description = "Возвращает полный отчёт QAIP 0.4."
    )
    public ResponseEntity<CoverageReport> analyzeRules(
            @RequestBody JsonNode qaModel
    ) {
        return ResponseEntity.ok(
                coverageService.analyze(qaModel)
        );
    }
}
