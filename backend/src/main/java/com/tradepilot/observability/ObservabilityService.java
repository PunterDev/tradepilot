package com.tradepilot.observability;

import com.tradepilot.billing.AiCreditPurchaseRepository;
import com.tradepilot.observability.dto.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ObservabilityService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObservabilityService.class);
    private static final long SLOW_WARNING_MS = 500;
    private static final long SLOW_CRITICAL_MS = 2000;
    private static final Duration SUMMARY_CACHE_TTL = Duration.ofSeconds(30);
    private final RequestMetricRepository requestMetricRepository;
    private final SlowRequestEventRepository slowRequestEventRepository;
    private final AiRequestMetricRepository aiRequestMetricRepository;
    private final CacheMetricRepository cacheMetricRepository;
    private final EmailDeliveryEventRepository emailDeliveryEventRepository;
    private final StripeEventMetricRepository stripeEventMetricRepository;
    private final QueryPerformanceEventRepository queryPerformanceEventRepository;
    private final SystemAlertRepository systemAlertRepository;
    private final AiCreditPurchaseRepository aiCreditPurchaseRepository;
    private final CacheMetricRecorder cacheMetricRecorder;
    private final QueryPerformanceEventRecorder queryPerformanceEventRecorder;
    @Value("${app.observability.ai-cost-per-1k-tokens:0.0006}")
    private BigDecimal aiCostPerThousandTokens;
    @Value("${app.observability.ai-cost-daily-threshold:10.00}")
    private BigDecimal aiCostDailyThreshold;
    @Value("${app.observability.slow-query-threshold-ms:250}")
    private long slowQueryThresholdMs;
    private volatile ObservabilitySummaryResponse cachedSummary;
    private volatile Instant cachedSummaryExpiresAt = Instant.EPOCH;
    private final Map<String, CachedSummary> summaryCache = new ConcurrentHashMap<>();

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequest(String endpoint, String method, int status, long durationMs, Long userId) {
        recordRequest(endpoint, method, status, durationMs, userId, MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequest(String endpoint, String method, int status, long durationMs, Long userId, String requestId) {
        LocalDateTime now = LocalDateTime.now();
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint(limit(endpoint, 220))
                .method(limit(method, 16))
                .status(status)
                .durationMs(durationMs)
                .userId(userId)
                .requestId(limit(requestId, 80))
                .errorCode(status >= 400 ? statusErrorCode(status) : null)
                .createdAt(now)
                .build());
        if (durationMs > SLOW_WARNING_MS) {
            slowRequestEventRepository.save(SlowRequestEvent.builder().endpoint(limit(endpoint, 220)).durationMs(durationMs).status(status).userId(userId).severity(durationMs > SLOW_CRITICAL_MS ? "CRITICAL" : "WARNING").createdAt(now).build());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAiCall(Long userId, String feature, String model, int promptTokens, int completionTokens, int totalTokens, long durationMs, boolean success) {
        aiRequestMetricRepository.save(AiRequestMetric.builder().userId(userId).feature(limit(feature, 64)).model(limit(model, 80)).promptTokens(promptTokens).completionTokens(completionTokens).totalTokens(totalTokens).estimatedCost(estimateCost(totalTokens)).durationMs(durationMs).success(success).createdAt(LocalDateTime.now()).build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCacheEvent(String cacheName, boolean hit) {
        cacheMetricRepository.save(CacheMetric.builder().cacheName(limit(cacheName, 120)).hit(hit).createdAt(LocalDateTime.now()).build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEmail(String recipient, String template, boolean success, long durationMs, String errorMessage) {
        emailDeliveryEventRepository.save(EmailDeliveryEvent.builder().recipient(limit(recipient, 240)).template(limit(template, 120)).success(success).durationMs(durationMs).errorMessage(limit(errorMessage, 700)).createdAt(LocalDateTime.now()).build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStripeEvent(String eventType, boolean success, long durationMs, String errorMessage) {
        stripeEventMetricRepository.save(StripeEventMetric.builder().eventType(limit(eventType, 120)).success(success).durationMs(durationMs).errorMessage(limit(errorMessage, 700)).createdAt(LocalDateTime.now()).build());
    }

    public <T> T timeQuery(String queryName, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        try {
            return supplier.get();
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            if (durationMs > slowQueryThresholdMs) {
                recordSlowQuery(queryName, durationMs);
            }
        }
    }

    public void recordSlowQuery(String queryName, long durationMs) {
        try {
            queryPerformanceEventRecorder.record(queryName, durationMs);
        } catch (RuntimeException ex) {
            log.warn("Query performance event persistence failed. queryName={}, durationMs={}, reason={}", limit(queryName, 140), durationMs, limit(ex.getMessage(), 300));
        }
    }

    public ObservabilitySummaryResponse summary() {
        return summary(ObservabilityDateRange.from("TODAY", null, null));
    }

    public ObservabilitySummaryResponse summary(ObservabilityDateRange range) {
        String cacheKey = range.cacheKey();
        Instant now = Instant.now();
        CachedSummary cached = summaryCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            cacheMetricRecorder.record("observability-summary", true);
            log.trace("Observability summary cache hit. range={}", cacheKey);
            return cached.summary();
        }
        synchronized (this) {
            now = Instant.now();
            cached = summaryCache.get(cacheKey);
            if (cached != null && cached.expiresAt().isAfter(now)) {
                cacheMetricRecorder.record("observability-summary", true);
                log.trace("Observability summary cache hit after lock. range={}", cacheKey);
                return cached.summary();
            }
            cacheMetricRecorder.record("observability-summary", false);
            log.info("Observability summary requested. range={}, start={}, end={}", cacheKey, range.start(), range.end());
            ObservabilitySummaryResponse summary = buildSummary(range);
            summaryCache.put(cacheKey, new CachedSummary(summary, now.plus(SUMMARY_CACHE_TTL)));
            log.info("Observability summary completed. range={}, requests={}, errorRate={}, p95Ms={}", cacheKey, summary.getRequestsToday(), summary.getErrorRate(), summary.getP95ResponseTime());
            return summary;
        }
    }

    private ObservabilitySummaryResponse buildSummary(ObservabilityDateRange range) {
        LocalDateTime start = range.start();
        LocalDateTime end = range.end();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime month = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long requestCount = requestMetricRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long errorCount = requestMetricRepository.countErrorsBetween(start, end);
        List<Object[]> slowest = requestMetricRepository.slowestEndpointsBetween(start, end, PageRequest.of(0, 8));
        List<RequestMetric> recentRequests = requestMetricRepository.recentBetween(start, end, PageRequest.of(0, 5000));
        List<RequestMetric> recentErrors = requestMetricRepository.recentErrorsBetween(start, end, PageRequest.of(0, 500));
        BigDecimal aiCostRange = nullSafe(aiRequestMetricRepository.totalCostBetween(start, end));
        BigDecimal aiCostMonth = nullSafe(aiRequestMetricRepository.totalCostSince(month));
        List<Object[]> aiCosts = aiRequestMetricRepository.featureCostsBetween(start, end, PageRequest.of(0, 8));
        List<Object[]> models = aiRequestMetricRepository.modelUsageBetween(start, end, PageRequest.of(0, 1));
        long cacheHits = cacheMetricRepository.hitsBetween(start, end);
        long cacheMisses = cacheMetricRepository.missesBetween(start, end);
        long emailSuccesses = emailDeliveryEventRepository.successesBetween(start, end);
        long emailFailures = emailDeliveryEventRepository.failuresBetween(start, end);
        long stripeSuccesses = stripeEventMetricRepository.successesBetween(start, end);
        long stripeFailures = stripeEventMetricRepository.failuresBetween(start, end);
        long aiRequestCount = aiRequestMetricRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        long aiFailures = aiRequestMetricRepository.failuresBetween(start, end);
        List<StripeEventMetric> recentStripeEvents = stripeEventMetricRepository.findBetween(start, end, PageRequest.of(0, 8));
        List<StripeEventMetric> stripeFailuresForSummary = stripeEventMetricRepository.findFailuresBetween(start, end, PageRequest.of(0, 1));
        List<EmailDeliveryEvent> emailFailuresForSummary = emailDeliveryEventRepository.findFailuresBetween(start, end, PageRequest.of(0, 6));
        List<AiRequestMetric> aiFailuresForSummary = aiRequestMetricRepository.findFailuresBetween(start, end, PageRequest.of(0, 1));
        long emailAttempts = emailSuccesses + emailFailures;
        long stripeAttempts = stripeSuccesses + stripeFailures;
        double errorRate = rate(errorCount, requestCount);
        double cacheHitRate = rate(cacheHits, cacheHits + cacheMisses);
        double emailSuccessRate = rate(emailSuccesses, emailAttempts);
        double stripeSuccessRate = rate(stripeSuccesses, stripeAttempts);
        Map<String, String> latestRequestIdByEndpoint = latestRequestIdByEndpoint(recentRequests);
        long aiGenerationP95 = aiP95Duration(start, end, aiRequestCount);
        List<LatencyCategoryDto> latencyCategories = latencyCategories(start, end, aiRequestCount, aiGenerationP95);
        long standardP95 = p95ForCategory(latencyCategories, "Standard API");
        long adminP95 = p95ForCategory(latencyCategories, "Admin/Reporting");
        ObservabilitySummaryResponse response = new ObservabilitySummaryResponse();
        response.setRequestsToday(requestCount);
        response.setAvgResponseTime(requestMetricRepository.averageDurationBetween(start, end));
        response.setP95ResponseTime(p95Duration(start, end, requestCount));
        response.setSlowestEndpoint(slowest.isEmpty() ? "No traffic yet" : String.valueOf(slowest.get(0)[0]));
        response.setErrorRate(errorRate);
        response.setErrorRateSeverity(errorRateSeverity(errorRate));
        response.setAiCallsToday(aiRequestCount);
        response.setAiFailuresToday(aiFailures);
        response.setAiTokensToday(aiRequestMetricRepository.totalTokensBetween(start, end));
        response.setAiCostToday(aiCostRange);
        response.setAiCostMonth(aiCostMonth);
        response.setAiAvgResponseTime(aiRequestMetricRepository.averageDurationBetween(start, end));
        response.setAiGenerationP95ResponseTime(aiGenerationP95);
        response.setAiGenerationP95Severity(aiGenerationSeverity(aiGenerationP95));
        response.setMostExpensiveFeature(aiCosts.isEmpty() ? "No AI calls yet" : String.valueOf(aiCosts.get(0)[0]));
        response.setMostUsedModel(models.isEmpty() ? "No model usage yet" : String.valueOf(models.get(0)[0]));
        response.setCacheHits(cacheHits);
        response.setCacheMisses(cacheMisses);
        response.setCacheHitRate(cacheHitRate);
        response.setCacheHitRateSeverity(cacheHitSeverity(cacheHitRate, cacheHits + cacheMisses));
        response.setEmailsSent(emailSuccesses);
        response.setEmailsFailed(emailFailures);
        response.setEmailSuccessRate(emailSuccessRate);
        response.setEmailHasAttempts(emailAttempts > 0);
        response.setEmailSuccessRateDisplay(rateDisplay(emailSuccesses, emailAttempts, "N/A"));
        response.setEmailHealthSeverity(emailHealthSeverity(emailFailures));
        response.setStripeSuccesses(stripeSuccesses);
        response.setStripeFailures(stripeFailures);
        response.setStripeSuccessRate(stripeSuccessRate);
        response.setStripeHasEvents(stripeAttempts > 0);
        response.setStripeSuccessRateDisplay(rateDisplay(stripeSuccesses, stripeAttempts, "N/A"));
        response.setStripeHealthSeverity(stripeHealthSeverity(stripeFailures));
        if (!recentStripeEvents.isEmpty()) {
            response.setLastStripeEventType(recentStripeEvents.get(0).getEventType());
            response.setLastStripeEventTime(recentStripeEvents.get(0).getCreatedAt());
        }
        response.setCreditPackPurchasesCompletedToday(aiCreditPurchaseRepository.countCompletedBetween(start, end));
        response.setStandardApiP95ResponseTime(standardP95);
        response.setStandardApiP95Severity(standardApiSeverity(standardP95));
        response.setAdminApiP95ResponseTime(adminP95);
        response.setLatencyCategories(latencyCategories);
        response.setErrorsByStatus(requestMetricRepository.errorsByStatusBetween(start, end, List.of(400, 401, 403, 404, 409, 429, 500, 502)).stream().map(this::mapErrorStatus).toList());
        response.setErrorsByEndpoint(errorEndpointBreakdowns(recentErrors));
        response.setLastApiFailure(recentErrors.isEmpty() ? null : apiFailureSummary(recentErrors.get(0)));
        response.setLastStripeFailure(stripeFailuresForSummary.isEmpty() ? null : stripeFailureSummary(stripeFailuresForSummary.get(0)));
        response.setLastEmailFailure(emailFailuresForSummary.isEmpty() ? null : emailFailureSummary(emailFailuresForSummary.get(0)));
        response.setLastAiFailure(aiFailuresForSummary.isEmpty() ? null : aiFailureSummary(aiFailuresForSummary.get(0)));
        response.setSlowestEndpoints(slowest.stream().map(row -> mapEndpoint(row, latestRequestIdByEndpoint)).toList());
        response.setSlowRequests(slowRequestEventRepository.findBetween(start, end, PageRequest.of(0, 10)).stream().map(this::mapSlowRequest).toList());
        response.setAiFeatureCosts(aiCosts.stream().map(this::mapAiCost).toList());
        response.setCacheHealth(cacheMetricRepository.cacheBreakdownBetween(start, end).stream().map(this::mapCache).toList());
        response.setRecentEmailFailures(emailFailuresForSummary.stream().map(this::mapEmailFailure).toList());
        response.setRecentStripeEvents(recentStripeEvents.stream().map(this::mapStripeEvent).toList());
        response.setAlerts(systemAlertRepository.findByStatusBetween("OPEN", start, end, PageRequest.of(0, 12)).stream().map(this::mapAlert).toList());
        response.setResolvedAlerts(systemAlertRepository.findByStatusBetween("RESOLVED", start, end, PageRequest.of(0, 12)).stream().map(this::mapAlert).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public ObservabilityTrendsResponse trends() {
        return trends(ObservabilityDateRange.from("LAST_30_DAYS", null, null));
    }

    @Transactional(readOnly = true)
    public ObservabilityTrendsResponse trends(ObservabilityDateRange range) {
        log.info("Observability trends requested. range={}, start={}, end={}", range.cacheKey(), range.start(), range.end());
        ObservabilityTrendsResponse response = new ObservabilityTrendsResponse();
        response.setApiPerformance(requestMetricRepository.dailyApiStatsBetween(range.start(), range.end()).stream().map(this::mapApiTrend).toList());
        response.setAiCosts(aiRequestMetricRepository.dailyAiStatsBetween(range.start(), range.end()).stream().map(this::mapAiTrend).toList());
        return response;
    }

    @SuppressWarnings("unused")
    private void evaluateAlerts(long errors, long requests, BigDecimal aiCostToday, long emailFailures, long emailTotal, long stripeFailures) {
        maybeAlert("ERROR_RATE", "CRITICAL", "API error rate is above 5%.", rate(errors, requests) > 5);
        maybeAlert("AI_COST", "WARNING", "AI cost today is above the configured threshold.", aiCostToday.compareTo(aiCostDailyThreshold) > 0);
        maybeAlert("EMAIL_FAILURE_RATE", "WARNING", "Email failure rate is above 10%.", rate(emailFailures, emailTotal) > 10);
        maybeAlert("STRIPE_WEBHOOK_ERRORS", "CRITICAL", "Stripe webhook/payment failures detected today.", stripeFailures > 0);
    }

    private long p95Duration(LocalDateTime start, LocalDateTime end, long totalRequests) {
        if (totalRequests <= 0) {
            return 0;
        }
        int index = (int) Math.max(0, Math.ceil(totalRequests * 0.95) - 1);
        return requestMetricRepository.durationPageBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private void maybeAlert(String type, String severity, String message, boolean active) {
        if (active && !systemAlertRepository.existsByTypeAndResolvedFalse(type)) {
            systemAlertRepository.save(SystemAlert.builder().type(type).severity(severity).message(message).resolved(false).createdAt(LocalDateTime.now()).build());
        }
    }

    private BigDecimal estimateCost(int totalTokens) {
        if (totalTokens <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalTokens).multiply(nullSafe(aiCostPerThousandTokens)).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }

    private Map<String, String> latestRequestIdByEndpoint(List<RequestMetric> recentRequests) {
        Map<String, String> latest = new LinkedHashMap<>();
        for (RequestMetric metric : recentRequests) {
            latest.putIfAbsent(metric.getEndpoint(), metric.getRequestId());
        }
        return latest;
    }

    private EndpointMetricDto mapEndpoint(Object[] row, Map<String, String> latestRequestIdByEndpoint) {
        String endpoint = String.valueOf(row[0]);
        return new EndpointMetricDto(endpoint, endpointCategory(endpoint), longValue(row[1]), doubleValue(row[2]), longValue(row[3]), latestRequestIdByEndpoint.get(endpoint));
    }

    private ErrorStatusBreakdownDto mapErrorStatus(Object[] row) {
        return new ErrorStatusBreakdownDto((int) longValue(row[0]), longValue(row[1]));
    }

    private List<ErrorEndpointBreakdownDto> errorEndpointBreakdowns(List<RequestMetric> recentErrors) {
        Map<String, ErrorEndpointAccumulator> byEndpoint = new LinkedHashMap<>();
        for (RequestMetric metric : recentErrors) {
            ErrorEndpointAccumulator accumulator = byEndpoint.computeIfAbsent(metric.getEndpoint(), ErrorEndpointAccumulator::new);
            accumulator.count++;
            if (accumulator.latest == null || metric.getCreatedAt().isAfter(accumulator.latest.getCreatedAt())) {
                accumulator.latest = metric;
            }
        }
        return byEndpoint.values().stream()
                .sorted(Comparator.comparingLong((ErrorEndpointAccumulator item) -> item.count).reversed())
                .limit(10)
                .map(item -> new ErrorEndpointBreakdownDto(
                        item.endpoint,
                        endpointCategory(item.endpoint),
                        item.count,
                        item.latest == null ? null : item.latest.getRequestId(),
                        item.latest == null ? null : item.latest.getCreatedAt(),
                        item.latest == null ? null : safeErrorCode(item.latest)
                ))
                .toList();
    }

    private List<LatencyCategoryDto> latencyCategories(LocalDateTime start, LocalDateTime end, long aiRequestCount, long aiGenerationP95) {
        long standardCount = requestMetricRepository.countStandardBetween(start, end);
        long adminCount = requestMetricRepository.countAdminReportingBetween(start, end);
        long webhookCount = requestMetricRepository.countWebhookSystemBetween(start, end);
        return List.of(
                new LatencyCategoryDto("Standard API", standardCount, roundOne(requestMetricRepository.averageStandardBetween(start, end)), p95Standard(start, end, standardCount)),
                new LatencyCategoryDto("AI Generation", aiRequestCount, roundOne(aiRequestMetricRepository.averageDurationBetween(start, end)), aiGenerationP95),
                new LatencyCategoryDto("Admin/Reporting", adminCount, roundOne(requestMetricRepository.averageAdminReportingBetween(start, end)), p95AdminReporting(start, end, adminCount)),
                new LatencyCategoryDto("Webhook/System", webhookCount, roundOne(requestMetricRepository.averageWebhookSystemBetween(start, end)), p95WebhookSystem(start, end, webhookCount))
        );
    }

    private long p95ForCategory(List<LatencyCategoryDto> categories, String category) {
        return categories.stream()
                .filter(item -> category.equals(item.getCategory()))
                .map(LatencyCategoryDto::getP95DurationMs)
                .findFirst()
                .orElse(0L);
    }

    private long aiP95Duration(LocalDateTime start, LocalDateTime end, long aiRequestCount) {
        if (aiRequestCount <= 0) {
            return 0;
        }
        int index = (int) Math.max(0, Math.ceil(aiRequestCount * 0.95) - 1);
        return aiRequestMetricRepository.durationPageBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private long p95Standard(LocalDateTime start, LocalDateTime end, long requestCount) {
        if (requestCount <= 0) return 0;
        int index = (int) Math.max(0, Math.ceil(requestCount * 0.95) - 1);
        return requestMetricRepository.durationPageStandardBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private long p95AdminReporting(LocalDateTime start, LocalDateTime end, long requestCount) {
        if (requestCount <= 0) return 0;
        int index = (int) Math.max(0, Math.ceil(requestCount * 0.95) - 1);
        return requestMetricRepository.durationPageAdminReportingBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private long p95WebhookSystem(LocalDateTime start, LocalDateTime end, long requestCount) {
        if (requestCount <= 0) return 0;
        int index = (int) Math.max(0, Math.ceil(requestCount * 0.95) - 1);
        return requestMetricRepository.durationPageWebhookSystemBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private FailureSummaryDto apiFailureSummary(RequestMetric metric) {
        return new FailureSummaryDto(
                "API",
                metric.getCreatedAt(),
                metric.getRequestId(),
                metric.getUserId(),
                metric.getEndpoint(),
                null,
                safeErrorCode(metric),
                "API request completed with status " + metric.getStatus() + "."
        );
    }

    private FailureSummaryDto stripeFailureSummary(StripeEventMetric metric) {
        return new FailureSummaryDto(
                "Stripe",
                metric.getCreatedAt(),
                null,
                null,
                "Stripe webhook",
                metric.getEventType(),
                "STRIPE_EVENT_FAILED",
                safeMessage(metric.getErrorMessage(), "Stripe event processing failed.")
        );
    }

    private FailureSummaryDto emailFailureSummary(EmailDeliveryEvent event) {
        return new FailureSummaryDto(
                "Email",
                event.getCreatedAt(),
                null,
                null,
                "Email delivery",
                event.getTemplate(),
                "EMAIL_DELIVERY_FAILED",
                safeMessage(event.getErrorMessage(), "Email delivery failed.")
        );
    }

    private FailureSummaryDto aiFailureSummary(AiRequestMetric metric) {
        return new FailureSummaryDto(
                "AI",
                metric.getCreatedAt(),
                null,
                metric.getUserId(),
                null,
                metric.getFeature(),
                "AI_GENERATION_FAILED",
                "AI generation failed for " + metric.getFeature() + "."
        );
    }

    private String endpointCategory(String endpoint) {
        String value = endpoint == null ? "" : endpoint.toLowerCase();
        if (value.contains("/stripe/webhook") || value.contains("/health") || value.contains("/dev/")) {
            return "Webhook/System";
        }
        if (value.contains("/ai/") || value.contains("/coach/") || value.contains("/sessions/review")) {
            return "AI Generation";
        }
        if (value.contains("/admin/") || value.contains("/analytics/") || value.contains("/stats/") || value.contains("/calendar")) {
            return "Admin/Reporting";
        }
        return "Standard API";
    }

    private String safeErrorCode(RequestMetric metric) {
        if (metric.getErrorCode() != null && !metric.getErrorCode().isBlank()) {
            return metric.getErrorCode();
        }
        return statusErrorCode(metric.getStatus() == null ? 500 : metric.getStatus());
    }

    private String statusErrorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "AUTHENTICATION_REQUIRED";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 429 -> "RATE_LIMITED";
            case 502 -> "BAD_GATEWAY";
            default -> status >= 500 ? "SERVER_ERROR" : "HTTP_" + status;
        };
    }

    private String rateDisplay(long numerator, long denominator, String emptyValue) {
        return denominator <= 0 ? emptyValue : rate(numerator, denominator) + "%";
    }

    private String errorRateSeverity(double errorRate) {
        if (errorRate > 5) return "CRITICAL";
        if (errorRate > 2) return "WARNING";
        return "HEALTHY";
    }

    private String standardApiSeverity(long p95Ms) {
        if (p95Ms > 3000) return "CRITICAL";
        if (p95Ms > 1500) return "WARNING";
        return "HEALTHY";
    }

    private String aiGenerationSeverity(long p95Ms) {
        return p95Ms > 30000 ? "WARNING" : "HEALTHY";
    }

    private String cacheHitSeverity(double hitRate, long totalEvents) {
        return totalEvents > 0 && hitRate < 50 ? "WARNING" : "HEALTHY";
    }

    private String stripeHealthSeverity(long failures) {
        return failures > 0 ? "WARNING" : "HEALTHY";
    }

    private String emailHealthSeverity(long failures) {
        return failures > 0 ? "WARNING" : "HEALTHY";
    }

    private String safeMessage(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return limit(value, 220);
    }

    private double roundOne(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private SlowRequestDto mapSlowRequest(SlowRequestEvent event) {
        return SlowRequestDto.builder().endpoint(event.getEndpoint()).durationMs(event.getDurationMs()).status(event.getStatus()).userId(event.getUserId()).severity(event.getSeverity()).createdAt(event.getCreatedAt()).build();
    }

    private AiFeatureCostDto mapAiCost(Object[] row) {
        return AiFeatureCostDto.builder().feature(String.valueOf(row[0])).calls(longValue(row[1])).tokens(longValue(row[2])).estimatedCost(bigDecimalValue(row[3])).build();
    }

    private CacheHealthDto mapCache(Object[] row) {
        long hits = longValue(row[1]);
        long misses = longValue(row[2]);
        return CacheHealthDto.builder().cacheName(String.valueOf(row[0])).hits(hits).misses(misses).hitRate(rate(hits, hits + misses)).build();
    }

    private EmailFailureDto mapEmailFailure(EmailDeliveryEvent event) {
        return EmailFailureDto.builder().recipient(event.getRecipient()).template(event.getTemplate()).errorMessage(event.getErrorMessage()).createdAt(event.getCreatedAt()).build();
    }

    private StripeEventDto mapStripeEvent(StripeEventMetric event) {
        return StripeEventDto.builder().eventType(event.getEventType()).success(Boolean.TRUE.equals(event.getSuccess())).durationMs(event.getDurationMs()).errorMessage(event.getErrorMessage()).createdAt(event.getCreatedAt()).build();
    }

    private SystemAlertDto mapAlert(SystemAlert alert) {
        return SystemAlertDto.builder().id(alert.getId()).type(alert.getType()).severity(alert.getSeverity()).message(alert.getMessage()).status(alert.getStatus()).createdAt(alert.getCreatedAt()).resolvedAt(alert.getResolvedAt()).resolvedBy(alert.getResolvedBy()).build();
    }

    private ApiTrendPointDto mapApiTrend(Object[] row) {
        long requests = longValue(row[1]);
        long errors = longValue(row[3]);
        return ApiTrendPointDto.builder().date(localDate(row[0])).requests(requests).avgDurationMs(doubleValue(row[2])).errorRate(rate(errors, requests)).build();
    }

    private AiTrendPointDto mapAiTrend(Object[] row) {
        return AiTrendPointDto.builder().date(localDate(row[0])).tokens(longValue(row[1])).estimatedCost(bigDecimalValue(row[2])).build();
    }

    private long percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0;
    }

    private BigDecimal bigDecimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }


    private static final class ErrorEndpointAccumulator {
        private final String endpoint;
        private long count;
        private RequestMetric latest;

        private ErrorEndpointAccumulator(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    private record CachedSummary(ObservabilitySummaryResponse summary, Instant expiresAt) {
    }

    @java.lang.SuppressWarnings("all")
    public ObservabilityService(final RequestMetricRepository requestMetricRepository, final SlowRequestEventRepository slowRequestEventRepository, final AiRequestMetricRepository aiRequestMetricRepository, final CacheMetricRepository cacheMetricRepository, final EmailDeliveryEventRepository emailDeliveryEventRepository, final StripeEventMetricRepository stripeEventMetricRepository, final QueryPerformanceEventRepository queryPerformanceEventRepository, final SystemAlertRepository systemAlertRepository, final AiCreditPurchaseRepository aiCreditPurchaseRepository, final CacheMetricRecorder cacheMetricRecorder, final QueryPerformanceEventRecorder queryPerformanceEventRecorder) {
        this.requestMetricRepository = requestMetricRepository;
        this.slowRequestEventRepository = slowRequestEventRepository;
        this.aiRequestMetricRepository = aiRequestMetricRepository;
        this.cacheMetricRepository = cacheMetricRepository;
        this.emailDeliveryEventRepository = emailDeliveryEventRepository;
        this.stripeEventMetricRepository = stripeEventMetricRepository;
        this.queryPerformanceEventRepository = queryPerformanceEventRepository;
        this.systemAlertRepository = systemAlertRepository;
        this.aiCreditPurchaseRepository = aiCreditPurchaseRepository;
        this.cacheMetricRecorder = cacheMetricRecorder;
        this.queryPerformanceEventRecorder = queryPerformanceEventRecorder;
    }
}
