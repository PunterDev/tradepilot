package com.tradepilot.ai.memory.service;

import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TradeMemoryIndexPublisher {
    private static final Logger log = LoggerFactory.getLogger(TradeMemoryIndexPublisher.class);
    private final TradeMemoryIndexService indexService;
    private final UserRepository userRepository;
    private final FeatureUsageService featureUsageService;

    public TradeMemoryIndexPublisher(TradeMemoryIndexService indexService, UserRepository userRepository, FeatureUsageService featureUsageService) {
        this.indexService = indexService;
        this.userRepository = userRepository;
        this.featureUsageService = featureUsageService;
    }

    public void requestIndexAfterCommit(Long userId, Long tradeId) {
        if (userId == null || tradeId == null) return;
        userRepository.findById(userId).ifPresent(user -> featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_INDEX_REQUESTED, FeatureUsageEventType.USER_ACTION, null, null));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    indexService.indexAfterReviewAsync(userId, tradeId);
                }
            });
            log.info("Trading Memory index request registered after commit. userId={}, tradeId={}", userId, tradeId);
            return;
        }
        indexService.indexAfterReviewAsync(userId, tradeId);
    }
}
