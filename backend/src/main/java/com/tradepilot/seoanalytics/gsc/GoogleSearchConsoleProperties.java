package com.tradepilot.seoanalytics.gsc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.google-search-console")
public class GoogleSearchConsoleProperties {
    private boolean enabled;
    private String property;
    private String serviceAccountJson;
    private String serviceAccountJsonBase64;
    private String canonicalHost = "www.tradepilotcoach.com";
    private int maxRowsPerQuery = 25_000;
    private int pageSize = 25_000;
    private long cacheTtlMinutes = 60;
    private String brandTerms = "tradepilot,trade pilot,tradepilotcoach";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public String getServiceAccountJson() { return serviceAccountJson; }
    public void setServiceAccountJson(String serviceAccountJson) { this.serviceAccountJson = serviceAccountJson; }
    public String getServiceAccountJsonBase64() { return serviceAccountJsonBase64; }
    public void setServiceAccountJsonBase64(String serviceAccountJsonBase64) { this.serviceAccountJsonBase64 = serviceAccountJsonBase64; }
    public String getCanonicalHost() { return canonicalHost; }
    public void setCanonicalHost(String canonicalHost) { this.canonicalHost = canonicalHost; }
    public int getMaxRowsPerQuery() { return maxRowsPerQuery; }
    public void setMaxRowsPerQuery(int maxRowsPerQuery) { this.maxRowsPerQuery = maxRowsPerQuery; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getCacheTtlMinutes() { return cacheTtlMinutes; }
    public void setCacheTtlMinutes(long cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }
    public String getBrandTerms() { return brandTerms; }
    public void setBrandTerms(String brandTerms) { this.brandTerms = brandTerms; }

    public boolean hasCredentials() {
        return StringUtils.hasText(serviceAccountJsonBase64) || StringUtils.hasText(serviceAccountJson);
    }

    public int boundedPageSize() {
        return Math.max(1, Math.min(pageSize, 25_000));
    }

    public int boundedMaxRows() {
        return Math.max(1, Math.min(maxRowsPerQuery, 50_000));
    }

    public Duration cacheTtl() {
        return Duration.ofMinutes(Math.max(1, cacheTtlMinutes));
    }

    public List<String> brandTermList() {
        return Arrays.stream(String.valueOf(brandTerms).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .toList();
    }
}
