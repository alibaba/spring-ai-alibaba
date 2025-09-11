package com.alibaba.cloud.ai.manus.coordinator.exception;

import com.alibaba.cloud.ai.manus.coordinator.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * Global exception handler for coordinator tool operations
 */
@RestControllerAdvice
public class CoordinatorToolExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(CoordinatorToolExceptionHandler.class);
    
    /**
     * Handle custom CoordinatorToolException
     */
    @ExceptionHandler(CoordinatorToolException.class)
    public ResponseEntity<ErrorResponse> handleCoordinatorToolException(CoordinatorToolException e) {
        log.error("CoordinatorToolException: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            e.getErrorCode(),
            e.getMessage()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage());
        
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
            
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            message
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            e.getMessage()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
