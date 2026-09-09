package com.tradepilot.seoanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class SeoAcquisitionEventRequest {
    @JsonAlias("event")
    @NotBlank
    @Size(max = 64)
    private String eventName;

    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "eventId must be a UUID")
    private String eventId;

    @Size(max = 100)
    private String anonymousVisitorId;

    @JsonAlias("anonymousId")
    @Size(max = 100)
    private String anonymousId;

    @NotBlank
    @Size(max = 256)
    private String landingPage;

    @Size(max = 100)
    private String tool;

    @Size(max = 120)
    private String ctaLabel;

    @Size(max = 256)
    private String ctaDestination;

    @Size(max = 500)
    private String referrer;

    @Size(max = 100)
    private String utmSource;

    @Size(max = 100)
    private String utmMedium;

    @Size(max = 150)
    private String utmCampaign;

    @Size(max = 150)
    private String utmContent;

    @Size(max = 150)
    private String utmTerm;

    @Size(max = 40)
    private String timestamp;

    private final Set<String> unknownFields = new HashSet<>();

    @JsonAnySetter
    void captureUnknownField(String name, Object value) {
        unknownFields.add(name);
    }

    @AssertTrue(message = "Unsupported SEO analytics field")
    public boolean isKnownFieldOnly() {
        return unknownFields.isEmpty();
    }

    public String getEventName() { return eventName; }
    public String getEventId() { return eventId; }
    public String getAnonymousVisitorId() { return anonymousVisitorId; }
    public String getAnonymousId() { return anonymousId; }
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
    public String getTimestamp() { return timestamp; }

    public void setEventName(String eventName) { this.eventName = eventName; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setAnonymousVisitorId(String anonymousVisitorId) { this.anonymousVisitorId = anonymousVisitorId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
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
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
