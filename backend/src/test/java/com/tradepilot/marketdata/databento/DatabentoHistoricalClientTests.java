package com.tradepilot.marketdata.databento;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.marketdata.MarketDataException;
import com.tradepilot.marketdata.model.MarketInstrument;
import com.tradepilot.tradereplay.service.MarketDataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabentoHistoricalClientTests {
    @Test
    void estimatesContinuousNqCostWithOfficialHistoricalParameters() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-02T00:00:00Z");

        server.expect(once(), request -> {
                    String value = request.getURI().toString();
                    assertThat(value).contains("metadata.get_cost").contains("dataset=GLBX.MDP3").contains("symbols=NQ.v.0")
                            .contains("stype_in=continuous").contains("schema=ohlcv-1m")
                            .contains("start=2026-01-01T00:00:00Z").contains("end=2026-01-02T00:00:00Z");
                    assertThat(request.getHeaders().getFirst("Authorization")).startsWith("Basic ");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("1.2300", MediaType.TEXT_PLAIN));

        assertThat(client.estimateCost(MarketInstrument.NQ, start, end)).isEqualByComparingTo(new BigDecimal("1.2300"));
        server.verify();
    }

    @Test
    void streamsAndParsesPrettyJsonOhlcvWithoutPriceScalingGuesswork() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);
        String jsonl = "{\"hd\":{\"ts_event\":\"2026-01-01T00:00:00Z\",\"instrument_id\":12345},\"open\":\"21500.250000\",\"high\":\"21501.000000\",\"low\":\"21499.750000\",\"close\":\"21500.500000\",\"volume\":42}\n";
        var bars = new ArrayList<DatabentoOhlcvBar>();

        server.expect(once(), requestTo("https://hist.databento.test/v0/timeseries.get_range"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonl, MediaType.APPLICATION_JSON));

        client.streamOhlcvBars(MarketInstrument.ES, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), bars::add);

        assertThat(bars).singleElement().satisfies(bar -> {
            assertThat(bar.instrumentId()).isEqualTo(12345L);
            assertThat(bar.open()).isEqualByComparingTo("21500.250000");
            assertThat(bar.close()).isEqualByComparingTo("21500.500000");
            assertThat(bar.volume()).isEqualTo(42L);
        });
        server.verify();
    }

    @Test
    void missingApiKeyFailsSafelyWithoutCallingDatabento() {
        MarketDataProperties properties = properties();
        properties.getDatabento().setApiKey("");
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties, new ObjectMapper(), new RestTemplate());

        assertThatThrownBy(() -> client.estimateCost(MarketInstrument.NQ, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(MarketDataException.class)
                .extracting(error -> ((MarketDataException) error).getCode())
                .isEqualTo("MARKET_DATA_NOT_CONFIGURED");
    }

    @Test
    void providerErrorHasSafeErrorCode() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);
        server.expect(once(), request -> { }).andRespond(withBadRequest());

        assertThatThrownBy(() -> client.estimateCost(MarketInstrument.ES, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(MarketDataException.class)
                .extracting(error -> ((MarketDataException) error).getCode())
                .isEqualTo("MARKET_DATA_PROVIDER_REQUEST_FAILED");
    }

    @Test
    void authenticationFailureIsNotReportedAsMissingConfiguration() {
        assertFailure(HttpStatus.UNAUTHORIZED, "MARKET_DATA_PROVIDER_AUTH_FAILED", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void authorizationFailureIsNotReportedAsMissingConfiguration() {
        assertFailure(HttpStatus.FORBIDDEN, "MARKET_DATA_PROVIDER_AUTH_FAILED", HttpStatus.BAD_GATEWAY);
    }

    @Test
    void rateLimitFailureIsReportedAsTemporarilyUnavailable() {
        assertFailure(HttpStatus.TOO_MANY_REQUESTS, "MARKET_DATA_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void providerServerFailureIsNotReportedAsMissingConfiguration() {
        assertFailure(HttpStatus.INTERNAL_SERVER_ERROR, "MARKET_DATA_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void malformedCostResponseHasSafeProviderError() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);
        server.expect(once(), request -> { }).andRespond(withSuccess("not-a-decimal", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.estimateCost(MarketInstrument.ES, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(MarketDataException.class)
                .satisfies(error -> {
                    MarketDataException marketDataError = (MarketDataException) error;
                    assertThat(marketDataError.getCode()).isEqualTo("MARKET_DATA_PROVIDER_ERROR");
                    assertThat(marketDataError.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @Test
    void timeoutIsReportedAsTemporarilyUnavailableNotMissingConfiguration() {
        RestTemplate template = mock(RestTemplate.class);
        when(template.exchange(anyString(), eq(HttpMethod.GET), any(org.springframework.http.HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);

        assertThatThrownBy(() -> client.estimateCost(MarketInstrument.NQ, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(MarketDataException.class)
                .satisfies(error -> {
                    MarketDataException marketDataError = (MarketDataException) error;
                    assertThat(marketDataError.getCode()).isEqualTo("MARKET_DATA_PROVIDER_UNAVAILABLE");
                    assertThat(marketDataError.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    private void assertFailure(HttpStatus upstreamStatus, String expectedCode, HttpStatus expectedStatus) {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        DatabentoHistoricalClient client = new DatabentoHistoricalClient(properties(), new ObjectMapper(), template);
        server.expect(once(), request -> { })
                .andRespond(withStatus(upstreamStatus).body("{\"detail\":\"provider failure\"}"));

        assertThatThrownBy(() -> client.estimateCost(MarketInstrument.ES, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(MarketDataException.class)
                .satisfies(error -> {
                    MarketDataException marketDataError = (MarketDataException) error;
                    assertThat(marketDataError.getCode()).isEqualTo(expectedCode);
                    assertThat(marketDataError.getStatus()).isEqualTo(expectedStatus);
                    assertThat(marketDataError.getCode()).isNotEqualTo("MARKET_DATA_NOT_CONFIGURED");
                });
    }

    private MarketDataProperties properties() {
        MarketDataProperties properties = new MarketDataProperties();
        properties.getDatabento().setApiKey("test-key");
        properties.getDatabento().setHistoricalBaseUrl("https://hist.databento.test/v0");
        properties.getDatabento().setDataset("GLBX.MDP3");
        properties.getDatabento().setSchema("ohlcv-1m");
        return properties;
    }
}
