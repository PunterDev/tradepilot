package com.tradepilot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorClientIpTests {

    @Test
    void ignoresForwardedForWhenForwardedHeadersAreNotTrusted() {
        RateLimitInterceptor interceptor = interceptor(false, "");
        MockHttpServletRequest request = request("198.51.100.10", "203.0.113.77");

        assertThat(ipAddress(interceptor, request)).isEqualTo("198.51.100.10");
    }

    @Test
    void usesForwardedForOnlyWhenImmediateProxyIsTrusted() {
        RateLimitInterceptor interceptor = interceptor(true, "198.51.100.10");
        MockHttpServletRequest request = request("198.51.100.10", "203.0.113.77, 198.51.100.10");

        assertThat(ipAddress(interceptor, request)).isEqualTo("203.0.113.77");
    }

    @Test
    void ignoresForwardedForWhenImmediateProxyIsUntrusted() {
        RateLimitInterceptor interceptor = interceptor(true, "198.51.100.11");
        MockHttpServletRequest request = request("198.51.100.10", "203.0.113.77");

        assertThat(ipAddress(interceptor, request)).isEqualTo("198.51.100.10");
    }

    private static RateLimitInterceptor interceptor(boolean trustForwardedHeaders, String trustedProxies) {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new InMemoryRateLimiter(), new SecurityErrorResponseWriter(new ObjectMapper()));
        ReflectionTestUtils.setField(interceptor, "trustForwardedHeaders", trustForwardedHeaders);
        ReflectionTestUtils.setField(interceptor, "trustedProxies", trustedProxies);
        return interceptor;
    }

    private static MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    private static String ipAddress(RateLimitInterceptor interceptor, MockHttpServletRequest request) {
        return ReflectionTestUtils.invokeMethod(interceptor, "ipAddress", request);
    }
}
