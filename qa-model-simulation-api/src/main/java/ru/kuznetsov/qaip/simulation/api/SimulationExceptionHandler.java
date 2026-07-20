package ru.kuznetsov.qaip.simulation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;

@RestControllerAdvice
class SimulationExceptionHandler {
    @ExceptionHandler(SimulationException.class)
    ProblemDetail handleSimulationException(SimulationException exception) {
        HttpStatus status = exception.code()
                == SimulationErrorCode.CANDIDATE_MODEL_VALIDATION_FAILED
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.BAD_REQUEST;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, exception.getMessage());
        problem.setTitle("Simulation failed");
        problem.setProperty("code", exception.code().name());
        if (exception.taskId() != null) {
            problem.setProperty("taskId", exception.taskId());
        }
        if (exception.nodeId() != null) {
            problem.setProperty("nodeId", exception.nodeId());
        }
        if (exception.validation() != null) {
            problem.setProperty("validation", exception.validation());
        }
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableJson(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body must contain valid simulation JSON");
        problem.setTitle("Invalid request body");
        problem.setProperty("code", "INVALID_JSON");
        return problem;
    }
}
