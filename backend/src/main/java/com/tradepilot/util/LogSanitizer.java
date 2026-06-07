package com.tradepilot.util;

import org.springframework.util.StringUtils;

public final class LogSanitizer {
    private static final int MAX_ERROR_LENGTH = 240;

    private LogSanitizer() {
    }

    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "unknown";
        }

        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return "***";
        }

        String local = normalized.substring(0, atIndex);
        String domain = normalized.substring(atIndex + 1);
        String visiblePrefix = local.substring(0, 1);
        return visiblePrefix + "***@" + domain;
    }

    public static String summarizeError(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }

        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_ERROR_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_LENGTH) + "...";
    }
}
