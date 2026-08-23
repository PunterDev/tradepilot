package com.tradepilot.ai.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradepilot.trading-memory")
public class TradingMemoryProperties {
    private boolean enabled = true;
    private String collectionName = "tradepilot_trade_memories";
    private int vectorSize = 1536;
    private double similarityThreshold = 0.72;
    private int defaultLimit = 5;
    private int maxLimit = 10;
    private int minPatternSample = 5;
    private int minPatternOccurrences = 3;
    private int requestTimeoutMs = 3000;
    private String embeddingModel = "text-embedding-3-small";
    private String qdrantUrl;
    private String qdrantApiKey;
    private boolean qdrantAutoCreateCollection = true;
    private boolean qdrantValidateOnStartup = true;
    private String qdrantExpectedDistance = "COSINE";
    private boolean qdrantCreatePayloadIndexes = true;
    private boolean retryEnabled = false;
    private int retryMaxAttempts = 5;
    private int retryInitialDelaySeconds = 60;
    private int retryMaxDelaySeconds = 3600;
    private int retryBatchSize = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getVectorSize() { return vectorSize; }
    public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
    public int getMaxLimit() { return maxLimit; }
    public void setMaxLimit(int maxLimit) { this.maxLimit = maxLimit; }
    public int getMinPatternSample() { return minPatternSample; }
    public void setMinPatternSample(int minPatternSample) { this.minPatternSample = minPatternSample; }
    public int getMinPatternOccurrences() { return minPatternOccurrences; }
    public void setMinPatternOccurrences(int minPatternOccurrences) { this.minPatternOccurrences = minPatternOccurrences; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getQdrantUrl() { return qdrantUrl; }
    public void setQdrantUrl(String qdrantUrl) { this.qdrantUrl = qdrantUrl; }
    public String getQdrantApiKey() { return qdrantApiKey; }
    public void setQdrantApiKey(String qdrantApiKey) { this.qdrantApiKey = qdrantApiKey; }
    public boolean isQdrantAutoCreateCollection() { return qdrantAutoCreateCollection; }
    public void setQdrantAutoCreateCollection(boolean qdrantAutoCreateCollection) { this.qdrantAutoCreateCollection = qdrantAutoCreateCollection; }
    public boolean isQdrantValidateOnStartup() { return qdrantValidateOnStartup; }
    public void setQdrantValidateOnStartup(boolean qdrantValidateOnStartup) { this.qdrantValidateOnStartup = qdrantValidateOnStartup; }
    public String getQdrantExpectedDistance() { return qdrantExpectedDistance; }
    public void setQdrantExpectedDistance(String qdrantExpectedDistance) { this.qdrantExpectedDistance = qdrantExpectedDistance; }
    public boolean isQdrantCreatePayloadIndexes() { return qdrantCreatePayloadIndexes; }
    public void setQdrantCreatePayloadIndexes(boolean qdrantCreatePayloadIndexes) { this.qdrantCreatePayloadIndexes = qdrantCreatePayloadIndexes; }
    public boolean isRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
    public int getRetryMaxAttempts() { return retryMaxAttempts; }
    public void setRetryMaxAttempts(int retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }
    public int getRetryInitialDelaySeconds() { return retryInitialDelaySeconds; }
    public void setRetryInitialDelaySeconds(int retryInitialDelaySeconds) { this.retryInitialDelaySeconds = retryInitialDelaySeconds; }
    public int getRetryMaxDelaySeconds() { return retryMaxDelaySeconds; }
    public void setRetryMaxDelaySeconds(int retryMaxDelaySeconds) { this.retryMaxDelaySeconds = retryMaxDelaySeconds; }
    public int getRetryBatchSize() { return retryBatchSize; }
    public void setRetryBatchSize(int retryBatchSize) { this.retryBatchSize = retryBatchSize; }
}
