package com.tradepilot.preanalysis.entity;

import com.tradepilot.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_pre_analysis_feedback",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_pre_analysis_feedback_analysis_user", columnNames = {"analysis_id", "user_id"}),
        indexes = @Index(name = "idx_ai_pre_analysis_feedback_user", columnList = "user_id"))
public class AiPreAnalysisFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AiPreAnalysis analysis;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private Integer rating;
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 60)
    private FeedbackReasonCode reasonCode;
    @Column(columnDefinition = "text")
    private String comment;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AiPreAnalysis getAnalysis() { return analysis; }
    public void setAnalysis(AiPreAnalysis analysis) { this.analysis = analysis; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public FeedbackReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(FeedbackReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
