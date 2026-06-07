package com.tradepilot.billing;

import com.tradepilot.entity.User;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.service.AggregateCacheService;
import com.tradepilot.service.PerformanceCacheInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageLimitService {
    private static final int UNLIMITED = -1;
    private static final Duration BILLING_USAGE_TTL = Duration.ofSeconds(45);

    private final AiUsageEventRepository usageEventRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AggregateCacheService aggregateCacheService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;

    public void enforceAccountCreation(User user) {
        if (plan(user) == PlanType.PRO) {
            return;
        }

        long count = tradingAccountRepository.countByUser(user);
        if (count >= 1) {
            log.warn("Account creation plan limit exceeded. userId={}, plan={}, existingAccounts={}",
                    user.getId(), plan(user), count);
            throw limit("You have reached your free plan limit for trading accounts.");
        }
    }

    public void enforceTradeImport(User user, int incomingRows) {
        if (plan(user) == PlanType.PRO) {
            return;
        }

        long existing = tradeRepository.countByUser(user);
        if (existing + Math.max(incomingRows, 0) > 100) {
            log.warn("Trade import plan limit exceeded. userId={}, plan={}, existingTrades={}, incomingRows={}",
                    user.getId(), plan(user), existing, incomingRows);
            throw limit("You have reached your free plan limit for imported trades.");
        }
    }

    public void enforceAiUsage(User user, UsageType usageType) {
        int limit = limits(plan(user)).get(usageType);
        if (limit == UNLIMITED) {
            return;
        }

        Period period = period(user);
        long used = usageEventRepository.countByUserIdAndUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(),
                usageType,
                period.start(),
                period.end()
        );

        if (used >= limit) {
            log.warn("AI usage plan limit exceeded. userId={}, usageType={}, used={}, limit={}",
                    user.getId(), usageType, used, limit);
            throw limit(messageFor(usageType));
        }
    }

    @Transactional
    public void recordAiUsage(User user, UsageType usageType, Long relatedResourceId) {
        usageEventRepository.save(AiUsageEvent.builder()
                .userId(user.getId())
                .usageType(usageType)
                .relatedResourceId(relatedResourceId)
                .build());
        log.info("AI usage recorded. userId={}, usageType={}, relatedResourceId={}",
                user.getId(), usageType, relatedResourceId);
        cacheInvalidationService.invalidateBillingUsage(user.getId());
    }

    public BillingUsageResponse getUsage(User user) {
        return aggregateCacheService.get(
                usageCacheKey(user),
                BILLING_USAGE_TTL,
                () -> buildUsage(user)
        );
    }

    public void invalidateUsageCache(Long userId) {
        cacheInvalidationService.invalidateBillingUsage(userId);
    }

    private BillingUsageResponse buildUsage(User user) {
        PlanType plan = plan(user);
        log.trace("Billing usage requested. userId={}, plan={}", user.getId(), plan);
        Map<UsageType, Integer> aiLimits = limits(plan);
        Period period = period(user);

        Map<String, Integer> limits = new LinkedHashMap<>();
        Map<String, Integer> used = new LinkedHashMap<>();
        Map<String, Integer> remaining = new LinkedHashMap<>();

        limits.put("TRADING_ACCOUNTS", plan == PlanType.PRO ? UNLIMITED : 1);
        limits.put("IMPORTED_TRADES", plan == PlanType.PRO ? UNLIMITED : 100);
        used.put("TRADING_ACCOUNTS", Math.toIntExact(Math.min(Integer.MAX_VALUE, tradingAccountRepository.countByUser(user))));
        used.put("IMPORTED_TRADES", Math.toIntExact(Math.min(Integer.MAX_VALUE, tradeRepository.countByUser(user))));

        for (UsageType type : UsageType.values()) {
            int limit = aiLimits.get(type);
            int count = Math.toIntExact(Math.min(
                    Integer.MAX_VALUE,
                    usageEventRepository.countByUserIdAndUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            user.getId(),
                            type,
                            period.start(),
                            period.end()
                    )
            ));
            limits.put(type.name(), limit);
            used.put(type.name(), count);
        }

        limits.forEach((key, limit) -> {
            int count = used.getOrDefault(key, 0);
            remaining.put(key, limit == UNLIMITED ? UNLIMITED : Math.max(0, limit - count));
        });

        BillingUsageResponse response = new BillingUsageResponse();
        response.setPlan(plan);
        response.setSubscriptionStatus(user.getSubscriptionStatus() == null ? SubscriptionStatus.NONE : user.getSubscriptionStatus());
        response.setLimits(limits);
        response.setUsageThisMonth(used);
        response.setRemaining(remaining);
        response.setResetDate(period.end().toLocalDate().toString());
        return response;
    }

    private String usageCacheKey(User user) {
        return "billing-usage:" + user.getId() + ":" + plan(user) + ":" + subscriptionStatus(user) + ":" + period(user).end();
    }

    private SubscriptionStatus subscriptionStatus(User user) {
        return user.getSubscriptionStatus() == null ? SubscriptionStatus.NONE : user.getSubscriptionStatus();
    }

    private Map<UsageType, Integer> limits(PlanType plan) {
        Map<UsageType, Integer> limits = new EnumMap<>(UsageType.class);
        if (plan == PlanType.PRO) {
            limits.put(UsageType.AI_TRADE_REVIEW, 100);
            limits.put(UsageType.DAILY_COACH, 30);
            limits.put(UsageType.WEEKLY_COACH, 10);
            limits.put(UsageType.TRADING_DNA, 10);
            limits.put(UsageType.SESSION_REVIEW, 30);
            return limits;
        }

        limits.put(UsageType.AI_TRADE_REVIEW, 5);
        limits.put(UsageType.DAILY_COACH, 3);
        limits.put(UsageType.WEEKLY_COACH, 1);
        limits.put(UsageType.TRADING_DNA, 1);
        limits.put(UsageType.SESSION_REVIEW, 3);
        return limits;
    }

    private PlanType plan(User user) {
        return user.getPlan() == null ? PlanType.FREE : user.getPlan();
    }

    private Period period(User user) {
        if (user.getCurrentPeriodStart() != null && user.getCurrentPeriodEnd() != null) {
            return new Period(user.getCurrentPeriodStart(), user.getCurrentPeriodEnd());
        }

        YearMonth current = YearMonth.now();
        return new Period(current.atDay(1).atStartOfDay(), current.plusMonths(1).atDay(1).atStartOfDay());
    }

    private PlanLimitExceededException limit(String message) {
        return new PlanLimitExceededException(message);
    }

    private String messageFor(UsageType usageType) {
        return switch (usageType) {
            case AI_TRADE_REVIEW -> "You have reached your free plan limit for AI trade reviews.";
            case DAILY_COACH -> "You have reached your free plan limit for Daily Coach generations.";
            case WEEKLY_COACH -> "You have reached your free plan limit for Weekly Coach generations.";
            case TRADING_DNA -> "You have reached your free plan limit for Trading DNA generations.";
            case SESSION_REVIEW -> "You have reached your free plan limit for Session Review generations.";
        };
    }

    private record Period(LocalDateTime start, LocalDateTime end) {
    }
}
