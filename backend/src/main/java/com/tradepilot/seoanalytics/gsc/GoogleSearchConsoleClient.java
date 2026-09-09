package com.tradepilot.seoanalytics.gsc;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class GoogleSearchConsoleClient {
    private static final String SEARCH_ANALYTICS_URL = "https://www.googleapis.com/webmasters/v3/sites/%s/searchAnalytics/query";
    private static final String WEBMASTERS_READONLY_SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";

    private final RestTemplate restTemplate;
    private final GoogleSearchConsoleProperties properties;
    private final Supplier<String> accessTokenSupplier;
    private GoogleCredentials credentials;

    @Autowired
    public GoogleSearchConsoleClient(RestTemplate restTemplate, GoogleSearchConsoleProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.accessTokenSupplier = this::accessTokenFromCredentials;
    }

    GoogleSearchConsoleClient(RestTemplate restTemplate, GoogleSearchConsoleProperties properties, Supplier<String> accessTokenSupplier) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.accessTokenSupplier = accessTokenSupplier;
    }

    public List<SearchConsoleMetricRow> query(SearchConsoleQueryType type, LocalDate startDate, LocalDate endDate) {
        List<SearchConsoleMetricRow> rows = new ArrayList<>();
        int pageSize = properties.boundedPageSize();
        int maxRows = properties.boundedMaxRows();
        for (int startRow = 0; startRow < maxRows; startRow += pageSize) {
            int rowLimit = Math.min(pageSize, maxRows - startRow);
            List<SearchConsoleMetricRow> page = queryPage(type, startDate, endDate, rowLimit, startRow);
            rows.addAll(page);
            if (page.size() < rowLimit) break;
        }
        return rows;
    }

    private List<SearchConsoleMetricRow> queryPage(SearchConsoleQueryType type, LocalDate startDate, LocalDate endDate, int rowLimit, int startRow) {
        Map<String, Object> body = Map.of(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "dimensions", type.dimensions(),
                "type", "web",
                "dataState", "final",
                "rowLimit", rowLimit,
                "startRow", startRow
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessTokenSupplier.get());
        String url = SEARCH_ANALYTICS_URL.formatted(UriUtils.encodePathSegment(properties.getProperty(), StandardCharsets.UTF_8));
        try {
            JsonNode response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class).getBody();
            return mapRows(type, response);
        } catch (RestClientException ex) {
            throw new SearchConsoleUnavailableException("Search Console data is temporarily unavailable.", ex);
        }
    }

    private List<SearchConsoleMetricRow> mapRows(SearchConsoleQueryType type, JsonNode response) {
        JsonNode rows = response == null ? null : response.get("rows");
        if (rows == null || !rows.isArray()) return List.of();
        List<SearchConsoleMetricRow> result = new ArrayList<>();
        for (JsonNode row : rows) {
            JsonNode keys = row.get("keys");
            String query = null;
            String page = null;
            String date = null;
            if (keys != null && keys.isArray()) {
                List<String> dimensions = type.dimensions();
                for (int i = 0; i < Math.min(keys.size(), dimensions.size()); i++) {
                    String value = keys.get(i).asText();
                    if ("query".equals(dimensions.get(i))) query = value;
                    if ("page".equals(dimensions.get(i))) page = value;
                    if ("date".equals(dimensions.get(i))) date = value;
                }
            }
            result.add(new SearchConsoleMetricRow(
                    query,
                    page,
                    date,
                    Math.round(row.path("clicks").asDouble(0)),
                    Math.round(row.path("impressions").asDouble(0)),
                    row.path("ctr").asDouble(0),
                    row.path("position").asDouble(0)
            ));
        }
        return result;
    }

    private String accessTokenFromCredentials() {
        try {
            GoogleCredentials current = credentials();
            current.refreshIfExpired();
            if (current.getAccessToken() == null || !StringUtils.hasText(current.getAccessToken().getTokenValue())) {
                current.refresh();
            }
            return current.getAccessToken().getTokenValue();
        } catch (IOException ex) {
            throw new SearchConsoleUnavailableException("Search Console credentials could not be refreshed.", ex);
        }
    }

    private synchronized GoogleCredentials credentials() throws IOException {
        if (credentials != null) return credentials;
        byte[] json = credentialBytes();
        credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json)).createScoped(List.of(WEBMASTERS_READONLY_SCOPE));
        return credentials;
    }

    private byte[] credentialBytes() {
        if (StringUtils.hasText(properties.getServiceAccountJsonBase64())) {
            return Base64.getDecoder().decode(properties.getServiceAccountJsonBase64().trim());
        }
        if (StringUtils.hasText(properties.getServiceAccountJson())) {
            return properties.getServiceAccountJson().getBytes(StandardCharsets.UTF_8);
        }
        throw new SearchConsoleUnavailableException("Search Console credentials are not configured.");
    }
}
