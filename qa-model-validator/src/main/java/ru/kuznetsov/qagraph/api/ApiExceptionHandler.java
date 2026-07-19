package ru.kuznetsov.qagraph.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.kuznetsov.qagraph.service.InvalidQaModelException;
import ru.kuznetsov.qagraph.service.QaModelNotFoundException;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidQaModelException.class)
    public ResponseEntity<QaModelValidationResult> handleInvalidQaModel(
            InvalidQaModelException exception
    ) {
        return ResponseEntity.unprocessableEntity()
                .body(exception.validationResult());
    }

    @ExceptionHandler(QaModelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleModelNotFound(
            QaModelNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "MODEL_NOT_FOUND",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_JSON",
                "message", "Тело запроса не является корректным JSON"
        ));
    }
}
