package com.tradepilot.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String LEGACY_REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 80;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestId(request);
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(LEGACY_REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String requestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(incoming)) {
            incoming = request.getHeader(LEGACY_REQUEST_ID_HEADER);
        }
        if (StringUtils.hasText(incoming)) {
            String trimmed = incoming.trim();
            if (isSafeRequestId(trimmed)) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }

    private boolean isSafeRequestId(String value) {
        if (value.length() > MAX_REQUEST_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean safe = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
            if (!safe) {
                return false;
            }
        }
        return true;
    }
}
