package com.timesheet.offline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A centralized exception handler for the entire application.
 * This class catches specific exceptions and formats them into a consistent
 * JSON error response for the client.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles failed authentication attempts (e.g., bad password/PIN).
     * @param ex The authentication exception.
     * @return A 401 Unauthorized response with a clear error message.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return new ResponseEntity<>(Map.of("error", "Invalid credentials provided"), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles illegal state exceptions, typically used for business logic errors
     * (e.g., an admin trying to use the employee kiosk).
     * @param ex The illegal state exception.
     * @return A 400 Bad Request response.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles illegal argument exceptions, often used for validation errors
     * (e.g., creating a user with an email that already exists).
     * @param ex The illegal argument exception.
     * @return A 400 Bad Request response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors from @Valid annotations on DTOs.
     * @param ex The validation exception.
     * @return A 400 Bad Request response with detailed validation messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));
        return new ResponseEntity<>(Map.of("error", "Validation failed", "details", errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * A catch-all handler for any other unexpected runtime exceptions.
     * @param ex The runtime exception.
     * @return A 500 Internal Server Error response.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGenericRuntimeException(RuntimeException ex) {
        // It's good practice to log the full exception for debugging purposes.
        ex.printStackTrace();
        return new ResponseEntity<>(Map.of("error", "An unexpected server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
