package com.tradepilot.exception;

import com.tradepilot.billing.PlanLimitExceededException;
import com.tradepilot.billing.AiCreditLimitExceededException;
import com.tradepilot.billing.FeatureAccessException;
import com.tradepilot.billing.StripeNotConfiguredException;
import com.tradepilot.config.FeatureDisabledException;
import com.tradepilot.marketdata.MarketDataException;
import com.tradepilot.playbook.exception.PlaybookAiReviewIneligibleException;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRoute(NoResourceFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> details.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponseFactory.body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", request, details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableRequest(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is invalid", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String parameter = ex.getName();
        String message = LocalDate.class.equals(ex.getRequiredType())
                ? "Invalid value for parameter `" + parameter + "`. Expected format: YYYY-MM-DD."
                : "Invalid value for parameter `" + parameter + "`.";
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", safeMessage(ex.getMessage(), "Request could not be processed."), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", ex.getMessage(), request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request);
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handlePlanLimit(PlanLimitExceededException ex, HttpServletRequest request) {
        Map<String, Object> body = ApiErrorResponseFactory.body(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage(), request);
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("upgradeRequired", ex.isUpgradeRequired());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(FeatureAccessException.class)
    public ResponseEntity<Map<String, Object>> handleFeatureAccess(FeatureAccessException ex, HttpServletRequest request) {
        Map<String, Object> body = ApiErrorResponseFactory.body(HttpStatus.FORBIDDEN, "SUBSCRIPTION_REQUIRED", ex.getMessage(), request);
        body.put("error", "Subscription required");
        body.put("message", ex.getMessage());
        body.put("requiredPlan", ex.getRequiredPlan());
        body.put("feature", ex.getFeature().name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleFeatureDisabled(FeatureDisabledException ex, HttpServletRequest request) {
        Map<String, Object> body = ApiErrorResponseFactory.body(HttpStatus.NOT_FOUND, "FEATURE_DISABLED", ex.getMessage(), request);
        body.put("code", "FEATURE_DISABLED");
        body.put("feature", ex.getFeature().name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AiCreditLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleAiCreditLimit(AiCreditLimitExceededException ex, HttpServletRequest request) {
        Map<String, Object> body = ApiErrorResponseFactory.body(HttpStatus.TOO_MANY_REQUESTS, "AI_CREDIT_LIMIT_REACHED", ex.getMessage(), request);
        body.put("error", "AI credit limit reached");
        body.put("message", ex.getMessage());
        body.put("creditsUsed", ex.getCreditsUsed());
        body.put("creditsLimit", ex.getCreditsLimit());
        body.put("creditsRemaining", ex.getCreditsRemaining());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(StripeNotConfiguredException.class)
    public ResponseEntity<Map<String, Object>> handleStripeNotConfigured(StripeNotConfiguredException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "STRIPE_NOT_CONFIGURED", ex.getMessage(), request);
    }

    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<Map<String, Object>> handleMarketData(MarketDataException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, Object>> handleStripe(StripeException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatusCode() != null && ex.getStatusCode() >= 400 && ex.getStatusCode() < 500
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.BAD_GATEWAY;
        log.warn("Stripe request failed. status={}, stripeRequestId={}, path={}", status.value(), ex.getRequestId(), request.getRequestURI());
        return response(status, "STRIPE_REQUEST_FAILED", "Billing provider request failed. Please try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database integrity violation. path={}, reason={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Request conflicts with existing data.", request);
    }

    @ExceptionHandler(PlaybookAiReviewIneligibleException.class)
    public ResponseEntity<Map<String, Object>> handlePlaybookAiIneligible(PlaybookAiReviewIneligibleException ex, HttpServletRequest request) {
        Map<String, Object> body = ApiErrorResponseFactory.body(HttpStatus.CONFLICT, "PLAYBOOK_AI_REVIEW_INELIGIBLE", ex.getMessage(), request);
        body.put("code", "PLAYBOOK_AI_REVIEW_INELIGIBLE");
        body.put("message", ex.getMessage());
        body.put("requiredTradeCount", ex.getRequiredTradeCount());
        body.put("currentEligibleTradeCount", ex.getCurrentEligibleTradeCount());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return response(status, "REQUEST_FAILED", safeMessage(ex.getReason(), "Request failed."), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request exception. path={}", request.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again.", request);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String errorCode, String userMessage, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrorResponseFactory.body(status, errorCode, userMessage, request));
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
