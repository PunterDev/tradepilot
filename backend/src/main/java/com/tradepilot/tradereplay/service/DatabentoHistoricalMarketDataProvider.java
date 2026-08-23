package com.tradepilot.tradereplay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DatabentoHistoricalMarketDataProvider implements HistoricalMarketDataProvider {
    private static final Logger log = LoggerFactory.getLogger(DatabentoHistoricalMarketDataProvider.class);
    static final String TIMESERIES_URL = "https://hist.databento.com/v0/timeseries.get_range";
    static final String COST_URL = "https://hist.databento.com/v0/metadata.get_cost";
    private static final String STYPE_IN = "raw_symbol";
    private static final String STYPE_OUT = "instrument_id";
    private static final String ENCODING = "json";
    private static final String COMPRESSION = "none";
    private static final BigDecimal DATABENTO_PRICE_SCALE = new BigDecimal("1000000000");
    private static final BigDecimal UNDEF_PRICE = new BigDecimal(Long.MAX_VALUE);
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile("(?i)(\"?(?:api[_-]?key|authorization|token|secret)\"?\\s*[:=]\\s*\")([^\"]+)(\")");
    private static final Pattern SENSITIVE_TEXT_FIELD = Pattern.compile("(?i)(api[_-]?key|authorization|token|secret)\\s*[:=]\\s*[^,}\\s]+");
    private final MarketDataProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public DatabentoHistoricalMarketDataProvider(MarketDataProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    DatabentoHistoricalMarketDataProvider(MarketDataProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerName() {
        return "databento";
    }

    @Override
    public HistoricalCandleResult getCandles(MarketDataRequest request) {
        String apiKey = properties.getDatabento() == null ? "" : properties.getDatabento().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return HistoricalCandleResult.failure("PROVIDER_NOT_CONFIGURED", "Databento API key is not configured.");
        }
        if (request.startUtc() == null || request.endUtc() == null || !request.startUtc().isBefore(request.endUtc())) {
            log.warn("Databento replay request rejected before provider call. contract={}, startUtc={}, endUtc={}", request.actualContractSymbol(), request.startUtc(), request.endUtc());
            return HistoricalCandleResult.failure("MARKET_DATA_REQUEST_REJECTED", "Historical market data request is invalid.");
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("dataset", dataset());
        body.add("symbols", request.actualContractSymbol());
        body.add("schema", schema());
        body.add("start", request.startUtc().toString());
        body.add("end", request.endUtc().toString());
        body.add("encoding", ENCODING);
        body.add("compression", COMPRESSION);
        body.add("stype_in", STYPE_IN);
        body.add("stype_out", STYPE_OUT);
        body.add("pretty_ts", "true");
        body.add("map_symbols", "true");
        body.add("limit", "400");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = Base64.getEncoder().encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + auth);
        String correlationId = "replay-" + Math.abs((request.actualContractSymbol() + request.startUtc()).hashCode());
        headers.set("X-Correlation-ID", correlationId);

        try {
            log.debug(
                    "Databento replay request. endpoint={}, contract={}, dataset={}, schema={}, symbols={}, stypeIn={}, stypeOut={}, startUtc={}, endUtc={}, encoding={}, compression={}, costCheckEnabled={}, correlationId={}",
                    TIMESERIES_URL, request.actualContractSymbol(), dataset(), schema(), request.actualContractSymbol(), STYPE_IN, STYPE_OUT,
                    request.startUtc(), request.endUtc(), ENCODING, COMPRESSION, databento().isCostCheckEnabled(), correlationId
            );
            if (databento().isCostCheckEnabled()) {
                BigDecimal cost;
                try {
                    cost = checkCost(body, headers, request, correlationId);
                } catch (HttpClientErrorException ex) {
                    return HistoricalCandleResult.failure(classify4xx(ex), "Historical market data provider rejected the replay request.");
                }
                BigDecimal maxCost = databento().getMaxRequestCostUsd() == null ? new BigDecimal("0.05") : databento().getMaxRequestCostUsd();
                if (cost.compareTo(maxCost) > 0) {
                    log.warn("Databento cost check rejected request. contract={}, correlationId={}, costUsd={}, maxCostUsd={}", request.actualContractSymbol(), correlationId, cost, maxCost);
                    return HistoricalCandleResult.failure("MARKET_DATA_COST_LIMIT_EXCEEDED", "Historical market data request exceeds the configured cost limit.");
                }
            }
            ResponseEntity<String> response = restTemplate.exchange(TIMESERIES_URL, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            List<HistoricalCandle> candles = parseJsonLines(response.getBody());
            return HistoricalCandleResult.success(candles);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            log.warn("Databento auth failed. contract={}, correlationId={}, status={}, providerMessage={}", request.actualContractSymbol(), correlationId, ex.getStatusCode(), providerMessage(ex));
            return HistoricalCandleResult.failure("PROVIDER_AUTH_FAILED", "Historical market data provider authentication failed.");
        } catch (HttpClientErrorException ex) {
            log.warn(
                    "Databento historical data fetch failed. contract={}, correlationId={}, status={}, providerMessage={}, dataset={}, schema={}, symbols={}, stypeIn={}, stypeOut={}, startUtc={}, endUtc={}",
                    request.actualContractSymbol(), correlationId, ex.getStatusCode(), providerMessage(ex), dataset(), schema(),
                    request.actualContractSymbol(), STYPE_IN, STYPE_OUT, request.startUtc(), request.endUtc()
            );
            return HistoricalCandleResult.failure(classify4xx(ex), "Historical market data provider rejected the replay request.");
        } catch (RestClientException ex) {
            log.warn("Databento historical data fetch failed. contract={}, correlationId={}, reason={}", request.actualContractSymbol(), correlationId, ex.getClass().getSimpleName());
            return HistoricalCandleResult.failure("PROVIDER_UNAVAILABLE", "Historical market data provider is temporarily unavailable.");
        }
    }

    private BigDecimal checkCost(MultiValueMap<String, String> baseBody, HttpHeaders headers, MarketDataRequest request, String correlationId) {
        MultiValueMap<String, String> costParams = new LinkedMultiValueMap<>();
        costParams.add("dataset", baseBody.getFirst("dataset"));
        costParams.add("symbols", baseBody.getFirst("symbols"));
        costParams.add("schema", baseBody.getFirst("schema"));
        costParams.add("start", baseBody.getFirst("start"));
        costParams.add("end", baseBody.getFirst("end"));
        costParams.add("stype_in", STYPE_IN);
        costParams.add("limit", baseBody.getFirst("limit"));
        try {
            log.debug(
                    "Databento cost check request. endpoint={}, contract={}, dataset={}, schema={}, symbols={}, stypeIn={}, startUtc={}, endUtc={}, correlationId={}",
                    COST_URL, request.actualContractSymbol(), dataset(), schema(), request.actualContractSymbol(), STYPE_IN, request.startUtc(), request.endUtc(), correlationId
            );
            String url = UriComponentsBuilder.fromHttpUrl(COST_URL)
                    .queryParams(costParams)
                    .toUriString();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return new BigDecimal(response.getBody() == null ? "0" : response.getBody().trim());
        } catch (HttpClientErrorException ex) {
            log.warn(
                    "Databento cost check failed. contract={}, correlationId={}, status={}, providerMessage={}, dataset={}, schema={}, symbols={}, stypeIn={}, startUtc={}, endUtc={}",
                    request.actualContractSymbol(), correlationId, ex.getStatusCode(), providerMessage(ex), dataset(), schema(),
                    request.actualContractSymbol(), STYPE_IN, request.startUtc(), request.endUtc()
            );
            throw ex;
        }
    }

    private MarketDataProperties.Databento databento() {
        return properties.getDatabento() == null ? new MarketDataProperties.Databento() : properties.getDatabento();
    }

    private String classify4xx(HttpClientErrorException ex) {
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            return "MARKET_DATA_REQUEST_REJECTED";
        }
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return "PROVIDER_RATE_LIMITED";
        }
        return "PROVIDER_BAD_REQUEST";
    }

    private String providerMessage(HttpClientErrorException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return ex.getStatusText();
        }
        String message = body.trim();
        try {
            JsonNode node = objectMapper.readTree(message);
            message = firstText(node, "message", "error", "detail", "reason");
            if (message == null || message.isBlank()) {
                message = node.toString();
            }
        } catch (Exception ignored) {
            // Body is still useful as plain text.
        }
        message = SENSITIVE_JSON_FIELD.matcher(message).replaceAll("$1<redacted>$3");
        message = SENSITIVE_TEXT_FIELD.matcher(message).replaceAll("$1=<redacted>");
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.isTextual() ? value.asText() : value.toString();
            }
        }
        return null;
    }

    private String dataset() {
        String configured = properties.getDatabento() == null ? null : properties.getDatabento().getDataset();
        return configured == null || configured.isBlank() ? "GLBX.MDP3" : configured.trim();
    }

    private String schema() {
        String configured = properties.getDatabento() == null ? null : properties.getDatabento().getSchema();
        return configured == null || configured.isBlank() ? "ohlcv-1m" : configured.trim();
    }

    private List<HistoricalCandle> parseJsonLines(String body) {
        if (body == null || body.isBlank()) return List.of();
        List<HistoricalCandle> candles = new ArrayList<>();
        Set<String> instrumentIds = new HashSet<>();
        Set<Instant> timestamps = new HashSet<>();
        String expectedInstrumentId = null;
        int records = 0;
        for (String line : body.split("\\R")) {
            if (line.isBlank()) continue;
            try {
                records++;
                JsonNode node = objectMapper.readTree(line);
                Instant timestamp = timestamp(node, "ts_event", text(node.path("hd"), "ts_event", text(node, "ts_recv", null)));
                String instrumentId = text(node, "instrument_id", text(node.path("hd"), "instrument_id", null));
                if (instrumentId != null) {
                    instrumentIds.add(instrumentId);
                    if (expectedInstrumentId == null) {
                        expectedInstrumentId = instrumentId;
                    }
                }
                if (timestamp != null) {
                    timestamps.add(timestamp);
                }
                BigDecimal open = databentoPrice(node, "open");
                BigDecimal high = databentoPrice(node, "high");
                BigDecimal low = databentoPrice(node, "low");
                BigDecimal close = databentoPrice(node, "close");
                BigDecimal volume = decimal(node, "volume", decimal(node, "volume_1m", BigDecimal.ZERO));
                if (expectedInstrumentId != null && instrumentId != null && !expectedInstrumentId.equals(instrumentId)) {
                    log.warn(
                            "Databento candle skipped for mixed instrument. timestamp={}, expectedInstrumentId={}, instrumentId={}, rawOpen={}, rawHigh={}, rawLow={}, rawClose={}",
                            timestamp, expectedInstrumentId, instrumentId, rawText(node, "open"), rawText(node, "high"), rawText(node, "low"), rawText(node, "close")
                    );
                } else if (timestamp != null && validOhlc(open, high, low, close)) {
                    candles.add(new HistoricalCandle(timestamp, open, high, low, close, volume == null ? BigDecimal.ZERO : volume));
                } else {
                    log.warn(
                            "Databento candle skipped. timestamp={}, instrumentId={}, rawOpen={}, rawHigh={}, rawLow={}, rawClose={}, open={}, high={}, low={}, close={}",
                            timestamp, instrumentId, rawText(node, "open"), rawText(node, "high"), rawText(node, "low"), rawText(node, "close"),
                            open, high, low, close
                    );
                }
            } catch (Exception ignored) {
                // Ignore malformed individual lines; the service will validate minimum data coverage later.
            }
        }
        log.debug(
                "Databento candle parse summary. records={}, accepted={}, uniqueTimestamps={}, uniqueInstrumentIds={}",
                records, candles.size(), timestamps.size(), instrumentIds.size()
        );
        return candles;
    }

    private Instant timestamp(JsonNode node, String field, String fallback) {
        String value = text(node, field, fallback);
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return Instant.ofEpochSecond(0, new BigDecimal(value).longValueExact());
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private BigDecimal databentoPrice(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) return null;
        BigDecimal raw;
        try {
            raw = new BigDecimal(value.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
        if (UNDEF_PRICE.compareTo(raw) == 0) return null;
        if (isDecimalPrice(value)) {
            return raw.stripTrailingZeros();
        }
        try {
            return raw.divide(DATABENTO_PRICE_SCALE).stripTrailingZeros();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private boolean isDecimalPrice(JsonNode value) {
        if (value == null || value.isNull()) return false;
        if (value.isFloatingPointNumber()) return true;
        String text = value.asText();
        return text.contains(".") || text.contains("e") || text.contains("E");
    }

    private boolean validOhlc(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        return positive(open)
                && positive(high)
                && positive(low)
                && positive(close)
                && high.compareTo(open) >= 0
                && high.compareTo(close) >= 0
                && high.compareTo(low) >= 0
                && low.compareTo(open) <= 0
                && low.compareTo(close) <= 0;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String rawText(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return decimal(node, field, null);
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return fallback;
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
