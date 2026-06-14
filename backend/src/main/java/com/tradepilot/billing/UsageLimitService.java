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
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageLimitService {
    private static final int UNLIMITED = -1;
    private static final Duration BILLING_USAGE_TTL = Duration.ofSeconds(45);
    private static final int FREE_MONTHLY_AI_CREDITS = 10;
    private static final int PRO_MONTHLY_AI_CREDITS = 100;

    private final AiUsageEventRepository usageEventRepository;
    private final AiCreditUsageRepository creditUsageRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final AggregateCacheService aggregateCacheService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;
    private final FeatureAccessService featureAccessService;

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
        Feature feature = featureFor(usageType);
        featureAccessService.requireFeature(user, feature);
        requireCredits(user, creditsFor(feature));
    }

    public int getMonthlyCreditLimit(User user) {
        return plan(user) == PlanType.PRO ? PRO_MONTHLY_AI_CREDITS : FREE_MONTHLY_AI_CREDITS;
    }

    public int getCreditsUsedThisMonth(User user) {
        Period period = period(user);
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                creditUsageRepository.sumCreditsByUserBetween(
                user.getId(),
                period.start(),
                period.end()
                )
        ));
    }

    public int getCreditsRemaining(User user) {
        return Math.max(0, getMonthlyCreditLimit(user) - getCreditsUsedThisMonth(user));
    }

    public void requireCredits(User user, int requiredCredits) {
        int limit = getMonthlyCreditLimit(user);
        int used = getCreditsUsedThisMonth(user);
        int remaining = Math.max(0, limit - used);
        if (remaining < requiredCredits) {
            log.warn("AI credit limit exceeded. userId={}, plan={}, used={}, limit={}, remaining={}, requested={}",
                    user.getId(), plan(user), used, limit, remaining, requiredCredits);
            throw new AiCreditLimitExceededException(used, limit, remaining);
        }
    }

    @Transactional
    public void recordAiUsage(User user, UsageType usageType, Long relatedResourceId) {
        usageEventRepository.save(AiUsageEvent.builder()
                .userId(user.getId())
                .usageType(usageType)
                .relatedResourceId(relatedResourceId)
                .build());
        consumeCredits(user, featureFor(usageType), creditsFor(featureFor(usageType)), relatedResourceId, null);
        log.info("AI usage recorded. userId={}, usageType={}, relatedResourceId={}",
                user.getId(), usageType, relatedResourceId);
    }

    @Transactional
    public void consumeCredits(User user, Feature feature, int credits) {
        consumeCredits(user, feature, credits, null, null);
    }

    @Transactional
    public void consumeCredits(User user, Feature feature, int credits, Long requestId, String metadata) {
        if (credits <= 0) {
            return;
        }
        featureAccessService.requireFeature(user, feature);
        requireCredits(user, credits);
        creditUsageRepository.save(AiCreditUsage.builder()
                .userId(user.getId())
                .feature(feature)
                .credits(credits)
                .requestId(requestId)
                .metadata(metadata)
                .build());
        log.info("AI credits consumed. userId={}, feature={}, credits={}, requestId={}",
                user.getId(), feature, credits, requestId);
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
        Period period = period(user);

        Map<String, Integer> limits = new LinkedHashMap<>();
        Map<String, Integer> used = new LinkedHashMap<>();
        Map<String, Integer> remaining = new LinkedHashMap<>();

        limits.put("TRADING_ACCOUNTS", plan == PlanType.PRO ? UNLIMITED : 1);
        limits.put("IMPORTED_TRADES", plan == PlanType.PRO ? UNLIMITED : 100);
        used.put("TRADING_ACCOUNTS", Math.toIntExact(Math.min(Integer.MAX_VALUE, tradingAccountRepository.countByUser(user))));
        used.put("IMPORTED_TRADES", Math.toIntExact(Math.min(Integer.MAX_VALUE, tradeRepository.countByUser(user))));
        limits.put("AI_CREDITS", getMonthlyCreditLimit(user));
        used.put("AI_CREDITS", getCreditsUsedThisMonth(user));

        for (UsageType type : UsageType.values()) {
            Feature feature = featureFor(type);
            int featureCredits = Math.toIntExact(Math.min(
                    Integer.MAX_VALUE,
                    creditUsageRepository.sumCreditsByUserAndFeatureBetween(
                            user.getId(),
                            feature,
                            period.start(),
                            period.end()
                    )
            ));
            limits.put(type.name(), getMonthlyCreditLimit(user));
            used.put(type.name(), featureCredits);
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

    private Feature featureFor(UsageType usageType) {
        return switch (usageType) {
            case AI_TRADE_REVIEW -> Feature.AI_TRADE_REVIEW;
            case DAILY_COACH -> Feature.DAILY_COACH;
            case WEEKLY_COACH -> Feature.WEEKLY_COACH;
            case TRADING_DNA -> Feature.TRADING_DNA;
            case AI_LEAK_DETECTOR -> Feature.AI_LEAK_DETECTOR;
            case SESSION_REVIEW -> Feature.SESSION_REVIEW;
        };
    }

    private int creditsFor(Feature feature) {
        return switch (feature) {
            case AI_TRADE_REVIEW -> 1;
            case SESSION_REVIEW, DAILY_COACH -> 2;
            case TRADING_DNA, AI_LEAK_DETECTOR -> 3;
            case WEEKLY_COACH -> 4;
            default -> 0;
        };
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
            case AI_LEAK_DETECTOR -> "You have reached your free plan limit for AI Leak Detector generations.";
            case SESSION_REVIEW -> "You have reached your free plan limit for Session Review generations.";
        };
    }

    private record Period(LocalDateTime start, LocalDateTime end) {
    }
}
