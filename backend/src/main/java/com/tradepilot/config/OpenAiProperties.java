package com.tradepilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {
    private String apiKey;
    private String model = "gpt-4.1-mini";
}