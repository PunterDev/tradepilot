package com.tradepilot.observability;

import com.tradepilot.security.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ObservabilityWebConfig implements WebMvcConfigurer {
    private final RequestMetricsInterceptor requestMetricsInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }

    @java.lang.SuppressWarnings("all")
    public ObservabilityWebConfig(final RequestMetricsInterceptor requestMetricsInterceptor, final RateLimitInterceptor rateLimitInterceptor) {
        this.requestMetricsInterceptor = requestMetricsInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }
}
