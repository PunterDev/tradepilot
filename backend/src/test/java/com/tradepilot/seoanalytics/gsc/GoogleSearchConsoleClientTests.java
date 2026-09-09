package com.tradepilot.seoanalytics.gsc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleSearchConsoleClientTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsCtrPositionZeroClicksAndPaginatesUntilShortPage() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GoogleSearchConsoleProperties properties = new GoogleSearchConsoleProperties();
        properties.setProperty("sc-domain:tradepilotcoach.com");
        properties.setPageSize(2);
        properties.setMaxRowsPerQuery(5);
        GoogleSearchConsoleClient client = new GoogleSearchConsoleClient(restTemplate, properties, () -> "test-token");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(ResponseEntity.ok(objectMapper.readTree("""
                        {"rows":[
                          {"keys":["revenge trading calculator","https://tradepilotcoach.com/tools/revenge-trading-calculator"],"clicks":140,"impressions":2800,"ctr":0.05,"position":7.4},
                          {"keys":["zero click","https://tradepilotcoach.com/revenge-trading"],"clicks":0,"impressions":10,"ctr":0,"position":18.2}
                        ]}
                        """)))
                .thenReturn(ResponseEntity.ok(objectMapper.readTree("""
                        {"rows":[
                          {"keys":["tradepilot","https://tradepilotcoach.com/"],"clicks":10,"impressions":200,"ctr":0.05,"position":1.2}
                        ]}
                        """)));

        var rows = client.query(SearchConsoleQueryType.QUERY_PAGE, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).query()).isEqualTo("revenge trading calculator");
        assertThat(rows.get(0).page()).isEqualTo("https://tradepilotcoach.com/tools/revenge-trading-calculator");
        assertThat(rows.get(0).clicks()).isEqualTo(140);
        assertThat(rows.get(0).impressions()).isEqualTo(2800);
        assertThat(rows.get(0).ctr()).isEqualTo(0.05);
        assertThat(rows.get(0).position()).isEqualTo(7.4);
        assertThat(rows.get(1).clicks()).isZero();
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(com.fasterxml.jackson.databind.JsonNode.class));
    }
}
