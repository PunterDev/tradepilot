package com.tradepilot.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTests {

    @Test
    void sharedRestTemplateRequestFactoryUsesConfiguredTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = RestClientConfig.requestFactory(
                Duration.ofSeconds(4),
                Duration.ofSeconds(17)
        );
        DirectFieldAccessor accessor = new DirectFieldAccessor(requestFactory);

        assertThat(accessor.getPropertyValue("connectTimeout")).isEqualTo(4000);
        assertThat(accessor.getPropertyValue("readTimeout")).isEqualTo(17000);
    }
}
