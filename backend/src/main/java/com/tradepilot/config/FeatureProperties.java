package com.tradepilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tradepilot.features")
public class FeatureProperties {
    private boolean v2Enabled = false;
    private boolean v3Enabled = false;
    private String accountSurvivalRolloutMode = "disabled";

    public boolean isV2Enabled() {
        return v2Enabled;
    }

    public void setV2Enabled(boolean v2Enabled) {
        this.v2Enabled = v2Enabled;
    }

    public boolean isV3Enabled() {
        return v3Enabled;
    }

    public void setV3Enabled(boolean v3Enabled) {
        this.v3Enabled = v3Enabled;
    }

    public String getAccountSurvivalRolloutMode() {
        return accountSurvivalRolloutMode;
    }

    public void setAccountSurvivalRolloutMode(String accountSurvivalRolloutMode) {
        this.accountSurvivalRolloutMode = accountSurvivalRolloutMode;
    }
}
