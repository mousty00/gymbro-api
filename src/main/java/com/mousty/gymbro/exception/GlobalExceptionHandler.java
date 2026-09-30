package com.mousty.gymbro.exception;

import com.mousty.gymbro.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST error mapping. Handlers return the ErrorResponse body directly: fixed statuses use
 * {@link ResponseStatus}, dynamic ones (domain exceptions, framework 4xx) set it on the response.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GymBroException.class)
    public ErrorResponse handleGymBroException(GymBroException ex, HttpServletResponse response) {
        return error(response, ex.getStatus(), ex.getStatus().getReasonPhrase(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
                errors.put(((FieldError) error).getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid input parameters", errors);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFoundException(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Entity Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid username or password", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access Denied", "You don't have permission to access this resource", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ErrorResponse handleResponseStatusException(ResponseStatusException ex, HttpServletResponse response) {
        return error(response, HttpStatus.valueOf(ex.getStatusCode().value()), "Request Error",
                ex.getReason() != null ? ex.getReason() : ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "Database constraint violation";

        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage().toLowerCase();
            if (causeMessage.contains("unique")) {
                message = "A record with this information already exists";
            } else if (causeMessage.contains("foreign key")) {
                message = "Referenced record does not exist";
            } else if (causeMessage.contains("not null")) {
                message = "Required field cannot be empty";
            }
        }
        return error(HttpStatus.CONFLICT, "Data Integrity Violation", message, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Constraint Violation", "Validation failed", errors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Not Found", "No resource found at this path", null);
    }

    // Framework errors (405, 415, missing param, malformed JSON...) keep their 4xx status
    // instead of falling into the 500 catch-all below.
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ErrorResponse handleFrameworkClientError(Exception ex, HttpServletResponse response) {
        HttpStatus status = ex instanceof org.springframework.web.ErrorResponse er
                ? HttpStatus.valueOf(er.getStatusCode().value())
                : HttpStatus.BAD_REQUEST;
        return error(response, status, status.getReasonPhrase(),
                status == HttpStatus.BAD_REQUEST ? "Malformed request" : status.getReasonPhrase());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllUncaughtException(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", null);
    }

    private static ErrorResponse error(HttpServletResponse response, HttpStatus status, String error, String message) {
        response.setStatus(status.value());
        return error(status, error, message, null);
    }

    private static ErrorResponse error(HttpStatus status, String error, String message, Map<String, String> details) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .details(details)
                .build();
    }
}
