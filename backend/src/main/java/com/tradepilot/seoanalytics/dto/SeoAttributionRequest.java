package com.tradepilot.seoanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class SeoAttributionRequest {
    @Size(max = 100)
    private String anonymousVisitorId;

    @JsonAlias("anonymousId")
    @Size(max = 100)
    private String anonymousId;

    @Size(max = 256)
    private String landingPage;

    @Size(max = 500)
    private String initialReferrer;

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
    private String capturedAt;

    private final Set<String> unknownFields = new HashSet<>();

    public SeoAttributionRequest() {
    }

    public SeoAttributionRequest(String anonymousId, String landingPage, String initialReferrer, String utmSource, String utmMedium, String utmCampaign, String utmContent, String utmTerm, String capturedAt) {
        this.anonymousId = anonymousId;
        this.landingPage = landingPage;
        this.initialReferrer = initialReferrer;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.utmContent = utmContent;
        this.utmTerm = utmTerm;
        this.capturedAt = capturedAt;
    }

    @JsonAnySetter
    void captureUnknownField(String name, Object value) {
        unknownFields.add(name);
    }

    @AssertTrue(message = "Unsupported SEO attribution field")
    public boolean isKnownFieldOnly() {
        return unknownFields.isEmpty();
    }

    public String getAnonymousVisitorId() { return anonymousVisitorId; }
    public String getAnonymousId() { return anonymousId; }
    public String getLandingPage() { return landingPage; }
    public String getInitialReferrer() { return initialReferrer; }
    public String getUtmSource() { return utmSource; }
    public String getUtmMedium() { return utmMedium; }
    public String getUtmCampaign() { return utmCampaign; }
    public String getUtmContent() { return utmContent; }
    public String getUtmTerm() { return utmTerm; }
    public String getCapturedAt() { return capturedAt; }

    public void setAnonymousVisitorId(String anonymousVisitorId) { this.anonymousVisitorId = anonymousVisitorId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
    public void setLandingPage(String landingPage) { this.landingPage = landingPage; }
    public void setInitialReferrer(String initialReferrer) { this.initialReferrer = initialReferrer; }
    public void setUtmSource(String utmSource) { this.utmSource = utmSource; }
    public void setUtmMedium(String utmMedium) { this.utmMedium = utmMedium; }
    public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }
    public void setUtmContent(String utmContent) { this.utmContent = utmContent; }
    public void setUtmTerm(String utmTerm) { this.utmTerm = utmTerm; }
    public void setCapturedAt(String capturedAt) { this.capturedAt = capturedAt; }
}
