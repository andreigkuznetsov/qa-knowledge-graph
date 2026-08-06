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
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionIncomplete;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionNotEventDriven;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.GetEventPathService;
import ru.kuznetsov.qaip.explorer.view.EventPathView;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/repositories")
public class EventPathController {
    private final GetEventPathService service;

    public EventPathController(GetEventPathService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Operation(
            summary = "Get an event-driven implementation path",
            description = "Returns the ordered Explorer presentation of the Runtime-resolved event path "
                    + "for one repository operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event path resolved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventPathView.class))),
            @ApiResponse(responseCode = "404",
                    description = "Repository/project or operation was not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Event path is ambiguous",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class))),
            @ApiResponse(responseCode = "422",
                    description = "Operation is not event-driven or its event path is incomplete",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExplorerErrorResponse.class)))
    })
    @GetMapping(value = "/{repositoryId}/operations/{operationId}/event-path",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> eventPath(
            @PathVariable String repositoryId,
            @PathVariable String operationId
    ) {
        var result = service.getEventPath(repositoryId, operationId);
        return switch (result) {
            case EventPathProjectionFound found -> ResponseEntity.ok(found.path());
            case EventPathProjectionProjectNotFound ignored -> error(
                    HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Repository was not found");
            case EventPathProjectionOperationNotFound ignored -> error(
                    HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", "Operation was not found");
            case EventPathProjectionAmbiguous ignored -> error(
                    HttpStatus.CONFLICT, "AMBIGUOUS_EVENT_PATH", "Event path is ambiguous");
            case EventPathProjectionIncomplete ignored -> error(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INCOMPLETE_EVENT_PATH", "Event path is incomplete");
            case EventPathProjectionNotEventDriven ignored -> error(
                    HttpStatus.UNPROCESSABLE_ENTITY, "NOT_EVENT_DRIVEN", "Operation is not event-driven");
        };
    }

    private static ResponseEntity<ExplorerErrorResponse> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ExplorerErrorResponse(code, message));
    }
}
