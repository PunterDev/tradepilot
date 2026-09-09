package com.tradepilot.preanalysis.entity;

import com.tradepilot.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_playbooks",
        indexes = {
                @Index(name = "idx_trading_playbooks_user_active", columnList = "user_id,is_active"),
                @Index(name = "idx_trading_playbooks_user_created", columnList = "user_id,created_at")
        })
public class TradingPlaybook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(read = "markets::text", write = "?::jsonb")
    private String markets;
    @Column(name = "context_timeframes", columnDefinition = "jsonb")
    @ColumnTransformer(read = "context_timeframes::text", write = "?::jsonb")
    private String contextTimeframes;
    @Column(name = "setup_timeframes", columnDefinition = "jsonb")
    @ColumnTransformer(read = "setup_timeframes::text", write = "?::jsonb")
    private String setupTimeframes;
    @Column(name = "execution_timeframes", columnDefinition = "jsonb")
    @ColumnTransformer(read = "execution_timeframes::text", write = "?::jsonb")
    private String executionTimeframes;
    @Column(name = "setup_sequence", columnDefinition = "jsonb")
    @ColumnTransformer(read = "setup_sequence::text", write = "?::jsonb")
    private String setupSequence;
    @Column(name = "entry_rules", columnDefinition = "jsonb")
    @ColumnTransformer(read = "entry_rules::text", write = "?::jsonb")
    private String entryRules;
    @Column(name = "invalidation_rules", columnDefinition = "jsonb")
    @ColumnTransformer(read = "invalidation_rules::text", write = "?::jsonb")
    private String invalidationRules;
    @Column(name = "target_rules", columnDefinition = "jsonb")
    @ColumnTransformer(read = "target_rules::text", write = "?::jsonb")
    private String targetRules;
    @Column(name = "avoid_rules", columnDefinition = "jsonb")
    @ColumnTransformer(read = "avoid_rules::text", write = "?::jsonb")
    private String avoidRules;
    @Column(name = "custom_notes", columnDefinition = "text")
    private String customNotes;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMarkets() { return markets; }
    public void setMarkets(String markets) { this.markets = markets; }
    public String getContextTimeframes() { return contextTimeframes; }
    public void setContextTimeframes(String contextTimeframes) { this.contextTimeframes = contextTimeframes; }
    public String getSetupTimeframes() { return setupTimeframes; }
    public void setSetupTimeframes(String setupTimeframes) { this.setupTimeframes = setupTimeframes; }
    public String getExecutionTimeframes() { return executionTimeframes; }
    public void setExecutionTimeframes(String executionTimeframes) { this.executionTimeframes = executionTimeframes; }
    public String getSetupSequence() { return setupSequence; }
    public void setSetupSequence(String setupSequence) { this.setupSequence = setupSequence; }
    public String getEntryRules() { return entryRules; }
    public void setEntryRules(String entryRules) { this.entryRules = entryRules; }
    public String getInvalidationRules() { return invalidationRules; }
    public void setInvalidationRules(String invalidationRules) { this.invalidationRules = invalidationRules; }
    public String getTargetRules() { return targetRules; }
    public void setTargetRules(String targetRules) { this.targetRules = targetRules; }
    public String getAvoidRules() { return avoidRules; }
    public void setAvoidRules(String avoidRules) { this.avoidRules = avoidRules; }
    public String getCustomNotes() { return customNotes; }
    public void setCustomNotes(String customNotes) { this.customNotes = customNotes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
