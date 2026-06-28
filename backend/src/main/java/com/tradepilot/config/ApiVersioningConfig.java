package com.tradepilot.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApiVersioningConfig {

    @Bean
    public FilterRegistrationBean<ApiVersioningFilter> apiVersioningFilterRegistration() {
        FilterRegistrationBean<ApiVersioningFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiVersioningFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
