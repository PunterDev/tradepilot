package com.tradepilot.featureusage;

import com.tradepilot.entity.User;
import com.tradepilot.featureusage.dto.*;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tradepilot.service.AggregateCacheService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureUsageService {
    private final FeatureUsageRepository featureUsageRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final AggregateCacheService aggregateCacheService;

    @Transactional
    public void track(User user, FeatureKey feature) {
        track(user, feature, null);
    }

    @Transactional
    public void track(User user, FeatureKey feature, LocalDateTime createdAt) {
        if (user == null || feature == null) {
            return;
        }
        FeatureUsageEvent saved = featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(user.getId())
                .feature(feature)
                .createdAt(createdAt)
                .build());
        log.trace("FeatureUsageEvent saved. userId={}, feature={}, eventId={}",
                saved.getUserId(),
                saved.getFeature(),
                saved.getId()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageSummaryResponse summary() {
        return aggregateCacheService.get("feature-usage-summary", Duration.ofSeconds(120), this::buildSummary);
    }

    private FeatureUsageSummaryResponse buildSummary() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        long totalUsers = userRepository.count();
        long activeUsers30d = featureUsageRepository.countActiveUsersSince(start);
        Map<FeatureKey, Object[]> featureCounts = featureUsageRepository.countByFeatureSince(start).stream()
                .collect(Collectors.toMap(row -> (FeatureKey) row[0], row -> row));
        List<FeatureAdoptionResponse> features = Arrays.stream(FeatureKey.values())
                .map(feature -> {
                    Object[] row = featureCounts.get(feature);
                    long totalUsage = row == null ? 0 : (Long) row[1];
                    long uniqueUsers = row == null ? 0 : (Long) row[2];
                    return FeatureAdoptionResponse.builder()
                            .feature(feature)
                            .totalUsage(totalUsage)
                            .uniqueUsers(uniqueUsers)
                            .overallAdoptionRate(totalUsers == 0 ? 0 : round((double) uniqueUsers / totalUsers * 100))
                            .activeUserAdoptionRate(activeUsers30d == 0 ? 0 : round((double) uniqueUsers / activeUsers30d * 100))
                            .build();
                })
                .toList();

        FeatureKey mostUsed = features.stream()
                .max(Comparator.comparingLong(FeatureAdoptionResponse::getTotalUsage))
                .map(FeatureAdoptionResponse::getFeature)
                .orElse(null);
        FeatureKey leastUsed = features.stream()
                .filter(feature -> feature.getTotalUsage() > 0)
                .min(Comparator.comparingLong(FeatureAdoptionResponse::getTotalUsage))
                .map(FeatureAdoptionResponse::getFeature)
                .orElse(null);

        return FeatureUsageSummaryResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers30d(activeUsers30d)
                .features(features)
                .mostUsedFeature(mostUsed)
                .leastUsedFeature(leastUsed)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FeatureUsageTrendsResponse trends() {
        LocalDate startDate = LocalDate.now().minusDays(29);
        LocalDateTime start = startDate.atStartOfDay();
        List<Object[]> rows = featureUsageRepository.countByDateAndFeatureSince(start);

        return FeatureUsageTrendsResponse.builder()
                .dailyCoach(trend(rows, FeatureKey.DAILY_COACH, startDate))
                .weeklyCoach(trend(rows, FeatureKey.WEEKLY_COACH, startDate))
                .tradingDna(trend(rows, FeatureKey.TRADING_DNA, startDate))
                .aiReview(trend(rows, FeatureKey.AI_REVIEW, startDate))
                .sessionReview(trend(rows, FeatureKey.SESSION_REVIEW, startDate))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FeatureUsageUserResponse> users() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        List<Object[]> topRows = featureUsageRepository.countByUserSince(start).stream()
                .limit(10)
                .toList();
        Map<Long, Long> totals = topRows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        List<Long> topUserIds = topRows.stream().map(row -> (Long) row[0]).toList();
        if (topUserIds.isEmpty()) {
            return List.of();
        }
        List<FeatureUsageEvent> events = featureUsageRepository.findByUserIdInAndCreatedAtGreaterThanEqual(topUserIds, start);
        Map<Long, Map<FeatureKey, Long>> byUser = events.stream()
                .collect(Collectors.groupingBy(
                        FeatureUsageEvent::getUserId,
                        Collectors.groupingBy(FeatureUsageEvent::getFeature, Collectors.counting())
                ));

        return userRepository.findAllById(topUserIds).stream()
                .map(user -> FeatureUsageUserResponse.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .plan(user.getPlan())
                        .trades(tradeRepository.countByUser(user))
                        .featureUsageCounts(byUser.getOrDefault(user.getId(), Map.of()))
                        .build())
                .sorted(Comparator.comparingLong((FeatureUsageUserResponse user) ->
                        totals.getOrDefault(user.getUserId(), 0L)).reversed())
                .toList();
    }

    private List<FeatureTrendPointResponse> trend(List<Object[]> rows, FeatureKey feature, LocalDate startDate) {
        Map<LocalDate, Long> counts = rows.stream()
                .filter(row -> row[1] == feature)
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> ((Number) row[2]).longValue()));

        List<FeatureTrendPointResponse> points = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            points.add(FeatureTrendPointResponse.builder()
                    .date(date)
                    .count(counts.getOrDefault(date, 0L))
                    .build());
        }
        return points;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
