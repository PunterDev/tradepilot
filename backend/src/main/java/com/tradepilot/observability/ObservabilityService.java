package com.tradepilot.observability;

import com.tradepilot.observability.dto.*;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.function.Supplier;

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
        LocalDateTime now = LocalDateTime.now();
        requestMetricRepository.save(RequestMetric.builder().endpoint(limit(endpoint, 220)).method(limit(method, 16)).status(status).durationMs(durationMs).userId(userId).createdAt(now).build());
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
        ObservabilitySummaryResponse response = new ObservabilitySummaryResponse();
        response.setRequestsToday(requestCount);
        response.setAvgResponseTime(requestMetricRepository.averageDurationBetween(start, end));
        response.setP95ResponseTime(p95Duration(start, end, requestCount));
        response.setSlowestEndpoint(slowest.isEmpty() ? "No traffic yet" : String.valueOf(slowest.get(0)[0]));
        response.setErrorRate(rate(errorCount, requestCount));
        response.setAiCallsToday(aiRequestMetricRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end));
        response.setAiTokensToday(aiRequestMetricRepository.totalTokensBetween(start, end));
        response.setAiCostToday(aiCostRange);
        response.setAiCostMonth(aiCostMonth);
        response.setAiAvgResponseTime(aiRequestMetricRepository.averageDurationBetween(start, end));
        response.setMostExpensiveFeature(aiCosts.isEmpty() ? "No AI calls yet" : String.valueOf(aiCosts.get(0)[0]));
        response.setMostUsedModel(models.isEmpty() ? "No model usage yet" : String.valueOf(models.get(0)[0]));
        response.setCacheHits(cacheHits);
        response.setCacheMisses(cacheMisses);
        response.setCacheHitRate(rate(cacheHits, cacheHits + cacheMisses));
        response.setEmailsSent(emailSuccesses);
        response.setEmailsFailed(emailFailures);
        response.setEmailSuccessRate(rate(emailSuccesses, emailSuccesses + emailFailures));
        response.setStripeSuccesses(stripeSuccesses);
        response.setStripeFailures(stripeFailures);
        response.setStripeSuccessRate(rate(stripeSuccesses, stripeSuccesses + stripeFailures));
        response.setSlowestEndpoints(slowest.stream().map(this::mapEndpoint).toList());
        response.setSlowRequests(slowRequestEventRepository.findBetween(start, end, PageRequest.of(0, 10)).stream().map(this::mapSlowRequest).toList());
        response.setAiFeatureCosts(aiCosts.stream().map(this::mapAiCost).toList());
        response.setCacheHealth(cacheMetricRepository.cacheBreakdownBetween(start, end).stream().map(this::mapCache).toList());
        response.setRecentEmailFailures(emailDeliveryEventRepository.findFailuresBetween(start, end, PageRequest.of(0, 6)).stream().map(this::mapEmailFailure).toList());
        response.setRecentStripeEvents(stripeEventMetricRepository.findBetween(start, end, PageRequest.of(0, 8)).stream().map(this::mapStripeEvent).toList());
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

    private EndpointMetricDto mapEndpoint(Object[] row) {
        return EndpointMetricDto.builder().endpoint(String.valueOf(row[0])).requestCount(longValue(row[1])).avgDurationMs(doubleValue(row[2])).maxDurationMs(longValue(row[3])).build();
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


    private record CachedSummary(ObservabilitySummaryResponse summary, Instant expiresAt) {
    }

    @java.lang.SuppressWarnings("all")
    public ObservabilityService(final RequestMetricRepository requestMetricRepository, final SlowRequestEventRepository slowRequestEventRepository, final AiRequestMetricRepository aiRequestMetricRepository, final CacheMetricRepository cacheMetricRepository, final EmailDeliveryEventRepository emailDeliveryEventRepository, final StripeEventMetricRepository stripeEventMetricRepository, final QueryPerformanceEventRepository queryPerformanceEventRepository, final SystemAlertRepository systemAlertRepository, final CacheMetricRecorder cacheMetricRecorder, final QueryPerformanceEventRecorder queryPerformanceEventRecorder) {
        this.requestMetricRepository = requestMetricRepository;
        this.slowRequestEventRepository = slowRequestEventRepository;
        this.aiRequestMetricRepository = aiRequestMetricRepository;
        this.cacheMetricRepository = cacheMetricRepository;
        this.emailDeliveryEventRepository = emailDeliveryEventRepository;
        this.stripeEventMetricRepository = stripeEventMetricRepository;
        this.queryPerformanceEventRepository = queryPerformanceEventRepository;
        this.systemAlertRepository = systemAlertRepository;
        this.cacheMetricRecorder = cacheMetricRecorder;
        this.queryPerformanceEventRecorder = queryPerformanceEventRecorder;
    }
}
