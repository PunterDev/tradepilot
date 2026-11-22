package com.tradepilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {
    private String apiKey;
    private String model = "gpt-6-luna";
    private String reasoningEffort = "low";
    private int maxOutputTokens = 4096;
    private boolean legacyChatCompletionsEnabled;
    private double legacyTemperature = 0.2;
    private java.math.BigDecimal inputCostPerMillionTokens = new java.math.BigDecimal("0.10");
    private java.math.BigDecimal cachedInputCostPerMillionTokens = new java.math.BigDecimal("0.01");
    private java.math.BigDecimal outputCostPerMillionTokens = new java.math.BigDecimal("0.50");

    @java.lang.SuppressWarnings("all")
    public OpenAiProperties() {
    }

    @java.lang.SuppressWarnings("all")
    public String getApiKey() {
        return this.apiKey;
    }

    @java.lang.SuppressWarnings("all")
    public String getModel() {
        return this.model;
    }

    public String getReasoningEffort() { return reasoningEffort; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public boolean isLegacyChatCompletionsEnabled() { return legacyChatCompletionsEnabled; }
    public double getLegacyTemperature() { return legacyTemperature; }
    public java.math.BigDecimal getInputCostPerMillionTokens() { return inputCostPerMillionTokens; }
    public java.math.BigDecimal getCachedInputCostPerMillionTokens() { return cachedInputCostPerMillionTokens; }
    public java.math.BigDecimal getOutputCostPerMillionTokens() { return outputCostPerMillionTokens; }

    @java.lang.SuppressWarnings("all")
    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    @java.lang.SuppressWarnings("all")
    public void setModel(final String model) {
        this.model = model;
    }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public void setLegacyChatCompletionsEnabled(boolean legacyChatCompletionsEnabled) { this.legacyChatCompletionsEnabled = legacyChatCompletionsEnabled; }
    public void setLegacyTemperature(double legacyTemperature) { this.legacyTemperature = legacyTemperature; }
    public void setInputCostPerMillionTokens(java.math.BigDecimal inputCostPerMillionTokens) { this.inputCostPerMillionTokens = inputCostPerMillionTokens; }
    public void setCachedInputCostPerMillionTokens(java.math.BigDecimal cachedInputCostPerMillionTokens) { this.cachedInputCostPerMillionTokens = cachedInputCostPerMillionTokens; }
    public void setOutputCostPerMillionTokens(java.math.BigDecimal outputCostPerMillionTokens) { this.outputCostPerMillionTokens = outputCostPerMillionTokens; }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OpenAiProperties)) return false;
        final OpenAiProperties other = (OpenAiProperties) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$apiKey = this.getApiKey();
        final java.lang.Object other$apiKey = other.getApiKey();
        if (this$apiKey == null ? other$apiKey != null : !this$apiKey.equals(other$apiKey)) return false;
        final java.lang.Object this$model = this.getModel();
        final java.lang.Object other$model = other.getModel();
        if (this$model == null ? other$model != null : !this$model.equals(other$model)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OpenAiProperties;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $apiKey = this.getApiKey();
        result = result * PRIME + ($apiKey == null ? 43 : $apiKey.hashCode());
        final java.lang.Object $model = this.getModel();
        result = result * PRIME + ($model == null ? 43 : $model.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "OpenAiProperties(apiKeyConfigured=" + (this.getApiKey() != null && !this.getApiKey().isBlank()) + ", model=" + this.getModel() + ")";
    }
}
