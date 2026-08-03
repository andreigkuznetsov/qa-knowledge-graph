package ru.kuznetsov.qaip.explorer.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryCommand;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryOutcome;
import ru.kuznetsov.qaip.explorer.application.AnalyzeRepositoryService;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryAnalysisController {
    private final AnalyzeRepositoryService service;

    public RepositoryAnalysisController(AnalyzeRepositoryService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeRepositoryResponse> analyze(@RequestBody AnalyzeRepositoryRequest request) {
        Objects.requireNonNull(request, "request");
        AnalyzeRepositoryOutcome outcome = service.analyze(
                new AnalyzeRepositoryCommand(request.repositoryPath(), request.projectName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AnalyzeRepositoryResponse(
                outcome.repositoryId(),
                outcome.projectIdentity(),
                outcome.analysisStatus().name(),
                outcome.discoveredOperationCount(),
                outcome.warnings()));
    }
}
