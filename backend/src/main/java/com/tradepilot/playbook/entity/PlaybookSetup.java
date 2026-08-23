package com.tradepilot.playbook.entity;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playbook_setup", indexes = {
        @Index(name = "idx_playbook_setup_user_id", columnList = "user_id"),
        @Index(name = "idx_playbook_setup_account_id", columnList = "account_id"),
        @Index(name = "idx_playbook_setup_status", columnList = "status"),
        @Index(name = "idx_playbook_setup_updated_at", columnList = "updated_at")
})
public class PlaybookSetup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private TradingAccount account;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "short_description", length = 280)
    private String shortDescription;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PlaybookCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PlaybookStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlaybookDirection direction;
    @Column(name = "instruments_json", columnDefinition = "text")
    private String instrumentsJson;
    @Column(name = "sessions_json", columnDefinition = "text")
    private String sessionsJson;
    @Column(name = "timeframes_json", columnDefinition = "text")
    private String timeframesJson;
    @Column(name = "tags_json", columnDefinition = "text")
    private String tagsJson;
    @Column(name = "market_context_json", columnDefinition = "text")
    private String marketContextJson;
    @Column(name = "risk_rules_json", columnDefinition = "text")
    private String riskRulesJson;
    @Column(name = "management_rules_json", columnDefinition = "text")
    private String managementRulesJson;
    @Column(name = "post_trade_rules_json", columnDefinition = "text")
    private String postTradeRulesJson;
    @Column(name = "icon_key", length = 60)
    private String iconKey;
    @Column(name = "color_key", length = 60)
    private String colorKey;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PlaybookStatus.DRAFT;
        if (category == null) category = PlaybookCategory.CUSTOM;
        if (direction == null) direction = PlaybookDirection.BOTH;
        if (versionNumber == null) versionNumber = 1;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        versionNumber = versionNumber == null ? 1 : versionNumber + 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TradingAccount getAccount() { return account; }
    public void setAccount(TradingAccount account) { this.account = account; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PlaybookCategory getCategory() { return category; }
    public void setCategory(PlaybookCategory category) { this.category = category; }
    public PlaybookStatus getStatus() { return status; }
    public void setStatus(PlaybookStatus status) { this.status = status; }
    public PlaybookDirection getDirection() { return direction; }
    public void setDirection(PlaybookDirection direction) { this.direction = direction; }
    public String getInstrumentsJson() { return instrumentsJson; }
    public void setInstrumentsJson(String instrumentsJson) { this.instrumentsJson = instrumentsJson; }
    public String getSessionsJson() { return sessionsJson; }
    public void setSessionsJson(String sessionsJson) { this.sessionsJson = sessionsJson; }
    public String getTimeframesJson() { return timeframesJson; }
    public void setTimeframesJson(String timeframesJson) { this.timeframesJson = timeframesJson; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getMarketContextJson() { return marketContextJson; }
    public void setMarketContextJson(String marketContextJson) { this.marketContextJson = marketContextJson; }
    public String getRiskRulesJson() { return riskRulesJson; }
    public void setRiskRulesJson(String riskRulesJson) { this.riskRulesJson = riskRulesJson; }
    public String getManagementRulesJson() { return managementRulesJson; }
    public void setManagementRulesJson(String managementRulesJson) { this.managementRulesJson = managementRulesJson; }
    public String getPostTradeRulesJson() { return postTradeRulesJson; }
    public void setPostTradeRulesJson(String postTradeRulesJson) { this.postTradeRulesJson = postTradeRulesJson; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public String getColorKey() { return colorKey; }
    public void setColorKey(String colorKey) { this.colorKey = colorKey; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
}
