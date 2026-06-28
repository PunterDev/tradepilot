package com.tradepilot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiVersioningFilter extends OncePerRequestFilter {

    public static final String CURRENT_PRODUCT_VERSION = "1.0";
    public static final String CANONICAL_PREFIX = "/api/v1";
    public static final String LEGACY_PREFIX = "/api";
    public static final String VERSION_HEADER = "X-TradePilot-API-Version";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!isApiPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader(VERSION_HEADER, CURRENT_PRODUCT_VERSION);

        if (isCanonicalApiPath(path)) {
            filterChain.doFilter(new ApiV1RequestWrapper(request), response);
            return;
        }

        if (isLegacyApiPath(path) && !isStableExternalCallback(path)) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Link", "</api/v1>; rel=\"successor-version\"");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isApiPath(String path) {
        return path != null && (path.equals(LEGACY_PREFIX) || path.startsWith(LEGACY_PREFIX + "/"));
    }

    private boolean isCanonicalApiPath(String path) {
        return path != null && (path.equals(CANONICAL_PREFIX) || path.startsWith(CANONICAL_PREFIX + "/"));
    }

    private boolean isLegacyApiPath(String path) {
        return path != null && path.startsWith(LEGACY_PREFIX + "/") && !isCanonicalApiPath(path);
    }

    private boolean isStableExternalCallback(String path) {
        return "/api/stripe/webhook".equals(path);
    }

    private static class ApiV1RequestWrapper extends HttpServletRequestWrapper {
        private final String requestUri;
        private final String servletPath;

        ApiV1RequestWrapper(HttpServletRequest request) {
            super(request);
            String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
            String originalUri = request.getRequestURI();
            String pathWithoutContext = originalUri.substring(contextPath.length());
            String normalizedPath = LEGACY_PREFIX + pathWithoutContext.substring(CANONICAL_PREFIX.length());
            this.requestUri = contextPath + normalizedPath;
            this.servletPath = normalizedPath;
        }

        @Override
        public String getRequestURI() {
            return requestUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            if (("http".equals(getScheme()) && getServerPort() != 80)
                    || ("https".equals(getScheme()) && getServerPort() != 443)) {
                url.append(':').append(getServerPort());
            }
            url.append(requestUri);
            return url;
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
