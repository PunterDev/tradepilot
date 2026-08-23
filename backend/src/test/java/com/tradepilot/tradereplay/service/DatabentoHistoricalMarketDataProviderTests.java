package com.tradepilot.tradereplay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class DatabentoHistoricalMarketDataProviderTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsExpectedCostCheckAndTimeseriesParameters() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(true), objectMapper, restTemplate);

        server.expect(once(), requestTo(allOf(
                        startsWith(DatabentoHistoricalMarketDataProvider.COST_URL),
                        not(containsString("stype_out"))
                )))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", containsString("Basic ")))
                .andExpect(queryParam("dataset", "GLBX.MDP3"))
                .andExpect(queryParam("schema", "ohlcv-1m"))
                .andExpect(queryParam("symbols", "MNQM6"))
                .andExpect(queryParam("stype_in", "raw_symbol"))
                .andExpect(queryParam("start", "2026-06-04T14:15:00Z"))
                .andExpect(queryParam("end", "2026-06-04T15:00:00Z"))
                .andRespond(withSuccess("0.001", MediaType.TEXT_PLAIN));

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("dataset=GLBX.MDP3"),
                        containsString("schema=ohlcv-1m"),
                        containsString("symbols=MNQM6"),
                        containsString("stype_in=raw_symbol"),
                        containsString("stype_out=instrument_id"),
                        containsString("encoding=json"),
                        containsString("compression=none"),
                        containsString("start=2026-06-04T14%3A15%3A00Z"),
                        containsString("end=2026-06-04T15%3A00%3A00Z")
                )))
                .andExpect(content().string(not(containsString("pretty_px"))))
                .andExpect(content().string(not(containsString("stype_out=raw_symbol"))))
                .andRespond(withSuccess("""
                        {"ts_event":"2026-06-04T14:15:00Z","open":"19000.00","high":"19010.00","low":"18995.00","close":"19005.00","volume":"12","instrument_id":123456}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-04T14:15:00Z"),
                Instant.parse("2026-06-04T15:00:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).hasSize(1);
        server.verify();
    }

    @Test
    void normalizesDatabentoFixedPointMnqOhlcvPrices() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-04T14:15:00Z","instrument_id":123456},"open":"21450000000000","high":"21480500000000","low":"21435500000000","close":"21462500000000","volume":"12"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-04T14:15:00Z"),
                Instant.parse("2026-06-04T15:00:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).hasSize(1);
        assertThat(result.candles().get(0).open()).isEqualByComparingTo(new java.math.BigDecimal("21450"));
        assertThat(result.candles().get(0).high()).isEqualByComparingTo(new java.math.BigDecimal("21480.5"));
        assertThat(result.candles().get(0).low()).isEqualByComparingTo(new java.math.BigDecimal("21435.5"));
        assertThat(result.candles().get(0).close()).isEqualByComparingTo(new java.math.BigDecimal("21462.5"));
        server.verify();
    }

    @Test
    void normalizesDatabentoFixedPointMesOhlcvPrices() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":987654},"open":5315000000000,"high":5318000000000,"low":5314250000000,"close":5316750000000,"volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).hasSize(1);
        assertThat(result.candles().get(0).open()).isEqualByComparingTo(new java.math.BigDecimal("5315.00"));
        assertThat(result.candles().get(0).high()).isEqualByComparingTo(new java.math.BigDecimal("5318.00"));
        assertThat(result.candles().get(0).low()).isEqualByComparingTo(new java.math.BigDecimal("5314.25"));
        assertThat(result.candles().get(0).close()).isEqualByComparingTo(new java.math.BigDecimal("5316.75"));
        server.verify();
    }

    @Test
    void skipsDatabentoUndefPriceCandles(CapturedOutput output) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":987654},"open":5315000000000,"high":9223372036854775807,"low":5314250000000,"close":5316750000000,"volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).isEmpty();
        assertThat(output).contains("Databento candle skipped");
        assertThat(output).contains("rawHigh=9223372036854775807");
        server.verify();
    }

    @Test
    void skipsDatabentoNullPriceCandles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":987654},"open":5315000000000,"high":5318000000000,"low":null,"close":5316750000000,"volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).isEmpty();
        server.verify();
    }

    @Test
    void skipsStructurallyInvalidDatabentoOhlcCandles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":987654},"open":5315000000000,"high":5314000000000,"low":5314250000000,"close":5316750000000,"volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).isEmpty();
        server.verify();
    }

    @Test
    void acceptsExplicitDecimalDatabentoPriceFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"ts_event":"2026-06-10T19:19:00Z","instrument_id":987654,"open":"5315.75","high":"5341.25","low":"5314.25","close":"5340.75","volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).hasSize(1);
        assertThat(result.candles().get(0).high()).isEqualByComparingTo(new java.math.BigDecimal("5341.25"));
        server.verify();
    }

    @Test
    void skipsUnexpectedMixedInstrumentCandles() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":111},"open":5315000000000,"high":5318000000000,"low":5314250000000,"close":5316750000000,"volume":"18"}
                        {"hd":{"ts_event":"2026-06-10T19:19:00Z","instrument_id":222},"open":5315000000000,"high":7288000000000,"low":5314250000000,"close":5316750000000,"volume":"18"}
                        """, MediaType.APPLICATION_JSON));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-10T19:19:00Z"),
                Instant.parse("2026-06-10T19:20:00Z")
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.candles()).hasSize(1);
        assertThat(result.candles().get(0).high()).isEqualByComparingTo(new java.math.BigDecimal("5318.00"));
        server.verify();
    }

    @Test
    void rejectsInvalidUtcWindowBeforeCallingDatabento() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(true), objectMapper, restTemplate);

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-04T15:00:00Z"),
                Instant.parse("2026-06-04T15:00:00Z")
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("MARKET_DATA_REQUEST_REJECTED");
        server.verify();
    }

    @Test
    void capturesDatabento422BodySafelyAndClassifiesAsRejected(CapturedOutput output) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(false), objectMapper, restTemplate);

        server.expect(once(), requestTo(DatabentoHistoricalMarketDataProvider.TIMESERIES_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"symbol MNQM6 was not found for the requested range; api_key=provider-secret\"}"));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-04T14:15:00Z"),
                Instant.parse("2026-06-04T15:00:00Z")
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("MARKET_DATA_REQUEST_REJECTED");
        assertThat(result.sanitizedMessage()).isEqualTo("Historical market data provider rejected the replay request.");
        assertThat(output).contains("Databento historical data fetch failed");
        assertThat(output).contains("symbol MNQM6 was not found for the requested range");
        assertThat(output).doesNotContain("provider-secret");
        assertThat(output).doesNotContain("test-databento-key");
        server.verify();
    }

    @Test
    void capturesCostCheck422AsCostCheckFailure(CapturedOutput output) {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DatabentoHistoricalMarketDataProvider provider = new DatabentoHistoricalMarketDataProvider(properties(true), objectMapper, restTemplate);

        server.expect(once(), requestTo(startsWith(DatabentoHistoricalMarketDataProvider.COST_URL)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("symbols", "MNQM6"))
                .andExpect(queryParam("stype_in", "raw_symbol"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"cost request rejected for symbol MNQM6\"}"));

        HistoricalCandleResult result = provider.getCandles(request(
                Instant.parse("2026-06-04T14:15:00Z"),
                Instant.parse("2026-06-04T15:00:00Z")
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("MARKET_DATA_REQUEST_REJECTED");
        assertThat(output).contains("Databento cost check failed");
        assertThat(output).doesNotContain("Databento historical data fetch failed");
        server.verify();
    }

    private MarketDataProperties properties(boolean costCheckEnabled) {
        MarketDataProperties properties = new MarketDataProperties();
        properties.setProvider("databento");
        properties.getDatabento().setApiKey("test-databento-key");
        properties.getDatabento().setDataset("GLBX.MDP3");
        properties.getDatabento().setSchema("ohlcv-1m");
        properties.getDatabento().setCostCheckEnabled(costCheckEnabled);
        return properties;
    }

    private MarketDataRequest request(Instant start, Instant end) {
        return new MarketDataRequest("MNQ", "MNQM6", start, end, "1m", java.util.Map.of());
    }
}
