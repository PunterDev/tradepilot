package com.tradepilot.e2e.base;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

public final class E2eConfig {
    public static final String DEFAULT_BASE_URL = "http://localhost:5173";

    private E2eConfig() {
    }

    public static String baseUrl() {
        return trimTrailingSlash(System.getProperty("tradepilot.baseUrl", DEFAULT_BASE_URL));
    }

    public static boolean headless() {
        return Boolean.parseBoolean(System.getProperty("headless", "true"));
    }

    public static boolean productionSmokeMode() {
        return Boolean.parseBoolean(System.getProperty("productionSmoke", "false"));
    }

    public static String requiredEnv(String name) {
        return optionalEnv(name)
                .orElseThrow(() -> new IllegalStateException("Missing required environment variable: " + name));
    }

    public static Optional<String> optionalEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static long requiredLongProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: -D" + name + "=<value>");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("System property -D" + name + " must be a number.", ex);
        }
    }

    public static void rejectProductionMutation(String baseUrl, String testName) {
        if (isProductionUrl(baseUrl) && !productionSmokeMode()) {
            throw new IllegalStateException(
                    "Refusing to run mutating Selenium test '" + testName + "' against production base URL: " + baseUrl
                            + ". Use a local/QA URL, or -DproductionSmoke=true for read-only smoke tests.");
        }
    }

    private static boolean isProductionUrl(String rawUrl) {
        try {
            String host = URI.create(rawUrl).getHost();
            if (host == null) return false;
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("tradepilot.app")
                    || normalized.equals("www.tradepilot.app")
                    || normalized.equals("app.tradepilot.app");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String trimTrailingSlash(String value) {
        return value == null ? DEFAULT_BASE_URL : value.replaceAll("/+$", "");
    }
}
