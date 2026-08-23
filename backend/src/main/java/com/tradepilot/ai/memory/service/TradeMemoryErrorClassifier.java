package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.entity.TradeMemoryErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.Locale;

@Component
public class TradeMemoryErrorClassifier {
    public TradeMemoryErrorCode classify(Throwable error) {
        Throwable root = root(error);
        if (root instanceof SocketTimeoutException || error instanceof ResourceAccessException) {
            return TradeMemoryErrorCode.TIMEOUT;
        }
        if (error instanceof HttpClientErrorException.Unauthorized || error instanceof HttpClientErrorException.Forbidden) {
            return TradeMemoryErrorCode.EMBEDDING_AUTHENTICATION_FAILED;
        }
        if (error instanceof HttpClientErrorException.TooManyRequests) {
            return TradeMemoryErrorCode.RATE_LIMITED;
        }
        if (error instanceof HttpClientErrorException http && http.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return TradeMemoryErrorCode.RATE_LIMITED;
        }
        String message = sanitize(error == null ? null : error.getMessage()).toUpperCase(Locale.ROOT);
        if (message.contains("DIMENSION") || message.contains("VECTOR SIZE")) {
            return TradeMemoryErrorCode.EMBEDDING_DIMENSION_MISMATCH;
        }
        if (message.contains("COLLECTION") || message.contains("DISTANCE")) {
            return TradeMemoryErrorCode.QDRANT_CONFIGURATION_INVALID;
        }
        if (message.contains("QDRANT")) {
            return TradeMemoryErrorCode.QDRANT_UNAVAILABLE;
        }
        if (message.contains("OPENAI") || message.contains("EMBEDDING")) {
            return TradeMemoryErrorCode.EMBEDDING_PROVIDER_UNAVAILABLE;
        }
        return TradeMemoryErrorCode.UNKNOWN;
    }

    public boolean retryable(TradeMemoryErrorCode code) {
        return code == TradeMemoryErrorCode.QDRANT_UNAVAILABLE
                || code == TradeMemoryErrorCode.EMBEDDING_PROVIDER_UNAVAILABLE
                || code == TradeMemoryErrorCode.TIMEOUT
                || code == TradeMemoryErrorCode.RATE_LIMITED
                || code == TradeMemoryErrorCode.UNKNOWN;
    }

    public String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }

    private Throwable root(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
