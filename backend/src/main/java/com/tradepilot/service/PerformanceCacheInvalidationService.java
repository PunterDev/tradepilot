package com.tradepilot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceCacheInvalidationService {
    private final AggregateCacheService aggregateCacheService;

    public void invalidateUserTradeDerivedCaches(Long userId) {
        invalidateSessionReview(userId);
        invalidateTradingDna(userId);
        invalidateAnalytics(userId);
        invalidateBillingUsage(userId);
    }

    public void invalidateSessionReview(Long userId) {
        aggregateCacheService.evictByPrefix("session-review:" + userId + ":");
    }

    public void invalidateTradingDna(Long userId) {
        aggregateCacheService.evictByPrefix("trading-dna:" + userId + ":");
    }

    public void invalidateAnalytics(Long userId) {
        aggregateCacheService.evictByPrefix("analytics:" + userId + ":");
    }

    public void invalidateBillingUsage(Long userId) {
        aggregateCacheService.evictByPrefix("billing-usage:" + userId + ":");
    }
}
