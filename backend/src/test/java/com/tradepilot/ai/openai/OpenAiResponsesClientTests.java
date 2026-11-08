package com.tradepilot.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.observability.ObservabilityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiResponsesClientTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void gpt6UsesResponsesApiWithNoStorageAndReasoningWithoutTemperature() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObservabilityService observability = mock(ObservabilityService.class);
        when(restTemplate.exchange(eq("https://api.openai.com/v1/responses"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"{\\\"grade\\\":\\\"A\\\"}"}]}],"usage":{"input_tokens":11,"input_tokens_details":{"cached_tokens":3},"output_tokens":7,"output_tokens_details":{"reasoning_tokens":2},"total_tokens":18}}
                        """));

        OpenAiResponsesClient client = new OpenAiResponsesClient(restTemplate, objectMapper, properties(), observability);
        OpenAiGeneratedResponse result = client.generateJson(9L, "AI_REVIEW", "system", "prompt");

        assertThat(result.content()).isEqualTo("{\"grade\":\"A\"}");
        assertThat(result.usage()).isEqualTo(new OpenAiUsage(11, 3, 7, 2, 18));
        ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://api.openai.com/v1/responses"), eq(HttpMethod.POST), request.capture(), eq(String.class));
        JsonNode json = objectMapper.valueToTree(request.getValue().getBody());
        assertThat(json.path("model").asText()).isEqualTo("gpt-6-luna");
        assertThat(json.path("store").asBoolean()).isFalse();
        assertThat(json.path("max_output_tokens").asInt()).isEqualTo(4096);
        assertThat(json.path("reasoning").path("effort").asText()).isEqualTo("low");
        assertThat(json.has("temperature")).isFalse();
        assertThat(json.path("input").get(0).path("role").asText()).isEqualTo("developer");
        verify(observability).recordAiCall(eq(9L), eq("AI_REVIEW"), eq("gpt-6-luna"), eq(result.usage()), eq(new BigDecimal("0.000024")), org.mockito.ArgumentMatchers.anyLong(), eq(1), eq(true), eq(null));
    }

    @Test
    void incompleteResponseFailsClosedAndRecordsFailure() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObservabilityService observability = mock(ObservabilityService.class);
        when(restTemplate.exchange(eq("https://api.openai.com/v1/responses"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"}}"));

        OpenAiResponsesClient client = new OpenAiResponsesClient(restTemplate, objectMapper, properties(), observability);

        assertThatThrownBy(() -> client.generateJson(9L, "AI_REVIEW", "system", "prompt"))
                .isInstanceOf(OpenAiUnavailableException.class)
                .hasMessageContaining("max_output_tokens");
        verify(observability).recordAiCall(eq(9L), eq("AI_REVIEW"), eq("gpt-6-luna"), any(OpenAiUsage.class), eq(BigDecimal.ZERO), org.mockito.ArgumentMatchers.anyLong(), eq(1), eq(false), eq("RESPONSE_CONTRACT"));
    }

    @Test
    void rollbackFlagUsesLegacyChatCompletionsOnlyWhenExplicitlyEnabled() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObservabilityService observability = mock(ObservabilityService.class);
        OpenAiProperties properties = properties();
        properties.setLegacyChatCompletionsEnabled(true);
        properties.setModel("gpt-4.1-mini");
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"{}\"}}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":3}}"));

        OpenAiResponsesClient client = new OpenAiResponsesClient(restTemplate, objectMapper, properties, observability);
        client.generateJson(9L, "AI_REVIEW", "system", "prompt");

        ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://api.openai.com/v1/chat/completions"), eq(HttpMethod.POST), request.capture(), eq(String.class));
        JsonNode json = objectMapper.valueToTree(request.getValue().getBody());
        assertThat(json.path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(json.path("max_tokens").asInt()).isEqualTo(4096);
        assertThat(json.has("store")).isFalse();
    }

    @Test
    void unsupportedResponsesModelOrReasoningCombinationFailsBeforeProviderCall() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenAiProperties properties = properties();
        properties.setReasoningEffort("unsupported");
        OpenAiResponsesClient client = new OpenAiResponsesClient(restTemplate, objectMapper, properties, mock(ObservabilityService.class));

        assertThatThrownBy(() -> client.generateJson(9L, "AI_REVIEW", "system", "prompt"))
                .isInstanceOf(OpenAiUnavailableException.class)
                .hasMessageContaining("not supported");
        verify(restTemplate, org.mockito.Mockito.never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void invalidOutputLimitFailsBeforeProviderCall() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenAiProperties properties = properties();
        properties.setMaxOutputTokens(128_001);
        OpenAiResponsesClient client = new OpenAiResponsesClient(restTemplate, objectMapper, properties, mock(ObservabilityService.class));

        assertThatThrownBy(() -> client.generateJson(9L, "AI_REVIEW", "system", "prompt"))
                .isInstanceOf(OpenAiUnavailableException.class)
                .hasMessageContaining("not supported");
        verify(restTemplate, org.mockito.Mockito.never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    private OpenAiProperties properties() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setInputCostPerMillionTokens(new BigDecimal("1.00"));
        properties.setCachedInputCostPerMillionTokens(new BigDecimal("0.50"));
        properties.setOutputCostPerMillionTokens(new BigDecimal("2.00"));
        return properties;
    }
}
