package ru.kuznetsov.qaip.explorer.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryService;
import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositorySummaryController {
    private final RepositorySummaryService service;

    public RepositorySummaryController(RepositorySummaryService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @GetMapping("/{repositoryId}/summary")
    public RepositorySummaryView summary(@PathVariable String repositoryId) {
        return service.getSummary(repositoryId);
    }
}
