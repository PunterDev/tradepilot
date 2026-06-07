package com.tradepilot.analytics.dto;

public class BehavioralInsightDto {
    private String type;
    private String severity;
    private String title;
    private String message;

    public BehavioralInsightDto() {
    }

    public BehavioralInsightDto(String type, String severity, String title, String message) {
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
    }

    public static BehavioralInsightDto of(String type, String severity, String title, String message) {
        return new BehavioralInsightDto(type, severity, title, message);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
