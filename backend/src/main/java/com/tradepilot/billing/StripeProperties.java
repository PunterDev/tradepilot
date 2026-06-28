package com.tradepilot.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private String secretKey = "";
    private String webhookSecret = "";
    private String proPriceId = "";
    private String proYearlyPriceId = "";

    @java.lang.SuppressWarnings("all")
    public String getSecretKey() {
        return this.secretKey;
    }

    @java.lang.SuppressWarnings("all")
    public String getWebhookSecret() {
        return this.webhookSecret;
    }

    @java.lang.SuppressWarnings("all")
    public String getProPriceId() {
        return this.proPriceId;
    }

    @java.lang.SuppressWarnings("all")
    public String getProYearlyPriceId() {
        return this.proYearlyPriceId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    @java.lang.SuppressWarnings("all")
    public void setWebhookSecret(final String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @java.lang.SuppressWarnings("all")
    public void setProPriceId(final String proPriceId) {
        this.proPriceId = proPriceId;
    }

    @java.lang.SuppressWarnings("all")
    public void setProYearlyPriceId(final String proYearlyPriceId) {
        this.proYearlyPriceId = proYearlyPriceId;
    }
}
