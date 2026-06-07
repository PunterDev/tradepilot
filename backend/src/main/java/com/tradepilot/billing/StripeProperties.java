package com.tradepilot.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {
    private String secretKey = "";
    private String webhookSecret = "";
    private String proPriceId = "";
    private String proYearlyPriceId = "";
}
