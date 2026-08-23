package com.tradepilot.ai.memory.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.config.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiTradeEmbeddingService implements TradeEmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiTradeEmbeddingService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final TradingMemoryProperties properties;

    public OpenAiTradeEmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper, OpenAiProperties openAiProperties, TradingMemoryProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
        this.properties = properties;
    }

    @Override
    public List<Double> createEmbedding(String content) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for Trading Memory embeddings.");
        }
        Instant start = Instant.now();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiProperties.getApiKey());
        Map<String, Object> body = Map.of("model", properties.getEmbeddingModel(), "input", content);
        ResponseEntity<String> response = restTemplate.exchange("https://api.openai.com/v1/embeddings", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode embeddingNode = objectMapper.readTree(response.getBody()).path("data").get(0).path("embedding");
            List<Double> vector = new ArrayList<>();
            embeddingNode.forEach(node -> vector.add(node.asDouble()));
            if (vector.size() != properties.getVectorSize()) {
                throw new IllegalStateException("Embedding dimension mismatch. expected=" + properties.getVectorSize() + ", actual=" + vector.size());
            }
            log.info("Trading Memory embedding created. model={}, dimension={}, durationMs={}", properties.getEmbeddingModel(), vector.size(), Duration.between(start, Instant.now()).toMillis());
            return vector;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse embedding response.", ex);
        }
    }
}
