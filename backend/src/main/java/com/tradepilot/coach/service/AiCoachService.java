package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.coach.dto.CoachGenerateRequest;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.service.TradingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiCoachService {

    private final AiCoachSnapshotRepository repository;
    private final CoachSnapshotMapper mapper;
    private final AiCoachAsyncWorker asyncWorker;
    private final TradingAccountService tradingAccountService;
    private final UsageLimitService usageLimitService;
    private final FeatureAccessService featureAccessService;
    private final ObservabilityService observabilityService;
    private final AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository;
    private final ObjectMapper objectMapper;

    public CoachSnapshotDto getDailyCoach(
            User user,
            Long accountId,
            LocalDate date
    ) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.DAILY_COACH);
        validateOwnedAccount(user, accountId);

        AiCoachSnapshot snapshot =
                findOrCreateCachedSnapshot(
                        user,
                        userId,
                        accountId,
                        CoachSnapshotType.DAILY,
                        date,
                        date,
                        false
                );

        return enrichWithTopLeak(mapper.toDto(snapshot), userId, accountId);
    }

    public CoachSnapshotDto getWeeklyCoach(
            User user,
            Long accountId,
            LocalDate weekStart
    ) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.WEEKLY_COACH);
        validateOwnedAccount(user, accountId);
        LocalDate weekEnd = weekStart.plusDays(6);

        AiCoachSnapshot snapshot =
                findOrCreateCachedSnapshot(
                        user,
                        userId,
                        accountId,
                        CoachSnapshotType.WEEKLY,
                        weekStart,
                        weekEnd,
                        false
                );

        return mapper.toDto(snapshot);
    }

    public CoachSnapshotDto generateDailyCoach(
            User user,
            CoachGenerateRequest request
    ) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.DAILY_COACH);
        validateOwnedAccount(user, request.getAccountId());
        LocalDate date = request.getPeriodStart();

        AiCoachSnapshot snapshot =
                findOrCreateCachedSnapshot(
                        user,
                        userId,
                        request.getAccountId(),
                        CoachSnapshotType.DAILY,
                        date,
                        date,
                        Boolean.TRUE.equals(request.getForceRefresh())
                );

        return enrichWithTopLeak(mapper.toDto(snapshot), userId, request.getAccountId());
    }

    public CoachSnapshotDto generateWeeklyCoach(
            User user,
            CoachGenerateRequest request
    ) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.WEEKLY_COACH);
        validateOwnedAccount(user, request.getAccountId());
        LocalDate weekStart = request.getPeriodStart();

        LocalDate weekEnd =
                request.getPeriodEnd() != null
                        ? request.getPeriodEnd()
                        : weekStart.plusDays(6);

        AiCoachSnapshot snapshot =
                findOrCreateCachedSnapshot(
                        user,
                        userId,
                        request.getAccountId(),
                        CoachSnapshotType.WEEKLY,
                        weekStart,
                        weekEnd,
                        Boolean.TRUE.equals(request.getForceRefresh())
                );

        return mapper.toDto(snapshot);
    }

    public List<CoachSnapshotDto> getSnapshots(
            User user,
            Long accountId,
            CoachSnapshotType type
    ) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, type == CoachSnapshotType.DAILY ? Feature.DAILY_COACH : Feature.WEEKLY_COACH);
        validateOwnedAccount(user, accountId);

        return repository
                .findTop20ByUserIdAndAccountIdAndSnapshotTypeOrderByPeriodStartDesc(
                        userId,
                        accountId,
                        type
                )
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    private AiCoachSnapshot findOrCreateCachedSnapshot(
            User user,
            Long userId,
            Long accountId,
            CoachSnapshotType type,
            LocalDate periodStart,
            LocalDate periodEnd,
            boolean forceRefresh
    ) {
        Long normalizedAccountId = accountId == null ? 0L : accountId;

        AiCoachSnapshot existing =
                repository
                        .findTopByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                                userId,
                                normalizedAccountId,
                                type,
                                periodStart,
                                periodEnd
                        )
                        .orElse(null);

        if (!forceRefresh) {
            if (existing != null) {
                if (existing.getStatus() == CoachSnapshotStatus.COMPLETED
                        || existing.getStatus() == CoachSnapshotStatus.PROCESSING
                        || existing.getStatus() == CoachSnapshotStatus.PENDING) {
                    observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", true);
                    return existing;
                }
            }
        }
        observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", false);

        UsageType usageType = type == CoachSnapshotType.DAILY ? UsageType.DAILY_COACH : UsageType.WEEKLY_COACH;
        usageLimitService.enforceAiUsage(user, usageType);

        if (existing != null) {
            existing.setStatus(CoachSnapshotStatus.PENDING);
            existing.setErrorMessage(null);
            AiCoachSnapshot saved = repository.save(existing);
            usageLimitService.recordAiUsage(user, usageType, saved.getId());
            asyncWorker.generateAsync(saved.getId());
            return saved;
        }

        AiCoachSnapshot snapshot =
                AiCoachSnapshot.builder()
                        .userId(userId)
                        .accountId(normalizedAccountId)
                        .snapshotType(type)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .status(CoachSnapshotStatus.PENDING)
                        .build();

        AiCoachSnapshot saved = repository.save(snapshot);
        usageLimitService.recordAiUsage(user, usageType, saved.getId());

        asyncWorker.generateAsync(saved.getId());

        return saved;
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }

        return user.getId();
    }

    private CoachSnapshotDto enrichWithTopLeak(CoachSnapshotDto dto, Long userId, Long accountId) {
        topLeak(userId, accountId).ifPresent(dto::setTopLeakToWatch);
        return dto;
    }

    private Optional<LeakResultDto> topLeak(Long userId, Long accountId) {
        return leakDetectorSnapshotRepository.findTopByUserIdAndAccountIdOrderByUpdatedAtDesc(userId, accountId)
                .flatMap(snapshot -> {
                    try {
                        List<LeakResultDto> leaks = objectMapper.readValue(snapshot.getResultJson(), new TypeReference<>() {});
                        return leaks.stream()
                                .max(Comparator
                                        .comparing((LeakResultDto leak) -> leak.getSeverity() == null ? 0 : leak.getSeverity().ordinal())
                                        .thenComparing(leak -> leak.getConfidenceScore() == null ? 0 : leak.getConfidenceScore()));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
    }

    private void validateOwnedAccount(User user, Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("accountId is required");
        }

        tradingAccountService.getAccountForImport(user, accountId);
    }
}
