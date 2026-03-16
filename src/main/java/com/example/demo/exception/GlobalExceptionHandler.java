package com.example.demo.exception;

import com.example.demo.util.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse response = build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
        log.warn("Resource not found at [{}]", ExceptionUtils.origin(ex));
        LogUtils.warn(log, "ErrorResponse", response);
        return response;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        ErrorResponse response = build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
        log.warn("Duplicate resource at [{}]", ExceptionUtils.origin(ex));
        LogUtils.warn(log, "ErrorResponse", response);
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse response = build(HttpStatus.BAD_REQUEST, "Validation Failed", message, request);
        log.warn("Validation failed at [{}]", ExceptionUtils.origin(ex));
        LogUtils.warn(log, "ErrorResponse", response);
        return response;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ErrorResponse response = build(HttpStatus.BAD_REQUEST, "Missing Parameter", ex.getMessage(), request);
        log.warn("Missing parameter at [{}]", ExceptionUtils.origin(ex));
        LogUtils.warn(log, "ErrorResponse", response);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse response = build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
        LogUtils.error(log, "Unexpected error at [" + ExceptionUtils.origin(ex) + "]", response, ex);
        return response;
    }

    private ErrorResponse build(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
