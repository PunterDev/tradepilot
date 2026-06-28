package com.tradepilot.retention;

import com.tradepilot.billing.PlanType;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AggregateCacheService;
import com.tradepilot.retention.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRetentionAnalyticsService {
    private final UserRepository userRepository;
    private final FeatureUsageRepository featureUsageRepository;
    private final AggregateCacheService aggregateCacheService;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public RetentionSummaryResponse summary() {
        log.info("Admin retention summary requested.");
        return aggregateCacheService.get("retention-summary", Duration.ofSeconds(120), this::buildSummary);
    }

    private RetentionSummaryResponse buildSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();
        LocalDateTime thirtyDaysAgo = today.minusDays(29).atStartOfDay();
        List<User> users = userRepository.findAll();
        List<FeatureUsageEvent> recentEvents = featureUsageRepository.findByCreatedAtGreaterThanEqual(thirtyDaysAgo);
        List<FeatureUsageEvent> retentionEvents = featureUsageRepository.findAll();
        log.trace("Retention summary aggregates loaded. totalUsers={}, recentEventCount={}", users.size(), recentEvents.size());

        long dau = featureUsageRepository.countActiveUsersBetween(todayStart, tomorrowStart);
        long wau = featureUsageRepository.countActiveUsersSince(sevenDaysAgo);
        long mau = featureUsageRepository.countActiveUsersSince(thirtyDaysAgo);
        Set<Long> dauUsers = activeUsersBetween(recentEvents, todayStart, tomorrowStart);
        Set<Long> wauUsers = activeUsersSince(recentEvents, sevenDaysAgo);
        Set<Long> mauUsers = activeUsersSince(recentEvents, thirtyDaysAgo);
        List<RetentionUserResponse> retentionUsers = buildUsers(users, recentEvents, thirtyDaysAgo);

        return RetentionSummaryResponse.builder()
                .totalUsers(users.size())
                .dau(dau)
                .wau(wau)
                .mau(mau)
                .dauMauRatio(mau == 0 ? 0 : round((double) dau / mau * 100))
                .newUsers30d(users.stream().filter(user -> !user.getCreatedAt().isBefore(thirtyDaysAgo)).count())
                .retention7d(retention(users, retentionEvents, 7))
                .retention30d(retention(users, retentionEvents, 30))
                .powerUsers(retentionUsers.stream().filter(user -> user.getStatus() == RetentionStatus.POWER_USER).count())
                .atRiskUsers(retentionUsers.stream().filter(user -> user.getStatus() == RetentionStatus.AT_RISK).count())
                .inactiveUsers(retentionUsers.stream().filter(user -> user.getStatus() == RetentionStatus.INACTIVE).count())
                .freeUsers(segment(users, dauUsers, wauUsers, mauUsers, user -> user.getPlan() == PlanType.FREE))
                .proUsers(segment(users, dauUsers, wauUsers, mauUsers, user -> user.getPlan() == PlanType.PRO))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public RetentionTrendsResponse trends() {
        log.info("Admin retention trends requested.");
        return aggregateCacheService.get("retention-trends:rolling-30", Duration.ofMinutes(5), this::buildTrends);
    }

    private RetentionTrendsResponse buildTrends() {
        LocalDate startDate = LocalDate.now().minusDays(29);
        LocalDate earliestWindowDate = startDate.minusDays(29);
        LocalDateTime start = earliestWindowDate.atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        List<Object[]> activityDays = featureUsageRepository.distinctUserActivityDays(start, end);
        log.trace("Retention trends aggregate loaded. start={}, end={}, activityDayRows={}", start, end, activityDays.size());
        return RetentionTrendsResponse.builder()
                .dailyActiveUsers(activeTrend(startDate, 1, activityDays))
                .weeklyActiveUsers(activeTrend(startDate, 7, activityDays))
                .monthlyActiveUsers(activeTrend(startDate, 30, activityDays))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<RetentionCohortResponse> cohorts() {
        List<User> users = userRepository.findAll();
        List<FeatureUsageEvent> events = featureUsageRepository.findAll();
        NavigableMap<YearMonth, List<User>> byMonth = users.stream()
                .collect(Collectors.groupingBy(user -> YearMonth.from(user.getCreatedAt()), TreeMap::new, Collectors.toList()));

        return byMonth.descendingMap().entrySet().stream()
                .map(entry -> {
                    List<User> cohortUsers = entry.getValue();
                    return RetentionCohortResponse.builder()
                            .cohortMonth(entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                            .users(cohortUsers.size())
                            .retained7d(retention(cohortUsers, events, 7))
                            .retained30d(retention(cohortUsers, events, 30))
                            .build();
                })
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<RetentionUserResponse> users() {
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(29).atStartOfDay();
        return buildUsers(userRepository.findAll(), featureUsageRepository.findByCreatedAtGreaterThanEqual(thirtyDaysAgo), thirtyDaysAgo);
    }

    private List<RetentionUserResponse> buildUsers(List<User> users, List<FeatureUsageEvent> events, LocalDateTime thirtyDaysAgo) {
        Map<Long, List<FeatureUsageEvent>> eventsByUser = events.stream()
                .collect(Collectors.groupingBy(FeatureUsageEvent::getUserId));
        LocalDateTime now = LocalDateTime.now();

        return users.stream()
                .map(user -> {
                    List<FeatureUsageEvent> userEvents = eventsByUser.getOrDefault(user.getId(), List.of());
                    LocalDateTime lastActive = userEvents.stream()
                            .map(FeatureUsageEvent::getCreatedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    long activityCount30d = userEvents.stream()
                            .filter(event -> !event.getCreatedAt().isBefore(thirtyDaysAgo))
                            .count();
                    long daysSinceActivity = daysSince(lastActive == null ? user.getCreatedAt() : lastActive, now);
                    RetentionStatus status = status(activityCount30d, lastActive, user.getCreatedAt(), now);
                    return RetentionUserResponse.of(
                            user.getId(),
                            user.getEmail(),
                            user.getName(),
                            user.getPlan(),
                            lastActive,
                            activityCount30d,
                            daysSinceActivity,
                            status
                    );
                })
                .sorted(Comparator
                        .comparing((RetentionUserResponse user) -> user.getStatus() == RetentionStatus.POWER_USER ? 0 : user.getStatus() == RetentionStatus.ACTIVE ? 1 : user.getStatus() == RetentionStatus.AT_RISK ? 2 : 3)
                        .thenComparing(RetentionUserResponse::getActivityCount30d, Comparator.reverseOrder()))
                .toList();
    }

    private RetentionStatus status(long activityCount30d, LocalDateTime lastActive, LocalDateTime createdAt, LocalDateTime now) {
        if (activityCount30d >= 10) {
            return RetentionStatus.POWER_USER;
        }
        LocalDateTime activityAnchor = lastActive == null ? createdAt : lastActive;
        long days = daysSince(activityAnchor, now);
        if (days >= 30) {
            return RetentionStatus.INACTIVE;
        }
        if (days >= 7) {
            return RetentionStatus.AT_RISK;
        }
        return RetentionStatus.ACTIVE;
    }

    private long daysSince(LocalDateTime dateTime, LocalDateTime now) {
        return Math.max(0, Duration.between(dateTime, now).toDays());
    }

    private RetentionPlanSegmentResponse segment(
            List<User> users,
            Set<Long> dauUsers,
            Set<Long> wauUsers,
            Set<Long> mauUsers,
            Predicate<User> filter
    ) {
        Set<Long> userIds = users.stream().filter(filter).map(User::getId).collect(Collectors.toSet());
        return RetentionPlanSegmentResponse.builder()
                .dau(dauUsers.stream().filter(userIds::contains).count())
                .wau(wauUsers.stream().filter(userIds::contains).count())
                .mau(mauUsers.stream().filter(userIds::contains).count())
                .build();
    }

    private List<RetentionTrendPointResponse> activeTrend(LocalDate startDate, int trailingDays, List<Object[]> activityDays) {
        List<RetentionTrendPointResponse> points = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDate windowStart = date.minusDays(trailingDays - 1L);
            points.add(RetentionTrendPointResponse.builder()
                    .date(date)
                    .count(countDistinctUsers(activityDays, windowStart, date))
                    .build());
        }
        return points;
    }

    private long countDistinctUsers(List<Object[]> activityDays, LocalDate start, LocalDate endInclusive) {
        return activityDays.stream()
                .filter(row -> {
                    LocalDate day = localDate(row[0]);
                    return !day.isBefore(start) && !day.isAfter(endInclusive);
                })
                .map(row -> (Long) row[1])
                .collect(Collectors.toSet())
                .size();
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

    private Set<Long> activeUsersSince(List<FeatureUsageEvent> events, LocalDateTime start) {
        return events.stream()
                .filter(event -> !event.getCreatedAt().isBefore(start))
                .map(FeatureUsageEvent::getUserId)
                .collect(Collectors.toSet());
    }

    private Set<Long> activeUsersBetween(List<FeatureUsageEvent> events, LocalDateTime start, LocalDateTime end) {
        return events.stream()
                .filter(event -> !event.getCreatedAt().isBefore(start) && event.getCreatedAt().isBefore(end))
                .map(FeatureUsageEvent::getUserId)
                .collect(Collectors.toSet());
    }

    private double retention(List<User> users, List<FeatureUsageEvent> events, int days) {
        LocalDateTime now = LocalDateTime.now();
        List<User> eligible = users.stream()
                .filter(user -> !user.getCreatedAt().plusDays(days).isAfter(now))
                .toList();
        if (eligible.isEmpty()) {
            return 0;
        }
        Map<Long, User> eligibleById = eligible.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Set<Long> retained = events.stream()
                .filter(event -> {
                    User user = eligibleById.get(event.getUserId());
                    return user != null && !event.getCreatedAt().isBefore(user.getCreatedAt().plusDays(days));
                })
                .map(FeatureUsageEvent::getUserId)
                .collect(Collectors.toSet());
        return round((double) retained.size() / eligible.size() * 100);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
