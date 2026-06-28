package com.tradepilot.observability;

import com.tradepilot.observability.dto.AlertResolveResponse;
import com.tradepilot.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ObservabilityAlertService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObservabilityAlertService.class);
    private static final String OPEN = "OPEN";
    private static final String RESOLVED = "RESOLVED";
    private final RequestMetricRepository requestMetricRepository;
    private final AiRequestMetricRepository aiRequestMetricRepository;
    private final CacheMetricRepository cacheMetricRepository;
    private final EmailDeliveryEventRepository emailDeliveryEventRepository;
    private final StripeEventMetricRepository stripeEventMetricRepository;
    private final SystemAlertRepository systemAlertRepository;
    @Value("${app.observability.ai-cost-daily-threshold:10.00}")
    private BigDecimal aiCostDailyThreshold;

    @Scheduled(fixedDelayString = "${app.observability.alert-evaluation-interval-ms:300000}")
    @Transactional
    public void evaluateScheduledAlerts() {
        evaluateAlerts();
    }

    public void evaluateAlerts() {
        log.info("Observability alert evaluation started.");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fifteenMinutesAgo = now.minusMinutes(15);
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long requests = requestMetricRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(fifteenMinutesAgo, now);
        long errors = requestMetricRepository.countErrorsBetween(fifteenMinutesAgo, now);
        maybeAlert("API_ERROR_RATE_HIGH", "WARNING", "API error rate is above 5% over the last 15 minutes.", requests > 0 && rate(errors, requests) > 5);
        long p95 = p95Duration(fifteenMinutesAgo, now, requests);
        maybeAlert("API_P95_LATENCY_HIGH", "WARNING", "API p95 latency is above 2000ms over the last 15 minutes.", p95 > 2000);
        BigDecimal aiCostToday = nullSafe(aiRequestMetricRepository.totalCostSince(today));
        maybeAlert("OPENAI_DAILY_COST_HIGH", "WARNING", "OpenAI estimated cost exceeded the configured daily threshold.", aiCostToday.compareTo(nullSafe(aiCostDailyThreshold)) > 0);
        long stripeFailures = stripeEventMetricRepository.failuresBetween(oneHourAgo, now);
        maybeAlert("STRIPE_FAILURES_HIGH", "CRITICAL", "Stripe failures exceeded 3 events in the last hour.", stripeFailures > 3);
        long emailFailures = emailDeliveryEventRepository.failuresBetween(oneHourAgo, now);
        maybeAlert("EMAIL_FAILURES_HIGH", "CRITICAL", "Email failures exceeded 3 events in the last hour.", emailFailures > 3);
        long cacheHits = cacheMetricRepository.hitsBetween(fifteenMinutesAgo, now);
        long cacheMisses = cacheMetricRepository.missesBetween(fifteenMinutesAgo, now);
        long cacheEvents = cacheHits + cacheMisses;
        maybeAlert("CACHE_HIT_RATE_LOW", "WARNING", "Cache hit rate is below 60% over the last 15 minutes.", cacheEvents >= 20 && rate(cacheHits, cacheEvents) < 60);
        log.info("Observability alert evaluation completed. requests={}, errors={}, p95Ms={}, stripeFailures={}, emailFailures={}, cacheEvents={}", requests, errors, p95, stripeFailures, emailFailures, cacheEvents);
    }

    @Transactional
    public AlertResolveResponse resolveAlert(Long alertId, User admin) {
        SystemAlert alert = systemAlertRepository.findById(alertId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Alert not found."));
        alert.setStatus(RESOLVED);
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(admin == null ? null : admin.getId());
        SystemAlert saved = systemAlertRepository.save(alert);
        log.info("System alert resolved. alertId={}, type={}, resolvedBy={}", saved.getId(), saved.getType(), saved.getResolvedBy());
        return AlertResolveResponse.builder().id(saved.getId()).status(saved.getStatus()).resolvedAt(saved.getResolvedAt()).resolvedBy(saved.getResolvedBy()).build();
    }

    private void maybeAlert(String type, String severity, String message, boolean active) {
        if (!active) {
            return;
        }
        if (systemAlertRepository.existsByTypeAndStatus(type, OPEN)) {
            log.trace("Duplicate open alert skipped. type={}", type);
            return;
        }
        systemAlertRepository.save(SystemAlert.builder().type(type).severity(severity).message(message).status(OPEN).resolved(false).createdAt(LocalDateTime.now()).build());
        log.warn("System alert opened: type={} severity={} message={}", type, severity, message);
    }

    private long p95Duration(LocalDateTime start, LocalDateTime end, long totalRequests) {
        if (totalRequests <= 0) {
            return 0;
        }
        int index = (int) Math.max(0, Math.ceil(totalRequests * 0.95) - 1);
        return requestMetricRepository.durationPageBetween(start, end, PageRequest.of(index, 1)).stream().findFirst().orElse(0L);
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @java.lang.SuppressWarnings("all")
    public ObservabilityAlertService(final RequestMetricRepository requestMetricRepository, final AiRequestMetricRepository aiRequestMetricRepository, final CacheMetricRepository cacheMetricRepository, final EmailDeliveryEventRepository emailDeliveryEventRepository, final StripeEventMetricRepository stripeEventMetricRepository, final SystemAlertRepository systemAlertRepository) {
        this.requestMetricRepository = requestMetricRepository;
        this.aiRequestMetricRepository = aiRequestMetricRepository;
        this.cacheMetricRepository = cacheMetricRepository;
        this.emailDeliveryEventRepository = emailDeliveryEventRepository;
        this.stripeEventMetricRepository = stripeEventMetricRepository;
        this.systemAlertRepository = systemAlertRepository;
    }
}
