package com.tradepilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, HttpStatus status, String errorCode, String userMessage, String path) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiErrorResponseFactory.body(status.value(), status.getReasonPhrase(), errorCode, userMessage, path, null));
    }

    @java.lang.SuppressWarnings("all")
    public SecurityErrorResponseWriter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
