package com.tradepilot.seoanalytics;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_acquisition_attribution", indexes = {
        @Index(name = "idx_user_acq_landing", columnList = "landing_page"),
        @Index(name = "idx_user_acq_signup_completed", columnList = "signup_completed_at"),
        @Index(name = "idx_user_acq_first_insight", columnList = "first_insight_generated_at"),
        @Index(name = "idx_user_acq_pro_converted", columnList = "pro_converted_at"),
        @Index(name = "idx_user_acq_source_medium", columnList = "utm_source, utm_medium")
})
public class UserAcquisitionAttribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "landing_page", nullable = false, length = 220)
    private String landingPage;

    @Column(name = "initial_referrer", length = 500)
    private String initialReferrer;

    @Column(name = "utm_source", length = 120)
    private String utmSource;

    @Column(name = "utm_medium", length = 120)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 160)
    private String utmCampaign;

    @Column(name = "utm_content", length = 160)
    private String utmContent;

    @Column(name = "utm_term", length = 160)
    private String utmTerm;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "signup_completed_at", nullable = false)
    private LocalDateTime signupCompletedAt;

    @Column(name = "first_import_completed_at")
    private LocalDateTime firstImportCompletedAt;

    @Column(name = "first_insight_generated_at")
    private LocalDateTime firstInsightGeneratedAt;

    @Column(name = "pro_converted_at")
    private LocalDateTime proConvertedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAnonymousId() { return anonymousId; }
    public String getLandingPage() { return landingPage; }
    public String getInitialReferrer() { return initialReferrer; }
    public String getUtmSource() { return utmSource; }
    public String getUtmMedium() { return utmMedium; }
    public String getUtmCampaign() { return utmCampaign; }
    public String getUtmContent() { return utmContent; }
    public String getUtmTerm() { return utmTerm; }
    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public LocalDateTime getSignupCompletedAt() { return signupCompletedAt; }
    public LocalDateTime getFirstImportCompletedAt() { return firstImportCompletedAt; }
    public LocalDateTime getFirstInsightGeneratedAt() { return firstInsightGeneratedAt; }
    public LocalDateTime getProConvertedAt() { return proConvertedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
    public void setLandingPage(String landingPage) { this.landingPage = landingPage; }
    public void setInitialReferrer(String initialReferrer) { this.initialReferrer = initialReferrer; }
    public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
    public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
    public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    public void setUtmContent(String utmContent) { this.utmContent = utmContent; }
    public void setUtmTerm(String utmTerm) { this.utmTerm = utmTerm; }
    public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public void setSignupCompletedAt(LocalDateTime signupCompletedAt) { this.signupCompletedAt = signupCompletedAt; }
    public void setFirstImportCompletedAt(LocalDateTime firstImportCompletedAt) { this.firstImportCompletedAt = firstImportCompletedAt; }
    public void setFirstInsightGeneratedAt(LocalDateTime firstInsightGeneratedAt) { this.firstInsightGeneratedAt = firstInsightGeneratedAt; }
    public void setProConvertedAt(LocalDateTime proConvertedAt) { this.proConvertedAt = proConvertedAt; }
}
