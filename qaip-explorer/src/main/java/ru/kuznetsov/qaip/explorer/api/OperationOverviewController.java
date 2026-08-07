package ru.kuznetsov.qaip.explorer.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsov.qaip.explorer.application.GetOperationOverviewService;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.view.OperationOverviewView;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class OperationOverviewController {
    private final GetOperationOverviewService service;

    public OperationOverviewController(GetOperationOverviewService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Operation(
            summary = "Get a unified operation overview",
            description = "Returns the Explorer presentation of Runtime-resolved operation identity, "
                    + "implementation, event path, and verification sections.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operation overview returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OperationOverviewView.class))),
            @ApiResponse(responseCode = "404",
                    description = "Repository/project or operation was not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class)))
    })
    @GetMapping(value = "/{repositoryId}/operations/{operationId}/overview",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> operationOverview(
            @PathVariable String repositoryId,
            @PathVariable String operationId
    ) {
        var result = service.getOperationOverview(repositoryId, operationId);
        return switch (result) {
            case OperationOverviewProjectionFound found -> ResponseEntity.ok(found.overview());
            case OperationOverviewProjectionProjectNotFound ignored -> error(
                    HttpStatus.NOT_FOUND,
                    OperationOverviewApiContract.PROJECT_NOT_FOUND_CODE,
                    "Repository was not found");
            case OperationOverviewProjectionOperationNotFound ignored -> error(
                    HttpStatus.NOT_FOUND,
                    OperationOverviewApiContract.OPERATION_NOT_FOUND_CODE,
                    "Operation was not found");
        };
    }

    private static ResponseEntity<ExplorerErrorResponse> error(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status).body(new ExplorerErrorResponse(code, message));
    }
}
