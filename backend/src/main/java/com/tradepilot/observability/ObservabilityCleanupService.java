package com.tradepilot.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObservabilityCleanupService {
    private final RequestMetricRepository requestMetricRepository;
    private final SlowRequestEventRepository slowRequestEventRepository;
    private final AiRequestMetricRepository aiRequestMetricRepository;
    private final CacheMetricRepository cacheMetricRepository;
    private final EmailDeliveryEventRepository emailDeliveryEventRepository;
    private final StripeEventMetricRepository stripeEventMetricRepository;
    private final QueryPerformanceEventRepository queryPerformanceEventRepository;
    private final SystemAlertRepository systemAlertRepository;

    @Value("${app.observability.retention.api-request-metrics-days:60}")
    private int apiRequestMetricsDays;
    @Value("${app.observability.retention.slow-request-events-days:90}")
    private int slowRequestEventsDays;
    @Value("${app.observability.retention.ai-request-metrics-days:90}")
    private int aiRequestMetricsDays;
    @Value("${app.observability.retention.cache-metrics-days:30}")
    private int cacheMetricsDays;
    @Value("${app.observability.retention.email-delivery-events-days:90}")
    private int emailDeliveryEventsDays;
    @Value("${app.observability.retention.stripe-events-days:180}")
    private int stripeEventsDays;
    @Value("${app.observability.retention.query-performance-events-days:60}")
    private int queryPerformanceEventsDays;
    @Value("${app.observability.retention.system-alerts-days:180}")
    private int systemAlertsDays;

    @Scheduled(cron = "${app.observability.cleanup-cron:0 20 3 * * *}")
    @Transactional
    public void cleanupOldObservabilityData() {
        log.info("Observability cleanup started.");
        CleanupCounts counts = cleanupNow(LocalDateTime.now());
        log.info("Observability cleanup completed. {}", counts);
    }

    @Transactional
    public CleanupCounts cleanupNow(LocalDateTime now) {
        log.trace("Observability cleanup retention windows. apiRequestDays={}, slowRequestDays={}, aiRequestDays={}, cacheDays={}, emailDays={}, stripeDays={}, queryDays={}, alertDays={}",
                apiRequestMetricsDays,
                slowRequestEventsDays,
                aiRequestMetricsDays,
                cacheMetricsDays,
                emailDeliveryEventsDays,
                stripeEventsDays,
                queryPerformanceEventsDays,
                systemAlertsDays);
        return new CleanupCounts(
                requestMetricRepository.deleteOlderThan(now.minusDays(apiRequestMetricsDays)),
                slowRequestEventRepository.deleteOlderThan(now.minusDays(slowRequestEventsDays)),
                aiRequestMetricRepository.deleteOlderThan(now.minusDays(aiRequestMetricsDays)),
                cacheMetricRepository.deleteOlderThan(now.minusDays(cacheMetricsDays)),
                emailDeliveryEventRepository.deleteOlderThan(now.minusDays(emailDeliveryEventsDays)),
                stripeEventMetricRepository.deleteOlderThan(now.minusDays(stripeEventsDays)),
                queryPerformanceEventRepository.deleteOlderThan(now.minusDays(queryPerformanceEventsDays)),
                systemAlertRepository.deleteOlderThan(now.minusDays(systemAlertsDays))
        );
    }

    public record CleanupCounts(
            int apiRequestMetrics,
            int slowRequestEvents,
            int aiRequestMetrics,
            int cacheMetrics,
            int emailDeliveryEvents,
            int stripeEvents,
            int queryPerformanceEvents,
            int systemAlerts
    ) {}
}
