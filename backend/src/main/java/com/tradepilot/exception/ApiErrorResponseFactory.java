package com.tradepilot.exception;

import com.tradepilot.observability.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorResponseFactory {
    private ApiErrorResponseFactory() {
    }

    public static Map<String, Object> body(HttpStatus status, String errorCode, String userMessage, HttpServletRequest request) {
        return body(status.value(), status.getReasonPhrase(), errorCode, userMessage, path(request), null);
    }

    public static Map<String, Object> body(HttpStatus status, String errorCode, String userMessage, HttpServletRequest request, Object details) {
        return body(status.value(), status.getReasonPhrase(), errorCode, userMessage, path(request), details);
    }

    public static Map<String, Object> body(int status, String error, String errorCode, String userMessage, String path, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("requestId", requestId());
        body.put("status", status);
        body.put("errorCode", errorCode);
        body.put("userMessage", userMessage);
        body.put("error", userMessage);
        body.put("path", StringUtils.hasText(path) ? path : "unknown");
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }

    private static String requestId() {
        String requestId = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID);
        return StringUtils.hasText(requestId) ? requestId : "unavailable";
    }

    private static String path(HttpServletRequest request) {
        return request == null ? "unknown" : request.getRequestURI();
    }
}
