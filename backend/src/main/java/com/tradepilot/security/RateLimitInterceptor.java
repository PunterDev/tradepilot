package com.tradepilot.security;

import com.tradepilot.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {
    private final InMemoryRateLimiter rateLimiter;

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
    @Value("${app.rate-limit.ai-generation.max-requests:12}")
    private int aiGenerationMax;
    @Value("${app.rate-limit.ai-generation.window-seconds:300}")
    private long aiGenerationWindowSeconds;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        RateLimitRule rule = ruleFor(request);
        if (rule == null) {
            return true;
        }

        String subject = rule.authenticated ? authenticatedSubject() : ipAddress(request);
        String key = "%s:%s".formatted(rule.name, subject);
        InMemoryRateLimiter.RateLimitResult result =
                rateLimiter.check(key, rule.maxRequests, rule.windowSeconds);
        if (result.allowed()) {
            return true;
        }

        log.warn("Rate limit blocked request. rule={}, subject={}, path={}, retryAfterSeconds={}",
                rule.name, safeSubject(rule.authenticated, subject), request.getRequestURI(), result.retryAfterSeconds());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"Too many requests. Please wait and try again.\"}");
        return false;
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(path)) {
            return new RateLimitRule("auth-login", authLoginMax, authLoginWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/register".equals(path)) {
            return new RateLimitRule("auth-register", authRegisterMax, authRegisterWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method)
                && ("/api/auth/forgot-password".equals(path) || "/api/auth/resend-verification".equals(path))) {
            return new RateLimitRule("auth-email", authEmailMax, authEmailWindowSeconds, false);
        }
        if (HttpMethod.POST.matches(method) && "/api/contact".equals(path)) {
            return new RateLimitRule("contact", contactMax, contactWindowSeconds, false);
        }
        if (isAiGenerationRequest(method, path)) {
            return new RateLimitRule("ai-generation:" + aiFeature(path), aiGenerationMax, aiGenerationWindowSeconds, true);
        }
        return null;
    }

    private boolean isAiGenerationRequest(String method, String path) {
        return (HttpMethod.POST.matches(method)
                && ("/api/ai/trade-review".equals(path)
                || "/api/ai/trade-review/async".equals(path)
                || "/api/ai/weekly-review".equals(path)
                || "/api/ai/chart-review".equals(path)
                || "/api/ai/trade-analysis/async".equals(path)
                || "/api/coach/daily/generate".equals(path)
                || "/api/coach/weekly/generate".equals(path)
                || "/api/sessions/review/generate".equals(path)
                || "/api/ai/leak-detector/regenerate".equals(path)))
                || (HttpMethod.GET.matches(method)
                && ("/api/ai/trading-dna".equals(path)
                || "/api/ai/leak-detector".equals(path)));
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
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String safeSubject(boolean authenticated, String subject) {
        return authenticated ? subject : "ip-hash-" + Integer.toHexString(subject.hashCode());
    }

    private record RateLimitRule(String name, int maxRequests, long windowSeconds, boolean authenticated) {
    }
}
