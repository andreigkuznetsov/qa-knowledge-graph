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
import ru.kuznetsov.qaip.explorer.application.GetOperationTestsService;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionNoneQualified;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.view.OperationTestsView;
import ru.kuznetsov.qaip.explorer.view.OperationVerificationStatus;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class OperationTestsController {
    private final GetOperationTestsService service;

    public OperationTestsController(GetOperationTestsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Operation(
            summary = "Get qualified operation tests",
            description = "Returns Explorer presentation models for the qualified tests and checks of one "
                    + "repository operation. Operations without qualified tests return HTTP 200 with an empty list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Qualified tests returned, or no tests qualified and the list is empty",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OperationTestsView.class))),
            @ApiResponse(responseCode = "404",
                    description = "Repository/project or operation was not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Qualified operation-test evidence is ambiguous",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class)))
    })
    @GetMapping(value = "/{repositoryId}/operations/{operationId}/tests",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> operationTests(
            @PathVariable String repositoryId,
            @PathVariable String operationId
    ) {
        var result = service.getOperationTests(repositoryId, operationId);
        return switch (result) {
            case OperationTestsProjectionFound found -> ResponseEntity.ok(found.operationTests());
            case OperationTestsProjectionNoneQualified none -> ResponseEntity.ok(new OperationTestsView(
                    none.repositoryId(), none.operationId(), OperationVerificationStatus.UNVERIFIED,
                    0, 0, List.of()));
            case OperationTestsProjectionProjectNotFound ignored -> error(
                    HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Repository was not found");
            case OperationTestsProjectionOperationNotFound ignored -> error(
                    HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", "Operation was not found");
            case OperationTestsProjectionAmbiguous ignored -> error(
                    HttpStatus.CONFLICT, "AMBIGUOUS_OPERATION_TESTS",
                    "Qualified operation-test evidence is ambiguous");
        };
    }

    private static ResponseEntity<ExplorerErrorResponse> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ExplorerErrorResponse(code, message));
    }
}
