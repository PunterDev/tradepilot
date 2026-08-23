package com.tradepilot.ai.memory.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_memories",
        indexes = {
                @Index(name = "idx_trade_memories_user_id", columnList = "user_id"),
                @Index(name = "idx_trade_memories_embedding_status", columnList = "embedding_status"),
                @Index(name = "idx_trade_memories_status_retry", columnList = "embedding_status,next_retry_at"),
                @Index(name = "idx_trade_memories_updated_at", columnList = "updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trade_memories_trade_id", columnNames = "trade_id"),
                @UniqueConstraint(name = "uk_trade_memories_vector_point_id", columnNames = "vector_point_id")
        })
public class TradeMemory {
    @Id
    private UUID id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vector_point_id", nullable = false)
    private UUID vectorPointId;

    @Column(name = "memory_version", nullable = false)
    private Integer memoryVersion = 1;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "setup_normalized", length = 120)
    private String setupNormalized;

    @Column(name = "emotion_normalized", length = 120)
    private String emotionNormalized;

    @Column(name = "mistake_normalized", length = 120)
    private String mistakeNormalized;

    @Column(name = "setup_source", columnDefinition = "text")
    private String setupSource;

    @Column(name = "emotion_source", columnDefinition = "text")
    private String emotionSource;

    @Column(name = "mistake_source", columnDefinition = "text")
    private String mistakeSource;

    @Column(name = "entry_reason_summary", columnDefinition = "text")
    private String entryReasonSummary;

    @Column(name = "exit_reason_summary", columnDefinition = "text")
    private String exitReasonSummary;

    @Column(name = "market_context_summary", columnDefinition = "text")
    private String marketContextSummary;

    @Column(name = "source_review_id")
    private Long sourceReviewId;

    @Column(name = "content_hash", nullable = false, length = 128)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 30)
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempted_at")
    private OffsetDateTime lastAttemptedAt;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_error_code", length = 80)
    private TradeMemoryErrorCode lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) id = UUID.randomUUID();
        if (vectorPointId == null) vectorPointId = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public UUID getVectorPointId() { return vectorPointId; }
    public void setVectorPointId(UUID vectorPointId) { this.vectorPointId = vectorPointId; }
    public Integer getMemoryVersion() { return memoryVersion; }
    public void setMemoryVersion(Integer memoryVersion) { this.memoryVersion = memoryVersion; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSetupNormalized() { return setupNormalized; }
    public void setSetupNormalized(String setupNormalized) { this.setupNormalized = setupNormalized; }
    public String getEmotionNormalized() { return emotionNormalized; }
    public void setEmotionNormalized(String emotionNormalized) { this.emotionNormalized = emotionNormalized; }
    public String getMistakeNormalized() { return mistakeNormalized; }
    public void setMistakeNormalized(String mistakeNormalized) { this.mistakeNormalized = mistakeNormalized; }
    public String getSetupSource() { return setupSource; }
    public void setSetupSource(String setupSource) { this.setupSource = setupSource; }
    public String getEmotionSource() { return emotionSource; }
    public void setEmotionSource(String emotionSource) { this.emotionSource = emotionSource; }
    public String getMistakeSource() { return mistakeSource; }
    public void setMistakeSource(String mistakeSource) { this.mistakeSource = mistakeSource; }
    public String getEntryReasonSummary() { return entryReasonSummary; }
    public void setEntryReasonSummary(String entryReasonSummary) { this.entryReasonSummary = entryReasonSummary; }
    public String getExitReasonSummary() { return exitReasonSummary; }
    public void setExitReasonSummary(String exitReasonSummary) { this.exitReasonSummary = exitReasonSummary; }
    public String getMarketContextSummary() { return marketContextSummary; }
    public void setMarketContextSummary(String marketContextSummary) { this.marketContextSummary = marketContextSummary; }
    public Long getSourceReviewId() { return sourceReviewId; }
    public void setSourceReviewId(Long sourceReviewId) { this.sourceReviewId = sourceReviewId; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public EmbeddingStatus getEmbeddingStatus() { return embeddingStatus; }
    public void setEmbeddingStatus(EmbeddingStatus embeddingStatus) { this.embeddingStatus = embeddingStatus; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public OffsetDateTime getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(OffsetDateTime lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
    public OffsetDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(OffsetDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public TradeMemoryErrorCode getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(TradeMemoryErrorCode lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getEmbeddedAt() { return embeddedAt; }
    public void setEmbeddedAt(OffsetDateTime embeddedAt) { this.embeddedAt = embeddedAt; }
}
