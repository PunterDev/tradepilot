package com.tradepilot.seoanalytics;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seo_acquisition_events", indexes = {
        @Index(name = "idx_seo_acq_events_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_seo_acq_events_event_occurred", columnList = "event_name, occurred_at"),
        @Index(name = "idx_seo_acq_events_landing_occurred", columnList = "landing_page, occurred_at"),
        @Index(name = "idx_seo_acq_events_tool_occurred", columnList = "tool, occurred_at"),
        @Index(name = "idx_seo_acq_events_anonymous", columnList = "anonymous_id")
})
public class SeoAcquisitionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 36, unique = true)
    private String eventId;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "event_name", nullable = false, length = 80)
    private String eventName;

    @Column(name = "landing_page", nullable = false, length = 220)
    private String landingPage;

    @Column(name = "tool", length = 80)
    private String tool;

    @Column(name = "cta_label", length = 120)
    private String ctaLabel;

    @Column(name = "cta_destination", length = 220)
    private String ctaDestination;

    @Column(name = "referrer", length = 500)
    private String referrer;

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

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (occurredAt == null) occurredAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAnonymousId() { return anonymousId; }
    public String getEventName() { return eventName; }
    public String getLandingPage() { return landingPage; }
    public String getTool() { return tool; }
    public String getCtaLabel() { return ctaLabel; }
    public String getCtaDestination() { return ctaDestination; }
    public String getReferrer() { return referrer; }
    public String getUtmSource() { return utmSource; }
    public String getUtmMedium() { return utmMedium; }
    public String getUtmCampaign() { return utmCampaign; }
    public String getUtmContent() { return utmContent; }
    public String getUtmTerm() { return utmTerm; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public void setLandingPage(String landingPage) { this.landingPage = landingPage; }
    public void setTool(String tool) { this.tool = tool; }
    public void setCtaLabel(String ctaLabel) { this.ctaLabel = ctaLabel; }
    public void setCtaDestination(String ctaDestination) { this.ctaDestination = ctaDestination; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
    public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
    public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    public void setUtmContent(String utmContent) { this.utmContent = utmContent; }
    public void setUtmTerm(String utmTerm) { this.utmTerm = utmTerm; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
