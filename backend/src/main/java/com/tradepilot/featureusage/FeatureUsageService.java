package com.tradepilot.featureusage;

import com.tradepilot.billing.PlanType;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.dto.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AggregateCacheService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureUsageService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureUsageService.class);
    private static final long MIN_HEALTH_SAMPLE_USERS = 3;
    private static final double HIGH_ADOPTION_THRESHOLD = 60.0;
    private static final double HEALTHY_ADOPTION_THRESHOLD = 25.0;
    private static final double HEALTHY_REPEAT_THRESHOLD = 25.0;
    private static final double NEEDS_ATTENTION_ADOPTION_THRESHOLD = 15.0;
    private static final double DECLINING_GROWTH_THRESHOLD = -20.0;
    private final FeatureUsageRepository featureUsageRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final AggregateCacheService aggregateCacheService;
    private final TransactionTemplate telemetryTransactionTemplate;
    private final FeatureUsageMetadataRegistry metadataRegistry;

    public void track(User user, FeatureKey feature) {
        track(user, feature, null);
    }

    public void track(User user, FeatureKey feature, LocalDateTime createdAt) {
        track(user, feature, null, null, null, createdAt);
    }

    public void track(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, Long accountId, LocalDateTime createdAt) {
        track(user, feature, action, eventType, accountId, null, null, null, null, createdAt);
    }

    public void track(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, Long accountId, String entityType, String entityId, String source, String metadataJson) {
        track(user, feature, action, eventType, accountId, entityType, entityId, source, metadataJson, null);
    }

    public void track(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, Long accountId, String entityType, String entityId, String source, String metadataJson, LocalDateTime createdAt) {
        if (user == null || feature == null) {
            return;
        }
        try {
            telemetryTransactionTemplate.executeWithoutResult(status -> {
                FeatureUsageMetadata metadata = metadataRegistry.get(feature);
                FeatureUsageEvent event = new FeatureUsageEvent();
                event.setUserId(user.getId());
                event.setFeature(feature);
                event.setAction(action);
                event.setEventType(eventType == null ? metadata.eventType() : eventType);
                event.setAccountId(accountId);
                event.setEntityType(entityType);
                event.setEntityId(entityId);
                event.setSource(source);
                event.setPlanAtEvent(user.getPlan() == null ? null : user.getPlan().name());
                event.setMetadataJson(metadataJson);
                event.setOccurredAt(createdAt);
                event.setCreatedAt(createdAt);
                FeatureUsageEvent saved = featureUsageRepository.saveAndFlush(event);
                log.trace("FeatureUsageEvent saved. userId={}, feature={}, action={}, eventId={}", saved.getUserId(), saved.getFeature(), saved.getAction(), saved.getId());
            });
        } catch (DataAccessException ex) {
            log.warn("Feature usage telemetry failed. userId={}, feature={}, reason={}", user.getId(), feature, ex.getClass().getSimpleName());
        } catch (RuntimeException ex) {
            log.warn("Feature usage telemetry failed unexpectedly. userId={}, feature={}, reason={}", user.getId(), feature, ex.getClass().getSimpleName());
        }
    }

    /**
     * Persists a lifecycle event in the caller's transaction. Use this only when
     * the event is part of the authoritative outcome being committed.
     */
    public void trackAuthoritative(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType,
                                   String entityType, String entityId, String source, String metadataJson) {
        if (user == null || feature == null) {
            throw new IllegalArgumentException("Authoritative feature usage requires a user and feature.");
        }
        FeatureUsageEvent event = new FeatureUsageEvent();
        event.setUserId(user.getId());
        event.setFeature(feature);
        event.setAction(action);
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setSource(source);
        event.setPlanAtEvent(user.getPlan() == null ? null : user.getPlan().name());
        event.setMetadataJson(metadataJson);
        featureUsageRepository.save(event);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageSummaryResponse summary() {
        return aggregateCacheService.get("feature-usage-summary", Duration.ofSeconds(120), () -> summary(null, null, null, null, null, null, null, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageSummaryResponse summary(String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, FeatureUsageEventType eventType, Boolean excludeAdmins, String activity) {
        FilterWindow window = window(range, startDate, endDate);
        Aggregation aggregation = aggregate(window, plan, category, eventType, excludeAdmins == null || excludeAdmins, activity);
        return buildSummary(window, aggregation);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageTrendsResponse trends() {
        return trends(null, null, null, null, null, null, null, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageTrendsResponse trends(String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, FeatureUsageEventType eventType, Boolean excludeAdmins, String activity) {
        FilterWindow window = window(range, startDate, endDate);
        List<Object[]> rows = featureUsageRepository.countByDateAndFeatureBetween(window.startDate().atStartOfDay(), window.endDate().plusDays(1).atStartOfDay(), plan, eventType, excludeAdmins == null || excludeAdmins, UserRole.ADMIN);
        Map<FeatureKey, Map<LocalDate, Long>> counts = new EnumMap<>(FeatureKey.class);
        for (Object[] row : rows) {
            FeatureKey feature = (FeatureKey) row[1];
            FeatureUsageMetadata metadata = metadataRegistry.get(feature);
            if (!matches(metadata, category, eventType, activity)) continue;
            counts.computeIfAbsent(feature, ignored -> new HashMap<>()).put((LocalDate) row[0], ((Number) row[2]).longValue());
        }
        Map<FeatureKey, List<FeatureTrendPointResponse>> series = Arrays.stream(FeatureKey.values())
                .filter(feature -> matches(metadataRegistry.get(feature), category, eventType, activity))
                .collect(Collectors.toMap(feature -> feature, feature -> trend(counts.getOrDefault(feature, Map.of()), window.startDate(), window.days()), (a, b) -> a, () -> new EnumMap<>(FeatureKey.class)));
        FeatureKey fastest = series.entrySet().stream().max(Comparator.comparingLong(entry -> secondHalf(entry.getValue()) - firstHalf(entry.getValue()))).map(Map.Entry::getKey).orElse(null);
        FeatureKey decline = series.entrySet().stream().min(Comparator.comparingLong(entry -> secondHalf(entry.getValue()) - firstHalf(entry.getValue()))).map(Map.Entry::getKey).orElse(null);
        FeatureKey stable = series.entrySet().stream().min(Comparator.comparingLong(entry -> Math.abs(secondHalf(entry.getValue()) - firstHalf(entry.getValue())))).map(Map.Entry::getKey).orElse(null);
        return new FeatureUsageTrendsResponse(
                series.getOrDefault(FeatureKey.DAILY_COACH, List.of()),
                series.getOrDefault(FeatureKey.WEEKLY_COACH, List.of()),
                series.getOrDefault(FeatureKey.TRADING_DNA, List.of()),
                series.getOrDefault(FeatureKey.AI_REVIEW, List.of()),
                series.getOrDefault(FeatureKey.SESSION_REVIEW, List.of()),
                series,
                fastest,
                decline,
                stable
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FeatureUsageUserResponse> users() {
        return users(null, null, null, null, null, null, null, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FeatureUsageUserResponse> users(String range, LocalDate startDate, LocalDate endDate, PlanType plan, FeatureUsageCategory category, FeatureUsageEventType eventType, Boolean excludeAdmins, String activity) {
        FilterWindow window = window(range, startDate, endDate);
        Aggregation aggregation = aggregate(window, plan, category, eventType, excludeAdmins == null || excludeAdmins, activity);
        List<Long> topUserIds = aggregation.byUser().entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().totalEvents(), a.getValue().totalEvents()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
        if (topUserIds.isEmpty()) return List.of();
        Map<Long, User> usersById = userRepository.findAllById(topUserIds).stream().collect(Collectors.toMap(User::getId, user -> user));
        return topUserIds.stream().map(userId -> userResponse(usersById.get(userId), aggregation.byUser().get(userId))).filter(Objects::nonNull).toList();
    }

    private FeatureUsageSummaryResponse buildSummary(FilterWindow window, Aggregation aggregation) {
        List<FeatureAdoptionResponse> features = metadataRegistry.all().stream()
                .filter(metadata -> matches(metadata, aggregation.category(), aggregation.eventType(), aggregation.activity()))
                .map(metadata -> adoption(metadata, aggregation))
                .sorted(Comparator.comparingInt(feature -> metadataRegistry.get(feature.feature()).sortOrder()))
                .toList();
        long totalEvents = features.stream().mapToLong(FeatureAdoptionResponse::totalUsage).sum();
        long activeActions = features.stream().mapToLong(FeatureAdoptionResponse::activeEventCount).sum();
        long passiveViews = features.stream().mapToLong(FeatureAdoptionResponse::passiveEventCount).sum();
        long aiGenerations = features.stream().mapToLong(FeatureAdoptionResponse::aiGenerationCount).sum();
        long uniqueFeaturesUsed = features.stream().filter(feature -> feature.totalUsage() > 0).count();
        long activeUsers = aggregation.activeUserIds().size();
        long previousActiveUsers = aggregation.previousActiveUserCount();
        long previousActiveActions = features.stream().mapToLong(FeatureAdoptionResponse::previousPeriodActiveEvents).sum();
        long previousPassiveViews = features.stream().mapToLong(FeatureAdoptionResponse::previousPeriodPassiveEvents).sum();
        long previousAiGenerations = features.stream().mapToLong(FeatureAdoptionResponse::previousPeriodAiGenerations).sum();
        long previousFeaturesAdopted = features.stream().filter(feature -> feature.previousPeriodEvents() > 0).count();
        FeatureKey highestVolume = features.stream().max(Comparator.comparingLong(FeatureAdoptionResponse::totalUsage)).map(FeatureAdoptionResponse::feature).orElse(null);
        FeatureKey highestAdoption = features.stream().max(Comparator.comparingDouble(FeatureAdoptionResponse::activeUserAdoptionRate)).map(FeatureAdoptionResponse::feature).orElse(null);
        FeatureKey highestActiveAdoption = features.stream()
                .filter(feature -> !feature.passiveEvent() && feature.activeEventCount() > 0)
                .max(Comparator.comparingDouble(FeatureAdoptionResponse::activeUserAdoptionRate))
                .map(FeatureAdoptionResponse::feature)
                .orElse(null);
        FeatureKey lowestAdoption = features.stream().filter(feature -> feature.totalUsage() > 0).min(Comparator.comparingDouble(FeatureAdoptionResponse::activeUserAdoptionRate)).map(FeatureAdoptionResponse::feature).orElse(null);
        LocalDate previousEndDate = window.startDate().minusDays(1);
        LocalDate previousStartDate = window.startDate().minusDays(window.days());
        return new FeatureUsageSummaryResponse(
                aggregation.totalEligibleUsers(),
                activeUsers,
                activeUsers,
                totalEvents,
                activeActions,
                passiveViews,
                aiGenerations,
                uniqueFeaturesUsed,
                activeUsers == 0 ? 0 : round((double) activeActions / activeUsers),
                activeUsers == 0 ? 0 : round(aggregation.byUser().values().stream().mapToLong(UserAggregate::distinctFeatures).average().orElse(0)),
                features,
                highestVolume,
                lowestAdoption,
                highestVolume,
                highestAdoption,
                highestActiveAdoption,
                lowestAdoption,
                comparison(activeUsers, previousActiveUsers, 0),
                comparison(activeActions, previousActiveActions, 0),
                comparison(passiveViews, previousPassiveViews, 0),
                comparison(aiGenerations, previousAiGenerations, 0),
                comparison(uniqueFeaturesUsed, previousFeaturesAdopted, 0),
                insights(features, aggregation),
                activationFunnelMetrics(window, aggregation.plan(), true),
                dataQuality(features),
                metadataRegistry.all().stream().map(this::metadataResponse).toList(),
                window.startDate(),
                window.endDate(),
                previousStartDate,
                previousEndDate,
                LocalDateTime.now()
        );
    }

    private FeatureAdoptionResponse adoption(FeatureUsageMetadata metadata, Aggregation aggregation) {
        FeatureAggregate current = aggregation.byFeature().getOrDefault(metadata.key(), FeatureAggregate.empty());
        FeatureAggregate previous = aggregation.previousByFeature().getOrDefault(metadata.key(), FeatureAggregate.empty());
        long activeEvents = current.activeEvents();
        long passiveEvents = current.passiveEvents();
        long aiGenerations = current.aiGenerations();
        long previousActiveEvents = previous.activeEvents();
        long previousPassiveEvents = previous.passiveEvents();
        long previousAiGenerations = previous.aiGenerations();
        double eventsPerUser = current.uniqueUsers() == 0 ? 0 : round((double) current.totalEvents() / current.uniqueUsers());
        double repeatUsage = current.uniqueUsers() == 0 ? 0 : round((double) current.repeatUsers() / current.uniqueUsers() * 100);
        double previousRepeatUsage = previous.uniqueUsers() == 0 ? 0 : round((double) previous.repeatUsers() / previous.uniqueUsers() * 100);
        double activeAdoption = aggregation.activeUserIds().isEmpty() ? 0 : round((double) current.uniqueUsers() / aggregation.activeUserIds().size() * 100);
        double previousActiveAdoption = aggregation.previousActiveUserCount() == 0 ? 0 : round((double) previous.uniqueUsers() / aggregation.previousActiveUserCount() * 100);
        double registeredAdoption = aggregation.totalEligibleUsers() == 0 ? 0 : round((double) current.uniqueUsers() / aggregation.totalEligibleUsers() * 100);
        double userGrowth = growth(current.uniqueUsers(), previous.uniqueUsers());
        double eventGrowth = growth(current.totalEvents(), previous.totalEvents());
        double activeActionGrowth = growth(activeEvents, previousActiveEvents);
        double adoptionDelta = round(activeAdoption - previousActiveAdoption);
        Double passiveToActiveConversion = passiveEvents == 0 ? null : round((double) activeEvents / passiveEvents * 100);
        double engagement = engagementScore(activeAdoption, repeatUsage, eventsPerUser, eventGrowth);
        double opportunity = opportunityScore(metadata, activeAdoption, repeatUsage, eventGrowth);
        String health = healthStatus(activeAdoption, repeatUsage, eventGrowth, current.totalEvents(), aggregation.activeUserIds().size());
        return new FeatureAdoptionResponse(metadata.key(), metadata.displayName(), metadata.shortName(), metadata.description(), metadata.category(), metadata.eventType(), metadata.coreFeature(), metadata.aiFeature(), metadata.paidAction(), metadata.passiveEvent(), metadataRegistry.isKnown(metadata.key()), current.uniqueUsers(), current.totalEvents(), activeEvents, passiveEvents, aiGenerations, registeredAdoption, activeAdoption, eventsPerUser, current.repeatUsers(), repeatUsage, previous.uniqueUsers(), previous.totalEvents(), previousActiveEvents, previousPassiveEvents, previousAiGenerations, previous.repeatUsers(), previousRepeatUsage, userGrowth, eventGrowth, adoptionDelta, activeActionGrowth, passiveToActiveConversion, engagement, opportunity, health, statusLabel(health));
    }

    private Aggregation aggregate(FilterWindow window, PlanType plan, FeatureUsageCategory category, FeatureUsageEventType eventType, boolean excludeAdmins, String activity) {
        LocalDateTime start = window.startDate().atStartOfDay();
        LocalDateTime end = window.endDate().plusDays(1).atStartOfDay();
        LocalDateTime previousStart = start.minusDays(window.days());
        List<Object[]> currentRows = featureUsageRepository.countByFeatureAndUserBetween(start, end, plan, eventType, excludeAdmins, UserRole.ADMIN);
        List<Object[]> previousRows = featureUsageRepository.countByFeatureAndUserBetween(previousStart, start, plan, eventType, excludeAdmins, UserRole.ADMIN);
        Map<FeatureKey, FeatureAggregate> byFeature = new EnumMap<>(FeatureKey.class);
        Map<Long, UserAggregate> byUser = new HashMap<>();
        Set<Long> activeUsers = new HashSet<>();
        for (Object[] row : currentRows) {
            FeatureKey feature = (FeatureKey) row[0];
            FeatureUsageMetadata metadata = metadataRegistry.get(feature);
            if (!matches(metadata, category, eventType, activity)) continue;
            Long userId = (Long) row[1];
            long count = ((Number) row[2]).longValue();
            long passiveCount = ((Number) row[3]).longValue();
            long aiCount = ((Number) row[4]).longValue();
            LocalDateTime first = (LocalDateTime) row[5];
            LocalDateTime last = (LocalDateTime) row[6];
            long activeDays = ((Number) row[7]).longValue();
            byFeature.computeIfAbsent(feature, ignored -> FeatureAggregate.empty()).add(userId, metadata, count, passiveCount, aiCount, first, last, activeDays);
            byUser.computeIfAbsent(userId, ignored -> new UserAggregate()).add(metadata, count, passiveCount, aiCount, last);
            activeUsers.add(userId);
        }
        Map<FeatureKey, FeatureAggregate> previousByFeature = new EnumMap<>(FeatureKey.class);
        for (Object[] row : previousRows) {
            FeatureKey feature = (FeatureKey) row[0];
            FeatureUsageMetadata metadata = metadataRegistry.get(feature);
            if (!matches(metadata, category, eventType, activity)) continue;
            previousByFeature.computeIfAbsent(feature, ignored -> FeatureAggregate.empty()).add((Long) row[1], metadata, ((Number) row[2]).longValue(), ((Number) row[3]).longValue(), ((Number) row[4]).longValue(), (LocalDateTime) row[5], (LocalDateTime) row[6], ((Number) row[7]).longValue());
        }
        long totalEligibleUsers = featureUsageRepository.countEligibleUsers(plan, excludeAdmins, UserRole.ADMIN);
        return new Aggregation(byFeature, previousByFeature, byUser, activeUsers, totalEligibleUsers, category, eventType, activity, plan);
    }

    private boolean matches(FeatureUsageMetadata metadata, FeatureUsageCategory category, FeatureUsageEventType eventType, String activity) {
        if (category != null && metadata.category() != category) return false;
        if ("ACTIVE".equalsIgnoreCase(activity) && metadata.passiveEvent()) return false;
        if ("PASSIVE".equalsIgnoreCase(activity) && !metadata.passiveEvent()) return false;
        if ("AI".equalsIgnoreCase(activity) && !metadata.aiFeature()) return false;
        return true;
    }

    private List<FeatureTrendPointResponse> trend(Map<LocalDate, Long> counts, LocalDate startDate, long days) {
        List<FeatureTrendPointResponse> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            points.add(new FeatureTrendPointResponse(date, counts.getOrDefault(date, 0L)));
        }
        return points;
    }

    private FeatureUsageUserResponse userResponse(User user, UserAggregate aggregate) {
        if (user == null || aggregate == null) return null;
        return new FeatureUsageUserResponse(user.getId(), user.getEmail(), maskEmail(user.getEmail()), user.getPlan(), tradeRepository.countByUser(user), aggregate.distinctFeatures(), aggregate.activeActions(), aggregate.passiveEvents(), aggregate.aiGenerations(), aggregate.totalEvents(), aggregate.lastActiveAt(), engagementLevel(aggregate), aggregate.featureCounts());
    }

    private List<FeatureUsageInsightResponse> insights(List<FeatureAdoptionResponse> features, Aggregation aggregation) {
        List<FeatureUsageInsightResponse> insights = new ArrayList<>();
        features.stream().filter(feature -> feature.totalUsage() > 0 && feature.activeUserAdoptionRate() < 25 && !feature.passiveEvent()).max(Comparator.comparingLong(FeatureAdoptionResponse::totalUsage)).ifPresent(feature -> insights.add(new FeatureUsageInsightResponse("HIGH_VOLUME_LOW_ADOPTION", feature.feature(), feature.displayName() + " has concentrated usage", feature.uniqueUsers() + " users generated " + feature.totalUsage() + " events.", "High volume from a small user set can hide low discovery.", "Review navigation visibility, empty-state guidance, and first-run completion.", "Medium", "High")));
        features.stream().filter(feature -> feature.activeUserAdoptionRate() >= 50 && feature.repeatUsageRate() < 35).findFirst().ifPresent(feature -> insights.add(new FeatureUsageInsightResponse("HIGH_ADOPTION_LOW_REPEAT", feature.feature(), feature.displayName() + " has broad reach but low repeat usage", feature.activeUserAdoptionRate() + "% adoption with " + feature.repeatUsageRate() + "% repeat usage.", "Users may be trying the workflow without building a habit.", "Inspect the second-use experience and follow-up CTAs.", "Medium", "Medium")));
        features.stream().filter(feature -> feature.eventGrowthPercent() <= -25 && feature.previousPeriodEvents() >= 5).min(Comparator.comparingDouble(FeatureAdoptionResponse::eventGrowthPercent)).ifPresent(feature -> insights.add(new FeatureUsageInsightResponse("RAPID_DECLINE", feature.feature(), feature.displayName() + " is declining", "Events changed " + feature.eventGrowthPercent() + "% vs. previous period.", "Declining usage may indicate regressions or weaker discovery.", "Compare recent releases, navigation changes, and errors for this feature.", "Medium", "High")));
        features.stream().filter(feature -> feature.passiveEvent() && feature.totalUsage() > 0).max(Comparator.comparingLong(FeatureAdoptionResponse::totalUsage)).ifPresent(feature -> insights.add(new FeatureUsageInsightResponse("PASSIVE_EVENT_DOMINATES_USAGE", feature.feature(), feature.displayName() + " is mostly passive telemetry", feature.totalUsage() + " passive events in this range.", "Page views and cached reads should not be mistaken for high engagement.", "Use active-action filters for product prioritization.", "High", "Medium")));
        FeatureAdoptionResponse playbook = features.stream().filter(feature -> feature.feature() == FeatureKey.PLAYBOOK).findFirst().orElse(null);
        if (playbook != null && playbook.totalUsage() > 0 && playbook.activeUserAdoptionRate() < 30) {
            insights.add(new FeatureUsageInsightResponse("PRO_ONLY_UNDERUSED", FeatureKey.PLAYBOOK, "Playbook adoption is still early", playbook.uniqueUsers() + " active users used Playbook in this range.", "Playbook is a strategic retention workflow and needs strong onboarding.", "Review first-setup creation and incomplete setup recovery.", "Medium", "High"));
        }
        if (insights.isEmpty()) {
            insights.add(new FeatureUsageInsightResponse("INSUFFICIENT_DATA", null, "Usage sample is still sparse", aggregation.activeUserIds().size() + " active users in this range.", "Small samples can make rankings noisy.", "Keep collecting events or widen the date range.", "Low", "Low"));
        }
        return insights.stream().limit(5).toList();
    }

    private List<FeatureUsageDataQualityIssueResponse> dataQuality(List<FeatureAdoptionResponse> features) {
        List<FeatureUsageDataQualityIssueResponse> issues = new ArrayList<>();
        List<FeatureAdoptionResponse> unknown = features.stream().filter(feature -> !feature.knownFeature() && feature.totalUsage() > 0).toList();
        if (!unknown.isEmpty()) {
            issues.add(new FeatureUsageDataQualityIssueResponse("UNKNOWN_FEATURE_EVENTS", unknown.stream().mapToLong(FeatureAdoptionResponse::totalUsage).sum(), null, null, "Add metadata and database constraint support for every emitted feature key."));
        }
        List<FeatureAdoptionResponse> blank = features.stream().filter(feature -> feature.displayName() == null || feature.displayName().isBlank()).toList();
        if (!blank.isEmpty()) {
            issues.add(new FeatureUsageDataQualityIssueResponse("BLANK_FEATURE_DISPLAY_NAME", blank.size(), null, null, "Complete FeatureUsageMetadataRegistry mappings."));
        }
        return issues;
    }

    private FeatureUsageMetadataResponse metadataResponse(FeatureUsageMetadata metadata) {
        return new FeatureUsageMetadataResponse(metadata.key(), metadata.displayName(), metadata.shortName(), metadata.description(), metadata.category(), metadata.eventType(), metadata.coreFeature(), metadata.aiFeature(), metadata.paidAction(), metadata.passiveEvent(), metadata.sortOrder(), metadata.iconKey(), metadata.colorKey());
    }

    private FilterWindow window(String range, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String normalized = range == null || range.isBlank() ? "LAST_30_DAYS" : range.trim().toUpperCase(Locale.ROOT);
        if ("LAST_7_DAYS".equals(normalized)) {
            start = today.minusDays(6);
            end = today;
        } else if ("LAST_90_DAYS".equals(normalized)) {
            start = today.minusDays(89);
            end = today;
        } else if ("CURRENT_MONTH".equals(normalized)) {
            start = today.withDayOfMonth(1);
            end = today;
        } else if ("PREVIOUS_MONTH".equals(normalized)) {
            LocalDate previous = today.minusMonths(1);
            start = previous.withDayOfMonth(1);
            end = previous.withDayOfMonth(previous.lengthOfMonth());
        } else if ("CUSTOM".equals(normalized) && startDate != null && endDate != null) {
            start = startDate;
            end = endDate;
        } else {
            start = today.minusDays(29);
            end = today;
        }
        if (end.isBefore(start)) throw new IllegalArgumentException("endDate must be on or after startDate.");
        return new FilterWindow(start, end, ChronoUnit.DAYS.between(start, end) + 1);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return prefix + "***@" + parts[1];
    }

    private String engagementLevel(UserAggregate aggregate) {
        if (aggregate.aiGenerations() >= 5 || aggregate.activeActions() >= 15) return "High engagement";
        if (aggregate.activeActions() >= 5 || aggregate.distinctFeatures() >= 3) return "Engaged";
        if (aggregate.totalEvents() > 0) return "Light usage";
        return "Inactive";
    }

    private String healthStatus(double adoption, double repeat, double growth, long events, long activeUsers) {
        if (events == 0 || activeUsers < MIN_HEALTH_SAMPLE_USERS) return "INSUFFICIENT_DATA";
        if (growth <= DECLINING_GROWTH_THRESHOLD) return "DECLINING";
        if (adoption >= HIGH_ADOPTION_THRESHOLD) return "HIGH_ADOPTION";
        if (adoption >= HEALTHY_ADOPTION_THRESHOLD && repeat >= HEALTHY_REPEAT_THRESHOLD) return "HEALTHY";
        if (adoption < NEEDS_ATTENTION_ADOPTION_THRESHOLD && events >= 10) return "NEEDS_ATTENTION";
        return "EARLY";
    }

    private String statusLabel(String healthStatus) {
        return switch (healthStatus) {
            case "HIGH_ADOPTION" -> "High Adoption";
            case "HEALTHY" -> "Healthy";
            case "NEEDS_ATTENTION" -> "Needs Attention";
            case "DECLINING" -> "Declining";
            case "INSUFFICIENT_DATA" -> "Insufficient Data";
            default -> "Early";
        };
    }

    private FeatureUsageComparisonResponse comparison(long current, long previous, double percentagePointDelta) {
        String direction;
        if (current > previous) direction = previous == 0 ? "NEW" : "UP";
        else if (current < previous) direction = "DOWN";
        else direction = "FLAT";
        return new FeatureUsageComparisonResponse(current, previous, growth(current, previous), round(percentagePointDelta), direction);
    }

    private double engagementScore(double adoption, double repeat, double eventsPerUser, double growth) {
        double cappedEvents = Math.min(eventsPerUser, 10) * 10;
        double normalizedGrowth = Math.max(-100, Math.min(100, growth));
        return round(adoption * 0.35 + repeat * 0.30 + cappedEvents * 0.20 + Math.max(0, normalizedGrowth) * 0.15);
    }

    private double opportunityScore(FeatureUsageMetadata metadata, double adoption, double repeat, double growth) {
        double strategic = metadata.coreFeature() || metadata.aiFeature() ? 100 : 50;
        double lowAdoption = Math.max(0, 100 - adoption);
        double lowRepeat = Math.max(0, 100 - repeat);
        double decline = Math.max(0, -growth);
        return round(lowAdoption * 0.35 + strategic * 0.25 + lowRepeat * 0.20 + decline * 0.10);
    }

    private double growth(long current, long previous) {
        if (previous == 0) return current == 0 ? 0 : 100;
        return round((double) (current - previous) / previous * 100);
    }

    private long firstHalf(List<FeatureTrendPointResponse> points) {
        return points.stream().limit(points.size() / 2).mapToLong(FeatureTrendPointResponse::count).sum();
    }

    private long secondHalf(List<FeatureTrendPointResponse> points) {
        return points.stream().skip(points.size() / 2).mapToLong(FeatureTrendPointResponse::count).sum();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageService(final FeatureUsageRepository featureUsageRepository, final UserRepository userRepository, final TradeRepository tradeRepository, final AggregateCacheService aggregateCacheService, final PlatformTransactionManager transactionManager, final FeatureUsageMetadataRegistry metadataRegistry) {
        this.featureUsageRepository = featureUsageRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.aggregateCacheService = aggregateCacheService;
        this.metadataRegistry = metadataRegistry;
        this.telemetryTransactionTemplate = new TransactionTemplate(transactionManager);
        this.telemetryTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private record FilterWindow(LocalDate startDate, LocalDate endDate, long days) {}
    private ActivationFunnelMetricsResponse activationFunnelMetrics(FilterWindow window, PlanType plan, boolean excludeAdmins) {
        LocalDateTime start = window.startDate().atStartOfDay();
        LocalDateTime end = window.endDate().plusDays(1).atStartOfDay();
        List<FeatureUsageEvent> events = featureUsageRepository.findFunnelEventsBetween(start, end, plan, excludeAdmins, false, UserRole.ADMIN);
        Map<Long, ActivationJourney> journeys = new HashMap<>();
        for (FeatureUsageEvent event : events) {
            FeatureUsageAction action = event.getAction();
            if (action == null) {
                continue;
            }
            ActivationJourney journey = journeys.computeIfAbsent(event.getUserId(), ignored -> new ActivationJourney());
            LocalDateTime occurredAt = event.getOccurredAt() == null ? event.getCreatedAt() : event.getOccurredAt();
            switch (action) {
                case USER_SIGNED_UP -> journey.signupAt = earliest(journey.signupAt, occurredAt);
                case FIRST_IMPORT_COMPLETED -> journey.firstImportAt = earliest(journey.firstImportAt, occurredAt);
                case FIRST_INSIGHT_GENERATED -> journey.firstInsightAt = earliest(journey.firstInsightAt, occurredAt);
                case SECOND_AI_FEATURE_USED -> journey.secondAiFeatureAt = earliest(journey.secondAiFeatureAt, occurredAt);
                case PRO_CONVERSION, SUBSCRIPTION_ACTIVATED -> journey.proConversionAt = earliest(journey.proConversionAt, occurredAt);
                default -> {
                }
            }
        }

        long signedUp = journeys.values().stream().filter(journey -> journey.signupAt != null).count();
        long imported = journeys.values().stream().filter(journey -> journey.firstImportAt != null).count();
        long firstInsight = journeys.values().stream().filter(journey -> journey.firstInsightAt != null).count();
        long secondAi = journeys.values().stream().filter(journey -> journey.secondAiFeatureAt != null).count();
        long proConversions = journeys.values().stream().filter(journey -> journey.proConversionAt != null).count();

        List<Long> signupToImport = journeys.values().stream()
                .filter(journey -> journey.signupAt != null && journey.firstImportAt != null && !journey.firstImportAt.isBefore(journey.signupAt))
                .map(journey -> ChronoUnit.MINUTES.between(journey.signupAt, journey.firstImportAt))
                .sorted()
                .toList();
        List<Long> importToInsight = journeys.values().stream()
                .filter(journey -> journey.firstImportAt != null && journey.firstInsightAt != null && !journey.firstInsightAt.isBefore(journey.firstImportAt))
                .map(journey -> ChronoUnit.MINUTES.between(journey.firstImportAt, journey.firstInsightAt))
                .sorted()
                .toList();
        List<Long> signupToInsight = journeys.values().stream()
                .filter(journey -> journey.signupAt != null && journey.firstInsightAt != null && !journey.firstInsightAt.isBefore(journey.signupAt))
                .map(journey -> ChronoUnit.MINUTES.between(journey.signupAt, journey.firstInsightAt))
                .sorted()
                .toList();

        return new ActivationFunnelMetricsResponse(
                signedUp,
                imported,
                firstInsight,
                secondAi,
                proConversions,
                median(signupToImport),
                median(importToInsight),
                median(signupToInsight),
                rate(imported, signedUp),
                rate(firstInsight, imported),
                rate(secondAi, firstInsight),
                rate(proConversions, firstInsight)
        );
    }

    private LocalDateTime earliest(LocalDateTime current, LocalDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private Double median(List<Long> values) {
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return round(values.get(middle));
        }
        return round((values.get(middle - 1) + values.get(middle)) / 2.0);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : round((double) numerator / denominator * 100);
    }

    private record Aggregation(Map<FeatureKey, FeatureAggregate> byFeature, Map<FeatureKey, FeatureAggregate> previousByFeature, Map<Long, UserAggregate> byUser, Set<Long> activeUserIds, long totalEligibleUsers, FeatureUsageCategory category, FeatureUsageEventType eventType, String activity, PlanType plan) {
        long previousActiveUserCount() {
            return previousByFeature.values().stream()
                    .flatMap(aggregate -> aggregate.users().stream())
                    .distinct()
                    .count();
        }
    }

    private static final class ActivationJourney {
        private LocalDateTime signupAt;
        private LocalDateTime firstImportAt;
        private LocalDateTime firstInsightAt;
        private LocalDateTime secondAiFeatureAt;
        private LocalDateTime proConversionAt;
    }

    private static final class FeatureAggregate {
        private final Set<Long> users = new HashSet<>();
        private long totalEvents;
        private long activeEvents;
        private long passiveEvents;
        private long aiGenerations;
        private long repeatUsers;
        private LocalDateTime earliest;
        private LocalDateTime latest;

        static FeatureAggregate empty() { return new FeatureAggregate(); }

        void add(Long userId, FeatureUsageMetadata metadata, long count, long passiveCount, long aiCount, LocalDateTime first, LocalDateTime last, long activeDays) {
            users.add(userId);
            totalEvents += count;
            long normalizedPassiveCount = passiveCount == 0 && metadata.passiveEvent() && aiCount == 0 ? count : passiveCount;
            passiveEvents += normalizedPassiveCount;
            activeEvents += Math.max(0, count - normalizedPassiveCount);
            aiGenerations += aiCount;
            if (activeDays > 1) repeatUsers++;
            if (first != null && (earliest == null || first.isBefore(earliest))) earliest = first;
            if (last != null && (latest == null || last.isAfter(latest))) latest = last;
        }

        long uniqueUsers() { return users.size(); }
        long totalEvents() { return totalEvents; }
        long activeEvents() { return activeEvents; }
        long passiveEvents() { return passiveEvents; }
        long aiGenerations() { return aiGenerations; }
        long repeatUsers() { return repeatUsers; }
        Set<Long> users() { return users; }
    }

    private static final class UserAggregate {
        private final Map<FeatureKey, Long> featureCounts = new EnumMap<>(FeatureKey.class);
        private long activeActions;
        private long passiveEvents;
        private long aiGenerations;
        private LocalDateTime lastActiveAt;

        void add(FeatureUsageMetadata metadata, long count, long passiveCount, long aiCount, LocalDateTime last) {
            featureCounts.merge(metadata.key(), count, Long::sum);
            long normalizedPassiveCount = passiveCount == 0 && metadata.passiveEvent() && aiCount == 0 ? count : passiveCount;
            passiveEvents += normalizedPassiveCount;
            activeActions += Math.max(0, count - normalizedPassiveCount);
            aiGenerations += aiCount;
            if (last != null && (lastActiveAt == null || last.isAfter(lastActiveAt))) lastActiveAt = last;
        }

        long distinctFeatures() { return featureCounts.size(); }
        long activeActions() { return activeActions; }
        long passiveEvents() { return passiveEvents; }
        long aiGenerations() { return aiGenerations; }
        long totalEvents() { return activeActions + passiveEvents; }
        LocalDateTime lastActiveAt() { return lastActiveAt; }
        Map<FeatureKey, Long> featureCounts() { return Map.copyOf(featureCounts); }
    }
}
