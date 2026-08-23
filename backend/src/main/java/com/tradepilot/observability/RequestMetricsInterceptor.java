package com.tradepilot.observability;

import com.tradepilot.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RequestMetricsInterceptor implements HandlerInterceptor {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RequestMetricsInterceptor.class);
    private static final String START_ATTRIBUTE = "tradepilot.request.start";
    private final ObservabilityService observabilityService;
    @Value("${app.observability.slow-request-warn-ms:1000}")
    private long slowRequestWarnMs;
    @Value("${app.observability.slow-request-error-ms:5000}")
    private long slowRequestErrorMs;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTRIBUTE);
        if (!(start instanceof Long startMs) || shouldSkip(request)) {
            return;
        }
        String endpoint = endpoint(request);
        long durationMs = System.currentTimeMillis() - startMs;
        Long userId = currentUserId();
        int status = effectiveStatus(response.getStatus(), ex);
        observabilityService.recordRequest(endpoint, request.getMethod(), status, durationMs, userId, MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
        logRequest(request.getMethod(), endpoint, status, durationMs, userId, ex);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || uri.startsWith("/actuator") || uri.startsWith("/h2-console") || uri.startsWith("/error");
    }

    private String endpoint(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return String.valueOf(pattern);
        }
        return request.getRequestURI();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private void logRequest(String method, String endpoint, int status, long durationMs, Long userId, Exception ex) {
        if (ex != null) {
            if (isExpectedRequestException(ex)) {
                log.info("Request rejected. method={}, endpoint={}, status={}, durationMs={}, userId={}, reason={}", method, endpoint, status, durationMs, userId, ex.getClass().getSimpleName());
            } else {
                log.error("Request failed. method={}, endpoint={}, status={}, durationMs={}, userId={}", method, endpoint, status, durationMs, userId, ex);
            }
            return;
        }
        if (durationMs >= slowRequestErrorMs) {
            log.error("Critical slow request. method={}, endpoint={}, status={}, durationMs={}, userId={}", method, endpoint, status, durationMs, userId);
        } else if (durationMs >= slowRequestWarnMs) {
            log.warn("Slow request. method={}, endpoint={}, status={}, durationMs={}, userId={}", method, endpoint, status, durationMs, userId);
        } else if (status >= 400) {
            log.info("Request completed with error status. method={}, endpoint={}, status={}, durationMs={}, userId={}", method, endpoint, status, durationMs, userId);
        }
    }

    private boolean isExpectedRequestException(Exception ex) {
        String exceptionName = ex.getClass().getName();
        return exceptionName.contains("AccessDeniedException") || exceptionName.contains("AuthenticationException") || exceptionName.contains("AuthorizationDeniedException") || exceptionName.contains("ResponseStatusException");
    }

    private int effectiveStatus(int responseStatus, Exception ex) {
        if (ex == null) {
            return responseStatus;
        }
        if (ex instanceof AccessDeniedException) {
            return HttpServletResponse.SC_FORBIDDEN;
        }
        if (ex instanceof AuthenticationException) {
            return HttpServletResponse.SC_UNAUTHORIZED;
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode().value();
        }
        return responseStatus >= 400 ? responseStatus : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    @java.lang.SuppressWarnings("all")
    public RequestMetricsInterceptor(final ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }
}
