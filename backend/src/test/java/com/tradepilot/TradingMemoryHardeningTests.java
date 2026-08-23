package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.TradingMemoryInsightSource;
import com.tradepilot.ai.memory.dto.TradingMemoryMetricsResponse;
import com.tradepilot.ai.memory.entity.TradeMemoryErrorCode;
import com.tradepilot.ai.memory.service.TradeMemoryErrorClassifier;
import com.tradepilot.ai.memory.service.TradeMemoryIndexPublisher;
import com.tradepilot.ai.memory.service.TradeMemoryIndexService;
import com.tradepilot.ai.memory.service.TradeMemoryPromptService;
import com.tradepilot.ai.memory.vector.QdrantTradeMemoryClient;
import com.tradepilot.ai.memory.vector.model.TradingMemorySearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TradingMemoryHardeningTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void qdrantValidationRejectsMissingCollectionWhenAutoCreateDisabled() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TradingMemoryProperties properties = properties();
        properties.setQdrantAutoCreateCollection(false);
        QdrantTradeMemoryClient client = new QdrantTradeMemoryClient(restTemplate, objectMapper, properties);

        server.expect(once(), requestTo("http://qdrant.test/collections/tradepilot_trade_memories"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        var result = client.validateCollection();

        assertThat(result.reachable()).isTrue();
        assertThat(result.collectionExists()).isFalse();
        assertThat(result.operational()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("auto-create is disabled"));
        server.verify();
    }

    @Test
    void qdrantValidationAcceptsExpectedVectorSizeAndDistance() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TradingMemoryProperties properties = properties();
        properties.setQdrantCreatePayloadIndexes(false);
        QdrantTradeMemoryClient client = new QdrantTradeMemoryClient(restTemplate, objectMapper, properties);

        server.expect(once(), requestTo("http://qdrant.test/collections/tradepilot_trade_memories"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"result":{"config":{"params":{"vectors":{"size":1536,"distance":"Cosine"}}},"payload_schema":{}}}
                        """, MediaType.APPLICATION_JSON));

        var result = client.validateCollection();

        assertThat(result.operational()).isTrue();
        assertThat(result.vectorSizeValid()).isTrue();
        assertThat(result.distanceMetricValid()).isTrue();
        server.verify();
    }

    @Test
    void qdrantSearchAlwaysIncludesUserFilterAndCurrentTradeExclusion() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QdrantTradeMemoryClient client = new QdrantTradeMemoryClient(restTemplate, objectMapper, properties());

        server.expect(once(), requestTo("http://qdrant.test/collections/tradepilot_trade_memories/points/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"key\":\"userId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"42\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"must_not\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"key\":\"tradeId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"99\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"key\":\"setup\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"IFVG\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"key\":\"instrument\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"NQ\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"key\":\"direction\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"LONG\"")))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));

        client.search(new TradingMemorySearchRequest(42L, List.of(0.1, 0.2), 99L, 5, "IFVG", "NQ", "LONG", null, null));

        server.verify();
    }

    @Test
    void deterministicInsightIsExplicitlyLabeled() {
        TradeMemoryPromptService promptService = new TradeMemoryPromptService();

        var metrics = new TradingMemoryMetricsResponse(1, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, 0, 0, null, 0, null, 0);
        var insight = promptService.buildDeterministicInsight(null, metrics, null, List.of(
                new com.tradepilot.ai.memory.dto.SimilarTradeResponse(1L, null, "NQ", "LONG", "IFVG", null, null, null, null, null, 0.9, "Summary")
        ));

        assertThat(insight.source()).isEqualTo(TradingMemoryInsightSource.DETERMINISTIC);
    }

    @Test
    void errorClassifierSeparatesRetryableAndNonRetryableFailures() {
        TradeMemoryErrorClassifier classifier = new TradeMemoryErrorClassifier();

        assertThat(classifier.classify(new IllegalStateException("vector size dimension mismatch"))).isEqualTo(TradeMemoryErrorCode.EMBEDDING_DIMENSION_MISMATCH);
        assertThat(classifier.retryable(TradeMemoryErrorCode.EMBEDDING_DIMENSION_MISMATCH)).isFalse();
        assertThat(classifier.classify(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))).isEqualTo(TradeMemoryErrorCode.RATE_LIMITED);
        assertThat(classifier.retryable(TradeMemoryErrorCode.RATE_LIMITED)).isTrue();
    }

    @Test
    void indexPublisherDefersIndexingUntilAfterCommitWhenTransactionIsActive() {
        TradeMemoryIndexService indexService = mock(TradeMemoryIndexService.class);
        com.tradepilot.repository.UserRepository userRepository = mock(com.tradepilot.repository.UserRepository.class);
        com.tradepilot.featureusage.FeatureUsageService featureUsageService = mock(com.tradepilot.featureusage.FeatureUsageService.class);
        TradeMemoryIndexPublisher publisher = new TradeMemoryIndexPublisher(indexService, userRepository, featureUsageService);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.requestIndexAfterCommit(7L, 12L);

            verify(indexService, never()).indexAfterReviewAsync(anyLong(), anyLong());
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(indexService).indexAfterReviewAsync(7L, 12L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private TradingMemoryProperties properties() {
        TradingMemoryProperties properties = new TradingMemoryProperties();
        properties.setQdrantUrl("http://qdrant.test");
        properties.setCollectionName("tradepilot_trade_memories");
        properties.setVectorSize(1536);
        properties.setQdrantExpectedDistance("COSINE");
        return properties;
    }
}
