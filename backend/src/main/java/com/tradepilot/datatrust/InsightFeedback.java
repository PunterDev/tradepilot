package com.tradepilot.datatrust;

import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insight_feedback", uniqueConstraints = @UniqueConstraint(name = "uk_insight_feedback_user_snapshot_finding", columnNames = {"user_id", "snapshot_id", "finding_type"}))
public class InsightFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private TradingAccount account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "snapshot_id", nullable = false) private AiLeakDetectorSnapshot snapshot;
    @Column(name = "finding_type", nullable = false, length = 80) private String findingType;
    @Column(name = "engine_version", nullable = false, length = 80) private String engineVersion;
    @Column(name = "rule_version", nullable = false, length = 80) private String ruleVersion;
    @Column(name = "evidence_strength", nullable = false, length = 30) private String evidenceStrength;
    @Column(nullable = false) private boolean helpful;
    @Enumerated(EnumType.STRING) @Column(length = 80) private InsightFeedbackReason reason;
    @Column(columnDefinition = "text") private String comment;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void created() { createdAt = updatedAt = LocalDateTime.now(); } @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public User getUser(){return user;} public void setUser(User v){user=v;} public TradingAccount getAccount(){return account;} public void setAccount(TradingAccount v){account=v;} public AiLeakDetectorSnapshot getSnapshot(){return snapshot;} public void setSnapshot(AiLeakDetectorSnapshot v){snapshot=v;} public String getFindingType(){return findingType;} public void setFindingType(String v){findingType=v;} public String getEngineVersion(){return engineVersion;} public void setEngineVersion(String v){engineVersion=v;} public String getRuleVersion(){return ruleVersion;} public void setRuleVersion(String v){ruleVersion=v;} public String getEvidenceStrength(){return evidenceStrength;} public void setEvidenceStrength(String v){evidenceStrength=v;} public boolean isHelpful(){return helpful;} public void setHelpful(boolean v){helpful=v;} public InsightFeedbackReason getReason(){return reason;} public void setReason(InsightFeedbackReason v){reason=v;} public String getComment(){return comment;} public void setComment(String v){comment=v;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
