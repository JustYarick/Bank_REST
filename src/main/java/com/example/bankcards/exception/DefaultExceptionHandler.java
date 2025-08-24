package com.example.bankcards.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class DefaultExceptionHandler {

    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<ApiError> handleException(NotAllowedException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.FORBIDDEN.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleException(EntityNotFoundException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleException(AccessDeniedException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.FORBIDDEN.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleException(NotFoundException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AlreadyTakenException.class)
    public ResponseEntity<ApiError> handleException(AlreadyTakenException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.CONFLICT.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleException(IllegalArgumentException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleException(IllegalStateException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        e.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        String errorMessage = "Validation failed: " + errors.toString();

        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        errorMessage,
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        "An unexpected error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ApiError(
                        request.getRequestURI(),
                        "An internal server error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        LocalDateTime.now()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
