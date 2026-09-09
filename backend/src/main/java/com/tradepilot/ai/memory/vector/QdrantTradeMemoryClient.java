package com.tradepilot.ai.memory.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;
import com.tradepilot.ai.memory.vector.model.TradeMemoryVectorPayload;
import com.tradepilot.ai.memory.vector.model.TradingMemorySearchRequest;
import com.tradepilot.ai.memory.vector.model.VectorSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class QdrantTradeMemoryClient implements TradingMemoryVectorClient {
    private static final Logger log = LoggerFactory.getLogger(QdrantTradeMemoryClient.class);
    private static final List<String> REQUIRED_PAYLOAD_FIELDS = List.of(
            "userId", "accountId", "tradeId", "instrument", "direction", "setup", "mistake", "emotion", "session", "tradeDate", "memoryVersion"
    );
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TradingMemoryProperties properties;
    private volatile QdrantCollectionValidationResult lastValidation = QdrantCollectionValidationResult.unavailable("Qdrant validation has not run.");

    public QdrantTradeMemoryClient(RestTemplate restTemplate, ObjectMapper objectMapper, TradingMemoryProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getQdrantUrl() != null
                && !properties.getQdrantUrl().isBlank()
                && lastValidation.operational();
    }

    @Override
    public void ensureCollection() {
        if (!configured()) return;
        QdrantCollectionValidationResult validation = validateCollection();
        if (!validation.operational()) {
            throw new IllegalStateException(String.join("; ", validation.errors()));
        }
    }

    @Override
    public QdrantCollectionValidationResult validateCollection() {
        if (!configured()) {
            lastValidation = QdrantCollectionValidationResult.unavailable("Qdrant URL is not configured.");
            return lastValidation;
        }
        List<String> errors = new ArrayList<>();
        String url = baseUrl() + "/collections/" + properties.getCollectionName();
        boolean reachable = false;
        boolean exists = false;
        boolean vectorSizeValid = false;
        boolean distanceValid = false;
        boolean payloadIndexesValid = true;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity(null), String.class);
            reachable = true;
            exists = true;
            JsonNode result = objectMapper.readTree(response.getBody()).path("result");
            Integer actualSize = vectorSize(result);
            String actualDistance = distance(result);
            if (actualSize == null) {
                errors.add("Trading Memory unavailable: Qdrant collection vector size could not be verified.");
            } else if (actualSize == properties.getVectorSize()) {
                vectorSizeValid = true;
            } else {
                errors.add("Trading Memory unavailable: Qdrant collection vector size is " + actualSize + ", but configured embeddings produce " + properties.getVectorSize() + " dimensions.");
            }
            String expectedDistance = normalizeDistance(properties.getQdrantExpectedDistance());
            if (actualDistance == null) {
                errors.add("Trading Memory unavailable: Qdrant collection distance metric could not be verified.");
            } else if (expectedDistance.equals(normalizeDistance(actualDistance))) {
                distanceValid = true;
            } else {
                errors.add("Trading Memory unavailable: Qdrant collection distance metric is " + actualDistance + ", expected " + expectedDistance + ".");
            }
            if (properties.isQdrantCreatePayloadIndexes()) {
                payloadIndexesValid = ensurePayloadIndexes(result);
                if (!payloadIndexesValid) {
                    errors.add("Trading Memory warning: one or more Qdrant payload indexes could not be verified or created.");
                }
            }
        } catch (HttpClientErrorException.NotFound missing) {
            reachable = true;
            if (!properties.isQdrantAutoCreateCollection()) {
                errors.add("Trading Memory unavailable: Qdrant collection is missing and auto-create is disabled.");
            } else {
                createCollection(url);
                exists = true;
                vectorSizeValid = true;
                distanceValid = true;
                payloadIndexesValid = createPayloadIndexes();
                if (!payloadIndexesValid) {
                    errors.add("Trading Memory warning: Qdrant collection was created, but one or more payload indexes could not be created.");
                }
            }
        } catch (Exception ex) {
            errors.add("Trading Memory unavailable: Qdrant validation failed with " + ex.getClass().getSimpleName() + ".");
        }
        boolean operational = reachable && exists && vectorSizeValid && distanceValid && payloadIndexesValid;
        lastValidation = new QdrantCollectionValidationResult(reachable, exists, vectorSizeValid, distanceValid, payloadIndexesValid, operational, errors, java.time.OffsetDateTime.now());
        if (operational) {
            log.info("trading_memory_collection_validation_succeeded collection={} vectorSize={} distance={}", properties.getCollectionName(), properties.getVectorSize(), normalizeDistance(properties.getQdrantExpectedDistance()));
        } else {
            log.warn("trading_memory_collection_validation_failed collection={} errors={}", properties.getCollectionName(), errors);
        }
        return lastValidation;
    }

    private void createCollection(String url) {
            Map<String, Object> vectors = Map.of("size", properties.getVectorSize(), "distance", "Cosine");
            restTemplate.exchange(url, HttpMethod.PUT, entity(Map.of("vectors", vectors)), String.class);
            log.info("Trading Memory Qdrant collection created. collection={}, vectorSize={}", properties.getCollectionName(), properties.getVectorSize());
    }

    @Override
    public void upsert(UUID pointId, List<Double> vector, TradeMemoryVectorPayload payload) {
        ensureCollection();
        Map<String, Object> qdrantPayload = new LinkedHashMap<>();
        qdrantPayload.put("tradeId", String.valueOf(payload.tradeId()));
        qdrantPayload.put("userId", String.valueOf(payload.userId()));
        qdrantPayload.put("accountId", payload.accountId() == null ? null : String.valueOf(payload.accountId()));
        qdrantPayload.put("instrument", payload.instrument());
        qdrantPayload.put("direction", payload.direction());
        qdrantPayload.put("setup", payload.setup());
        qdrantPayload.put("mistake", payload.mistake());
        qdrantPayload.put("emotion", payload.emotion());
        qdrantPayload.put("session", payload.session());
        qdrantPayload.put("tradeDate", payload.tradeDate() == null ? null : payload.tradeDate().toString());
        qdrantPayload.put("memoryVersion", payload.memoryVersion());
        Map<String, Object> point = Map.of("id", pointId.toString(), "vector", vector, "payload", qdrantPayload);
        restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/points?wait=true", HttpMethod.PUT, entity(Map.of("points", List.of(point))), String.class);
    }

    @Override
    public List<VectorSearchResult> search(Long userId, List<Double> queryVector, Long excludeTradeId, int limit) {
        return search(new TradingMemorySearchRequest(userId, queryVector, excludeTradeId, limit, null, null, null, null, null, null));
    }

    @Override
    public List<VectorSearchResult> search(TradingMemorySearchRequest request) {
        Instant start = Instant.now();
        if (request.userId() == null) {
            throw new IllegalArgumentException("Trading Memory Qdrant search requires userId filter.");
        }
        Map<String, Object> mustUser = Map.of("key", "userId", "match", Map.of("value", String.valueOf(request.userId())));
        List<Object> must = new ArrayList<>();
        must.add(mustUser);
        if (request.accountId() != null) {
            must.add(Map.of("key", "accountId", "match", Map.of("value", String.valueOf(request.accountId()))));
        }
        addMatch(must, "setup", request.setup());
        addMatch(must, "instrument", request.instrument());
        addMatch(must, "direction", request.direction());
        addMatch(must, "session", request.session());
        if (request.tradeDateFrom() != null) {
            must.add(Map.of("key", "tradeDate", "range", Map.of("gte", request.tradeDateFrom().toString())));
        }
        List<Object> mustNot = request.excludeTradeId() == null ? List.of() : List.of(Map.of("key", "tradeId", "match", Map.of("value", String.valueOf(request.excludeTradeId()))));
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("must", must);
        if (!mustNot.isEmpty()) filter.put("must_not", mustNot);
        Map<String, Object> body = Map.of("vector", request.queryVector(), "limit", request.limit(), "with_payload", true, "filter", filter);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/points/search", HttpMethod.POST, entity(body), String.class);
        try {
            JsonNode results = objectMapper.readTree(response.getBody()).path("result");
            List<VectorSearchResult> parsed = new ArrayList<>();
            for (JsonNode result : results) {
                Long tradeId = parseTradeId(result.path("payload").path("tradeId"));
                if (tradeId != null) parsed.add(new VectorSearchResult(tradeId, result.path("score").asDouble()));
            }
            log.info("Trading Memory Qdrant search completed. userId={}, results={}, durationMs={}", request.userId(), parsed.size(), Duration.between(start, Instant.now()).toMillis());
            return parsed;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse Qdrant search response.", ex);
        }
    }

    @Override
    public void delete(UUID pointId) {
        if (!configured() || pointId == null) return;
        Map<String, Object> body = Map.of("points", List.of(pointId.toString()));
        restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/points/delete?wait=true", HttpMethod.POST, entity(body), String.class);
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (!configured() || userId == null) return;
        Map<String, Object> filter = Map.of("must", List.of(Map.of("key", "userId", "match", Map.of("value", String.valueOf(userId)))));
        restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/points/delete?wait=true", HttpMethod.POST, entity(Map.of("filter", filter)), String.class);
    }

    @Override
    public void deleteByUserIdAndAccountId(Long userId, Long accountId) {
        if (!configured() || userId == null || accountId == null) return;
        Map<String, Object> filter = Map.of("must", List.of(
                Map.of("key", "userId", "match", Map.of("value", String.valueOf(userId))),
                Map.of("key", "accountId", "match", Map.of("value", String.valueOf(accountId)))
        ));
        restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/points/delete?wait=true", HttpMethod.POST, entity(Map.of("filter", filter)), String.class);
    }

    private boolean configured() {
        return properties.isEnabled() && properties.getQdrantUrl() != null && !properties.getQdrantUrl().isBlank();
    }

    private void addMatch(List<Object> must, String key, String value) {
        if (value != null && !value.isBlank()) {
            must.add(Map.of("key", key, "match", Map.of("value", value)));
        }
    }

    private Integer vectorSize(JsonNode result) {
        JsonNode vectors = result.path("config").path("params").path("vectors");
        if (vectors.has("size")) return vectors.path("size").asInt();
        Iterator<JsonNode> children = vectors.elements();
        if (children.hasNext()) {
            JsonNode first = children.next();
            if (first.has("size")) return first.path("size").asInt();
        }
        return null;
    }

    private String distance(JsonNode result) {
        JsonNode vectors = result.path("config").path("params").path("vectors");
        if (vectors.has("distance")) return vectors.path("distance").asText(null);
        Iterator<JsonNode> children = vectors.elements();
        if (children.hasNext()) {
            JsonNode first = children.next();
            if (first.has("distance")) return first.path("distance").asText(null);
        }
        return null;
    }

    private boolean ensurePayloadIndexes(JsonNode result) {
        JsonNode schema = result.path("payload_schema");
        boolean allPresent = true;
        for (String field : REQUIRED_PAYLOAD_FIELDS) {
            if (!schema.has(field)) {
                allPresent = false;
            }
        }
        if (allPresent) return true;
        return createPayloadIndexes();
    }

    private boolean createPayloadIndexes() {
        boolean ok = true;
        for (String field : REQUIRED_PAYLOAD_FIELDS) {
            try {
                String type = "tradeDate".equals(field) ? "datetime" : "keyword";
                if ("memoryVersion".equals(field)) type = "integer";
                restTemplate.exchange(baseUrl() + "/collections/" + properties.getCollectionName() + "/index", HttpMethod.PUT, entity(Map.of("field_name", field, "field_schema", type)), String.class);
            } catch (Exception ex) {
                ok = false;
                log.warn("Trading Memory Qdrant payload index creation failed. field={}, reason={}", field, ex.getClass().getSimpleName());
            }
        }
        return ok;
    }

    private String normalizeDistance(String value) {
        return value == null ? "COSINE" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Long parseTradeId(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try {
            return Long.parseLong(node.asText());
        } catch (Exception ignored) {
            return null;
        }
    }

    private HttpEntity<Object> entity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getQdrantApiKey() != null && !properties.getQdrantApiKey().isBlank()) {
            headers.set("api-key", properties.getQdrantApiKey());
        }
        return new HttpEntity<>(body, headers);
    }

    private String baseUrl() {
        return properties.getQdrantUrl().replaceAll("/+$", "");
    }
}
