package io.github.joaomnz.bettracker.exception.handler;

import io.github.joaomnz.bettracker.dto.common.ErrorResponse;
import io.github.joaomnz.bettracker.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request){
        Map<String, List<String>> validationErrors = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> validationErrors
                        .computeIfAbsent(error.getField(), _ -> new ArrayList<>())
                        .add(error.getDefaultMessage())
                );

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields.",
                validationErrors,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpServletRequest request){
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid data type.", request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException exception, HttpServletRequest request){
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception, HttpServletRequest request){
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(DataConflictException exception, HttpServletRequest request){
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.warn("Failed login attempt for URI: {}", requestURI);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials.", requestURI);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException exception, HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.warn("Login attempt on locked account for URI: {}", requestURI);
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), requestURI);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.warn("Login attempt on deactivated account for URI: {}", requestURI);
        return buildResponse(HttpStatus.FORBIDDEN, "Your account has been deactivated. Please contact support to reactivate it.", requestURI);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException exception, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("External failure on {} {}", request.getMethod(), requestURI, exception);
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), requestURI);
    }

    @ExceptionHandler(JwtGenerationException.class)
    public ResponseEntity<ErrorResponse> handleJwtGeneration(JwtGenerationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledExceptions(Exception exception, HttpServletRequest request){
        String requestURI = request.getRequestURI();
        log.error("Unexpected failure on {} {}", request.getMethod(), requestURI, exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred.", requestURI);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String requestURI){
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                null,
                requestURI
        );

        return ResponseEntity.status(status).body(response);
    }
}