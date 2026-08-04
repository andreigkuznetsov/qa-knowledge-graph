package ru.kuznetsov.qaip.explorer.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsService;
import ru.kuznetsov.qaip.explorer.view.OperationDetailsView;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class OperationDetailsController {
    private final OperationDetailsService service;

    public OperationDetailsController(OperationDetailsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @GetMapping("/{repositoryId}/operations/{operationId}")
    public OperationDetailsView details(
            @PathVariable String repositoryId,
            @PathVariable String operationId
    ) {
        return service.getDetails(repositoryId, operationId);
    }
}
