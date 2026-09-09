package com.tradepilot.seoanalytics.gsc;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Component
public class SearchConsolePageNormalizer {
    private final GoogleSearchConsoleProperties properties;

    public SearchConsolePageNormalizer(GoogleSearchConsoleProperties properties) {
        this.properties = properties;
    }

    public Optional<String> normalize(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        String trimmed = value.trim();
        if (trimmed.startsWith("/")) {
            return Optional.of(normalizePath(trimmed));
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (!isTradePilotHost(host)) return Optional.empty();
            return Optional.of(normalizePath(StringUtils.hasText(uri.getPath()) ? uri.getPath() : "/"));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private boolean isTradePilotHost(String host) {
        if (!StringUtils.hasText(host)) return false;
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        String canonical = StringUtils.hasText(properties.getCanonicalHost())
                ? properties.getCanonicalHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "")
                : "www.tradepilotcoach.com";
        return normalizedHost.equals(canonical) || normalizedHost.equals("www." + canonical);
    }

    private String normalizePath(String path) {
        String normalized = path.split("[?#]", 2)[0];
        if (!StringUtils.hasText(normalized)) return "/";
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
