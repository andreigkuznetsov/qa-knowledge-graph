package ru.kuznetsov.qaip.explorer.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisErrorCode;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisException;
import ru.kuznetsov.qaip.explorer.application.OperationDetailsException;
import ru.kuznetsov.qaip.explorer.application.OperationListException;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryException;

@RestControllerAdvice
public class ExplorerApiExceptionHandler {

    @ExceptionHandler(OperationDetailsException.class)
    ResponseEntity<ExplorerErrorResponse> operationDetails(OperationDetailsException exception) {
        HttpStatus status = switch (exception.code()) {
            case REPOSITORY_NOT_FOUND, OPERATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INCOMPLETE_IMPLEMENTATION_PATH, AMBIGUOUS_IMPLEMENTATION_PATH ->
                    HttpStatus.UNPROCESSABLE_ENTITY;
            case ANALYSIS_UNAVAILABLE -> HttpStatus.CONFLICT;
            case RUNTIME_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(
                new ExplorerErrorResponse(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler(OperationListException.class)
    ResponseEntity<ExplorerErrorResponse> operationList(OperationListException exception) {
        HttpStatus status = switch (exception.code()) {
            case REPOSITORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ANALYSIS_UNAVAILABLE -> HttpStatus.CONFLICT;
            case RUNTIME_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(
                new ExplorerErrorResponse(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler(RepositorySummaryException.class)
    ResponseEntity<ExplorerErrorResponse> repositorySummary(RepositorySummaryException exception) {
        HttpStatus status = switch (exception.code()) {
            case REPOSITORY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ANALYSIS_UNAVAILABLE -> HttpStatus.CONFLICT;
            case RUNTIME_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(
                new ExplorerErrorResponse(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler(RepositoryAnalysisException.class)
    ResponseEntity<ExplorerErrorResponse> repositoryAnalysis(RepositoryAnalysisException exception) {
        HttpStatus status = switch (exception.code()) {
            case INVALID_REPOSITORY_INPUT -> HttpStatus.BAD_REQUEST;
            case REPOSITORY_ANALYSIS_FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case RUNTIME_IMPORT_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(
                new ExplorerErrorResponse(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ExplorerErrorResponse> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ExplorerErrorResponse(
                RepositoryAnalysisErrorCode.INVALID_REPOSITORY_INPUT.name(), "Invalid request"));
    }
}
