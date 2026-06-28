package com.tradepilot.billing;

public class FeatureAccessException extends RuntimeException {
    private final Feature feature;
    private final String requiredPlan;

    public FeatureAccessException(Feature feature, String message) {
        super(message);
        this.feature = feature;
        this.requiredPlan = "PRO";
    }

    @java.lang.SuppressWarnings("all")
    public Feature getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    public String getRequiredPlan() {
        return this.requiredPlan;
    }
}
