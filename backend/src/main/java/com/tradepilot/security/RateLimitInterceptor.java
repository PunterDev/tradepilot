package com.tradepilot.security;

import com.tradepilot.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final RateLimiter rateLimiter;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    @Value("${app.rate-limit.auth-login.max-requests:8}")
    private int authLoginMax;
    @Value("${app.rate-limit.auth-login.window-seconds:60}")
    private long authLoginWindowSeconds;
    @Value("${app.rate-limit.auth-register.max-requests:5}")
    private int authRegisterMax;
    @Value("${app.rate-limit.auth-register.window-seconds:300}")
    private long authRegisterWindowSeconds;
    @Value("${app.rate-limit.auth-email.max-requests:5}")
    private int authEmailMax;
    @Value("${app.rate-limit.auth-email.window-seconds:300}")
    private long authEmailWindowSeconds;
    @Value("${app.rate-limit.contact.max-requests:3}")
    private int contactMax;
    @Value("${app.rate-limit.contact.window-seconds:300}")
    private long contactWindowSeconds;
    @Value("${app.rate-limit.seo-events.max-requests:120}")
    private int seoEventsMax;
    @Value("${app.rate-limit.seo-events.window-seconds:60}")
    private long seoEventsWindowSeconds;
    @Value("${app.rate-limit.seo-events.max-content-bytes:16384}")
    private long seoEventsMaxContentBytes;
    @Value("${app.rate-limit.ai-generation.max-requests:12}")
    private int aiGenerationMax;
    @Value("${app.rate-limit.ai-generation.window-seconds:300}")
    private long aiGenerationWindowSeconds;
    @Value("${app.rate-limit.csv-import.max-requests:6}")
    private int csvImportMax;
    @Value("${app.rate-limit.csv-import.window-seconds:300}")
    private long csvImportWindowSeconds;
    @Value("${app.rate-limit.billing-checkout.max-requests:10}")
    private int billingCheckoutMax;
    @Value("${app.rate-limit.billing-checkout.window-seconds:300}")
    private long billingCheckoutWindowSeconds;
    @Value("${app.rate-limit.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;
    @Value("${app.rate-limit.trusted-proxies:}")
    private String trustedProxies;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (isOversizedSeoEventsRequest(request)) {
            securityErrorResponseWriter.write(response, HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Request body is too large.", request.getRequestURI());
            return false;
        }
        RateLimitRule rule = ruleFor(request);
        if (rule == null) {
            return true;
        }
        String subject = rule.authenticated ? authenticatedSubject() : ipAddress(request);
        String key = "%s:%s".formatted(rule.name, subject);
        RateLimitResult result = rateLimiter.check(key, rule.maxRequests, rule.windowSeconds);
        if (result.allowed()) {
            return true;
        }
        log.warn("Rate limit blocked request. rule={}, subject={}, path={}, retryAfterSeconds={}", rule.name, safeSubject(rule.authenticated, subject), request.getRequestURI(), result.retryAfterSeconds());
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        securityErrorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many requests. Please wait and try again.", request.getRequestURI());
        return false;
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = normalizedApiPath(request.getRequestURI());
        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(path)) {
            return new RateLimitRule("auth-login", authLoginMax, authLoginWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/register".equals(path)) {
            return new RateLimitRule("auth-register", authRegisterMax, authRegisterWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && ("/api/auth/forgot-password".equals(path) || "/api/auth/reset-password".equals(path) || "/api/auth/resend-verification".equals(path) || "/api/auth/verify-email".equals(path))) {
            return new RateLimitRule("auth-email", authEmailMax, authEmailWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && "/api/contact".equals(path)) {
            return new RateLimitRule("contact", contactMax, contactWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && "/api/seo/events".equals(path)) {
            return new RateLimitRule("seo-events", seoEventsMax, seoEventsWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && ("/api/trades/import".equals(path) || "/api/trades/import/preview".equals(path) || "/api/trades/import/csv".equals(path) || "/api/trades/repair-tradovate-directions".equals(path))) {
            return new RateLimitRule("csv-import", csvImportMax, csvImportWindowSeconds, true);
        }
        if (HttpMethod.POST.matches(method) && ("/api/billing/create-checkout-session".equals(path) || "/api/billing/create-credit-pack-checkout-session".equals(path) || "/api/billing/create-portal-session".equals(path))) {
            return new RateLimitRule("billing-checkout", billingCheckoutMax, billingCheckoutWindowSeconds, true);
        }
        if (isAiGenerationRequest(method, path)) {
            return new RateLimitRule("ai-generation:" + aiFeature(path), aiGenerationMax, aiGenerationWindowSeconds, true);
        }
        return null;
    }

    private boolean isOversizedSeoEventsRequest(HttpServletRequest request) {
        String path = normalizedApiPath(request.getRequestURI());
        long contentLength = request.getContentLengthLong();
        return HttpMethod.POST.matches(request.getMethod())
                && "/api/seo/events".equals(path)
                && seoEventsMaxContentBytes > 0
                && contentLength > seoEventsMaxContentBytes;
    }

    private boolean isAiGenerationRequest(String method, String path) {
        return (HttpMethod.POST.matches(method) && ("/api/ai/trade-review".equals(path) || "/api/ai/trade-review/async".equals(path) || "/api/ai/weekly-review".equals(path) || "/api/ai/chart-review".equals(path) || "/api/ai/trade-analysis/async".equals(path) || "/api/coach/daily/generate".equals(path) || "/api/coach/weekly/generate".equals(path) || "/api/sessions/review/generate".equals(path) || "/api/ai/leak-detector/regenerate".equals(path) || "/api/ai/performance-timeline/regenerate".equals(path) || isPlaybookAiGenerationPath(path) || isTradeReplayGenerationPath(path))) || (HttpMethod.GET.matches(method) && ("/api/ai/trading-dna".equals(path) || "/api/ai/leak-detector".equals(path)));
    }

    private boolean isTradeReplayGenerationPath(String path) {
        return path != null && path.matches("^/api/trades/\\d+/replay/(generate|regenerate)$");
    }

    private boolean isPlaybookAiGenerationPath(String path) {
        return path != null && (path.equals("/api/ai/playbook/draft-from-description")
                || path.equals("/api/ai/playbook/draft-from-trades")
                || path.matches("^/api/ai/playbook/\\d+/regenerate$"));
    }

    private String normalizedApiPath(String path) {
        if (path != null && (path.equals("/api/v1") || path.startsWith("/api/v1/"))) {
            return "/api" + path.substring("/api/v1".length());
        }
        return path;
    }

    private String aiFeature(String path) {
        return path.replace("/api/", "").replace('/', '-');
    }

    private String authenticatedSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return "user-" + user.getId();
        }
        return "anonymous";
    }

    private String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (trustForwardedHeaders && trustedProxy(request.getRemoteAddr()) && StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private boolean trustedProxy(String remoteAddr) {
        if (!StringUtils.hasText(remoteAddr)) {
            return false;
        }
        if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return true;
        }
        if (remoteAddr.startsWith("10.") || remoteAddr.startsWith("192.168.") || isPrivate172(remoteAddr)) {
            return true;
        }
        return configuredTrustedProxies().contains(remoteAddr);
    }

    private boolean isPrivate172(String remoteAddr) {
        if (!remoteAddr.startsWith("172.")) {
            return false;
        }
        String[] parts = remoteAddr.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Set<String> configuredTrustedProxies() {
        if (!StringUtils.hasText(trustedProxies)) {
            return Set.of();
        }
        return Stream.of(trustedProxies.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String safeSubject(boolean authenticated, String subject) {
        return authenticated ? subject : "ip-hash-" + Integer.toHexString(subject.hashCode());
    }


    private record RateLimitRule(String name, int maxRequests, long windowSeconds, boolean authenticated) {
    }

    @java.lang.SuppressWarnings("all")
    public RateLimitInterceptor(final RateLimiter rateLimiter, final SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.rateLimiter = rateLimiter;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }
}
