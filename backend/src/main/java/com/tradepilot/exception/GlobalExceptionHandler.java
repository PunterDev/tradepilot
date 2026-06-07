package com.tradepilot.exception;

import com.tradepilot.billing.PlanLimitExceededException;
import com.tradepilot.billing.StripeNotConfiguredException;
import com.tradepilot.observability.RequestCorrelationFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex){ return Map.of("error", ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex){ Map<String,String> details = new HashMap<>(); ex.getBindingResult().getFieldErrors().forEach(e -> details.put(e.getField(), e.getDefaultMessage())); return Map.of("error", "Validation failed", "details", details); }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex){ return Map.of("error", ex.getMessage()); }
    @ExceptionHandler(BadCredentialsException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleBadCredentials(BadCredentialsException ex){ return Map.of("error", "Invalid email or password"); }
    @ExceptionHandler(EmailNotVerifiedException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleEmailNotVerified(EmailNotVerifiedException ex){ return Map.of("error", ex.getMessage()); }
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class}) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(Exception ex){ return Map.of("error", "Access denied"); }
    @ExceptionHandler(PlanLimitExceededException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handlePlanLimit(PlanLimitExceededException ex){ return Map.of("code", ex.getCode(), "message", ex.getMessage(), "upgradeRequired", ex.isUpgradeRequired()); }
    @ExceptionHandler(StripeNotConfiguredException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleStripeNotConfigured(StripeNotConfiguredException ex){ return Map.of("error", ex.getMessage()); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() == null ? "Request failed." : ex.getReason()));
    }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception ex) {
        String requestId = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID);
        log.error("Unhandled request exception. requestId={}", requestId, ex);
        return Map.of(
                "error", "Something went wrong. Please try again.",
                "requestId", requestId == null ? "unavailable" : requestId
        );
    }
}
