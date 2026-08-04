package ru.kuznetsov.qaip.explorer.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.explorer.application.OperationListService;
import ru.kuznetsov.qaip.explorer.view.OperationListView;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class OperationListController {
    private final OperationListService service;

    public OperationListController(OperationListService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @GetMapping("/{repositoryId}/operations")
    public OperationListView operations(@PathVariable String repositoryId) {
        return service.getOperations(repositoryId);
    }
}
