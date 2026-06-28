package com.tradepilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {
    private String apiKey;
    private String model = "gpt-4.1-mini";

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

    @java.lang.SuppressWarnings("all")
    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    @java.lang.SuppressWarnings("all")
    public void setModel(final String model) {
        this.model = model;
    }

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
        return "OpenAiProperties(apiKey=" + this.getApiKey() + ", model=" + this.getModel() + ")";
    }
}
