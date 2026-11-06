package ai.yottaflex.attribution.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String,String> fieldErrors=new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(),error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",
            "Request validation failed.",fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",exception.getMessage(),Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,Object>> handleMalformedJson(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST,"MALFORMED_JSON",
            "Request body is missing or contains invalid JSON.",Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> handleIntegrity(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT,"DATA_INTEGRITY_VIOLATION",
            "The operation violates a data integrity constraint.",Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,Object>> handleConstraint(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST,"CONSTRAINT_VIOLATION",
            "One or more values violate a domain constraint.",Map.of());
    }

    private ResponseEntity<Map<String,Object>> response(HttpStatus status,String code,
            String message,Map<String,?> details) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("timestamp",Instant.now().toString());
        body.put("status",status.value());
        body.put("code",code);
        body.put("message",message);
        body.put("details",details);
        return ResponseEntity.status(status).body(body);
    }
}

