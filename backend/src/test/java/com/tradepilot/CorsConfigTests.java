package com.tradepilot;

import com.tradepilot.config.CorsConfig;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTests {

    @Test
    void accountRiskPatchPreflightIsAllowedFromLocalVite() throws ServletException, IOException {
        CorsFilter filter = new CorsConfig().corsFilter(
                "http://localhost:5173,https://www.tradepilotcoach.com,https://tradepilotcoach.com",
                new MockEnvironment()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/accounts/6/risk");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("Preflight should be handled by CORS filter");
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("PATCH");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    void productionProfileRejectsLocalhostCorsOrigins() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> new CorsConfig().corsFilter("https://tradepilot.app,http://localhost:5173", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production CORS cannot use localhost origins");
    }

    @Test
    void wildcardCorsOriginIsRejectedBecauseCredentialsAreAllowed() {
        assertThatThrownBy(() -> new CorsConfig().corsFilter("*", new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard origin cannot be used");
    }

    @Test
    void productionRequiresCanonicalFrontendOrigin() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> new CorsConfig().corsFilter("https://tradepilotcoach.com", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https://www.tradepilotcoach.com");
    }
}
