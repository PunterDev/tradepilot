package com.tradepilot.preanalysis.entity;

import com.tradepilot.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_pre_analyses",
        indexes = {
                @Index(name = "idx_ai_pre_analyses_user_created", columnList = "user_id,created_at"),
                @Index(name = "idx_ai_pre_analyses_playbook", columnList = "playbook_id")
        })
public class AiPreAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playbook_id", nullable = false)
    private TradingPlaybook playbook;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PreAnalysisMarket market;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PreAnalysisTimeframe timeframe;
    @Column(name = "image_reference", length = 120)
    private String imageReference;
    @Column(name = "image_content_type", length = 80)
    private String imageContentType;
    @Column(name = "image_bytes", columnDefinition = "bytea")
    private byte[] imageBytes;
    @Column(name = "trader_note", columnDefinition = "text")
    private String traderNote;
    @Enumerated(EnumType.STRING)
    @Column(name = "setup_status", nullable = false, length = 40)
    private SetupStatus setupStatus;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Bias bias;
    @Column(name = "setup_alignment_score")
    private Integer setupAlignmentScore;
    @Column(name = "structure_quality_score", columnDefinition = "double precision")
    private Double structureQualityScore;
    @Column(name = "analysis_json", columnDefinition = "jsonb")
    @ColumnTransformer(read = "analysis_json::text", write = "?::jsonb")
    private String analysisJson;
    @Column(name = "model_name", length = 120)
    private String modelName;
    @Column(name = "prompt_version", nullable = false, length = 60)
    private String promptVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TradingPlaybook getPlaybook() { return playbook; }
    public void setPlaybook(TradingPlaybook playbook) { this.playbook = playbook; }
    public PreAnalysisMarket getMarket() { return market; }
    public void setMarket(PreAnalysisMarket market) { this.market = market; }
    public PreAnalysisTimeframe getTimeframe() { return timeframe; }
    public void setTimeframe(PreAnalysisTimeframe timeframe) { this.timeframe = timeframe; }
    public String getImageReference() { return imageReference; }
    public void setImageReference(String imageReference) { this.imageReference = imageReference; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
    public String getTraderNote() { return traderNote; }
    public void setTraderNote(String traderNote) { this.traderNote = traderNote; }
    public SetupStatus getSetupStatus() { return setupStatus; }
    public void setSetupStatus(SetupStatus setupStatus) { this.setupStatus = setupStatus; }
    public Bias getBias() { return bias; }
    public void setBias(Bias bias) { this.bias = bias; }
    public Integer getSetupAlignmentScore() { return setupAlignmentScore; }
    public void setSetupAlignmentScore(Integer setupAlignmentScore) { this.setupAlignmentScore = setupAlignmentScore; }
    public Double getStructureQualityScore() { return structureQualityScore; }
    public void setStructureQualityScore(Double structureQualityScore) { this.structureQualityScore = structureQualityScore; }
    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String analysisJson) { this.analysisJson = analysisJson; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
