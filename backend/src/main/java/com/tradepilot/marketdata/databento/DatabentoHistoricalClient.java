package com.tradepilot.marketdata.databento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.marketdata.MarketDataException;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.tradereplay.service.MarketDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Component
public class DatabentoHistoricalClient {
    private static final Logger log = LoggerFactory.getLogger(DatabentoHistoricalClient.class);
    private static final String STYPE_IN = "continuous";
    private static final String STYPE_OUT = "instrument_id";
    private static final Pattern SENSITIVE_VALUE = Pattern.compile("(?i)(api[_-]?key|authorization|token|secret)\\s*[:=]\\s*[^,\\s}]+");

    private final MarketDataProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public DatabentoHistoricalClient(MarketDataProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, restTemplate());
    }

    DatabentoHistoricalClient(MarketDataProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public BigDecimal estimateCost(MarketInstrument instrument, Instant start, Instant end) {
        ensureConfigured();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(endpoint("metadata.get_cost"))
                    .queryParams(form(instrument, start, end))
                    .toUriString();
            log.debug("Databento cost estimate request. instrument={}, continuousSymbol={}, start={}, end={}, dataset={}, schema={}",
                    instrument, instrument.continuousSymbol(), start, end, databento().getDataset(), databento().getSchema());
            String response = restTemplate.exchange(url, HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers()), String.class).getBody();
            try {
                return new BigDecimal(response == null || response.isBlank() ? "0" : response.trim());
            } catch (NumberFormatException ex) {
                log.warn("Databento cost estimate returned an invalid response. instrument={}, continuousSymbol={}, start={}, end={}, response={}",
                        instrument, instrument.continuousSymbol(), start, end, sanitize(response));
                throw new MarketDataException(HttpStatus.BAD_GATEWAY, "MARKET_DATA_PROVIDER_ERROR", "Historical market data provider returned an unexpected response.");
            }
        } catch (HttpStatusCodeException ex) {
            throw providerFailure(ex, "estimate", instrument, start, end);
        } catch (RestClientException ex) {
            log.warn("Databento cost estimate unavailable. instrument={}, continuousSymbol={}, start={}, end={}, reason={}",
                    instrument, instrument.continuousSymbol(), start, end, ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    public void streamOhlcvBars(MarketInstrument instrument, Instant start, Instant end, Consumer<DatabentoOhlcvBar> consumer) {
        ensureConfigured();
        MultiValueMap<String, String> body = form(instrument, start, end);
        body.add("encoding", "json");
        body.add("pretty_px", "true");
        body.add("pretty_ts", "true");
        body.add("stype_out", STYPE_OUT);
        try {
            restTemplate.execute(endpoint("timeseries.get_range"), HttpMethod.POST,
                    restTemplate.httpEntityCallback(new org.springframework.http.HttpEntity<>(body, headers()), String.class),
                    response -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.isBlank()) consumer.accept(parseBar(line));
                            }
                            return null;
                        } catch (IOException ex) {
                            throw new MarketDataException(HttpStatus.BAD_GATEWAY, "DATABENTO_STREAM_FAILED", "Databento data download did not complete.");
                        }
                    });
        } catch (MarketDataException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            throw providerFailure(ex, "download", instrument, start, end);
        } catch (RestClientException ex) {
            log.warn("Databento historical stream unavailable. instrument={}, continuousSymbol={}, start={}, end={}, reason={}",
                    instrument, instrument.continuousSymbol(), start, end, ex.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private DatabentoOhlcvBar parseBar(String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            JsonNode header = node.path("hd");
            Instant tsEvent = Instant.parse(text(header, "ts_event", text(node, "ts_event", null)));
            long instrumentId = number(header, "instrument_id", number(node, "instrument_id", -1L));
            if (instrumentId < 0) throw new IllegalArgumentException("instrument_id is missing");
            return new DatabentoOhlcvBar(
                    tsEvent,
                    instrumentId,
                    decimal(node, "open"),
                    decimal(node, "high"),
                    decimal(node, "low"),
                    decimal(node, "close"),
                    number(node, "volume", 0L)
            );
        } catch (Exception ex) {
            log.warn("Databento OHLCV record could not be parsed. reason={}", ex.getClass().getSimpleName());
            throw new MarketDataException(HttpStatus.BAD_GATEWAY, "MARKET_DATA_PROVIDER_ERROR", "Historical market data provider returned an invalid OHLCV record.");
        }
    }

    private MultiValueMap<String, String> form(MarketInstrument instrument, Instant start, Instant end) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("dataset", databento().getDataset());
        form.add("symbols", instrument.continuousSymbol());
        form.add("schema", databento().getSchema());
        form.add("start", start.toString());
        form.add("end", end.toString());
        form.add("stype_in", STYPE_IN);
        return form;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(databento().getApiKey(), "");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void ensureConfigured() {
        if (databento().getApiKey() == null || databento().getApiKey().isBlank()) {
            throw new MarketDataException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_NOT_CONFIGURED", "Historical market data is not configured.");
        }
    }

    private String endpoint(String method) {
        String base = databento().getHistoricalBaseUrl() == null ? "" : databento().getHistoricalBaseUrl().replaceAll("/+$", "");
        if (base.isBlank()) throw new MarketDataException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_NOT_CONFIGURED", "Historical market data is not configured.");
        return base + "/" + method;
    }

    private MarketDataException providerFailure(HttpStatusCodeException ex, String operation, MarketInstrument instrument, Instant start, Instant end) {
        HttpStatus upstreamStatus = HttpStatus.valueOf(ex.getStatusCode().value());
        String code;
        String message;
        HttpStatus status;
        if (upstreamStatus == HttpStatus.UNAUTHORIZED || upstreamStatus == HttpStatus.FORBIDDEN) {
            code = "MARKET_DATA_PROVIDER_AUTH_FAILED";
            message = "Historical market data provider authentication failed.";
            status = HttpStatus.BAD_GATEWAY;
        } else if (upstreamStatus == HttpStatus.TOO_MANY_REQUESTS || upstreamStatus.is5xxServerError()) {
            code = "MARKET_DATA_PROVIDER_UNAVAILABLE";
            message = "Historical market data provider is temporarily unavailable.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            code = "MARKET_DATA_PROVIDER_REQUEST_FAILED";
            message = "Databento rejected the historical data request.";
            status = HttpStatus.BAD_GATEWAY;
        }
        log.warn("Databento {} failed. instrument={}, continuousSymbol={}, start={}, end={}, status={}, failureCode={}, response={}",
                operation, instrument, instrument.continuousSymbol(), start, end, upstreamStatus.value(), code, sanitize(ex.getResponseBodyAsString()));
        return new MarketDataException(status, code, message);
    }

    private MarketDataException unavailable() {
        return new MarketDataException(HttpStatus.SERVICE_UNAVAILABLE, "MARKET_DATA_PROVIDER_UNAVAILABLE", "Historical market data provider is temporarily unavailable.");
    }

    private MarketDataProperties.Databento databento() {
        return properties.getDatabento();
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) return "<empty>";
        String redacted = SENSITIVE_VALUE.matcher(value).replaceAll("$1=<redacted>");
        String normalized = redacted.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }

    private static RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        return new RestTemplate(factory);
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private static long number(JsonNode node, String field, long fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asLong(fallback);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) throw new IllegalArgumentException(field + " is missing");
        return new BigDecimal(value.asText());
    }
}
