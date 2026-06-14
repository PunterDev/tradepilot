package com.tradepilot.billing;

import lombok.Getter;

@Getter
public class FeatureAccessException extends RuntimeException {
    private final Feature feature;
    private final String requiredPlan;

    public FeatureAccessException(Feature feature, String message) {
        super(message);
        this.feature = feature;
        this.requiredPlan = "PRO";
    }
}
