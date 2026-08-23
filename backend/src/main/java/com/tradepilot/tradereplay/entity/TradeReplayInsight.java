package com.tradepilot.tradereplay.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trade_replay_insights",
        indexes = {@Index(name = "idx_trade_replay_insights_replay_order", columnList = "replay_id, display_order")})
public class TradeReplayInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replay_id", nullable = false)
    private TradeReplay replay;
    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false, length = 50)
    private TradeReplayInsightType type;
    @Column(name = "event_timestamp")
    private Instant eventTimestamp;
    @Column(name = "event_price", precision = 18, scale = 6)
    private BigDecimal eventPrice;
    @Column(length = 20)
    private String severity;
    @Column(name = "deterministic_title", nullable = false, length = 180)
    private String deterministicTitle;
    @Column(name = "deterministic_explanation", columnDefinition = "TEXT")
    private String deterministicExplanation;
    @Column(name = "supporting_metrics_json", columnDefinition = "TEXT")
    private String supportingMetricsJson;
    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public TradeReplay getReplay() { return replay; }
    public void setReplay(TradeReplay replay) { this.replay = replay; }
    public TradeReplayInsightType getType() { return type; }
    public void setType(TradeReplayInsightType type) { this.type = type; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public BigDecimal getEventPrice() { return eventPrice; }
    public void setEventPrice(BigDecimal eventPrice) { this.eventPrice = eventPrice; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDeterministicTitle() { return deterministicTitle; }
    public void setDeterministicTitle(String deterministicTitle) { this.deterministicTitle = deterministicTitle; }
    public String getDeterministicExplanation() { return deterministicExplanation; }
    public void setDeterministicExplanation(String deterministicExplanation) { this.deterministicExplanation = deterministicExplanation; }
    public String getSupportingMetricsJson() { return supportingMetricsJson; }
    public void setSupportingMetricsJson(String supportingMetricsJson) { this.supportingMetricsJson = supportingMetricsJson; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
