package com.tradepilot.activation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.BillingUsageResponse;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.ImportBatch;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.*;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.ImportBatchRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ActivationService {
    private static final int MIN_TRADES_FOR_LEAK_DETECTION = 10;
    private final TradingAccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final ImportBatchRepository importBatchRepository;
    private final AiLeakDetectorSnapshotRepository leakSnapshotRepository;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final UsageLimitService usageLimitService;
    private final FeatureAccessService featureAccessService;
    private final FeatureUsageRepository featureUsageRepository;
    private final FeatureUsageService featureUsageService;
    private final UserFeaturePreviewRepository previewRepository;
    private final UserFeaturePreviewService previewService;
    private final ObjectMapper objectMapper;
    private final SeoAnalyticsService seoAnalyticsService;
    private final boolean activationEnabled;

    public ActivationService(
            TradingAccountRepository accountRepository,
            TradeRepository tradeRepository,
            ImportBatchRepository importBatchRepository,
            AiLeakDetectorSnapshotRepository leakSnapshotRepository,
            AiTradeReviewRepository aiTradeReviewRepository,
            UsageLimitService usageLimitService,
            FeatureAccessService featureAccessService,
            FeatureUsageRepository featureUsageRepository,
            FeatureUsageService featureUsageService,
            UserFeaturePreviewRepository previewRepository,
            UserFeaturePreviewService previewService,
            ObjectMapper objectMapper,
            SeoAnalyticsService seoAnalyticsService,
            @Value("${app.activation.first-insight-enabled:true}") boolean activationEnabled
    ) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.importBatchRepository = importBatchRepository;
        this.leakSnapshotRepository = leakSnapshotRepository;
        this.aiTradeReviewRepository = aiTradeReviewRepository;
        this.usageLimitService = usageLimitService;
        this.featureAccessService = featureAccessService;
        this.featureUsageRepository = featureUsageRepository;
        this.featureUsageService = featureUsageService;
        this.previewRepository = previewRepository;
        this.previewService = previewService;
        this.objectMapper = objectMapper;
        this.seoAnalyticsService = seoAnalyticsService;
        this.activationEnabled = activationEnabled;
    }

    @Transactional
    public ActivationStatusResponse status(User user) {
        long accountCount = accountRepository.countByUser(user);
        long tradeCount = tradeRepository.countByUser(user);
        ImportBatch firstImport = importBatchRepository.findTopByUserAndImportedRowsGreaterThanOrderByImportedAtAsc(user, 0).orElse(null);
        AccountReadiness readiness = accountReadiness(user);
        ActivationStatusResponse.FirstInsightSummary firstInsight = firstInsight(user);
        boolean insightGenerated = firstInsight != null;
        boolean imported = firstImport != null;
        boolean available = readiness.bestAccountId() != null && readiness.bestTradeCount() >= MIN_TRADES_FOR_LEAK_DETECTION;
        if (activationEnabled && insightGenerated && !featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.FIRST_INSIGHT_GENERATED)) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.FIRST_INSIGHT_GENERATED, FeatureUsageEventType.AI_GENERATION, firstInsight.accountId(), firstInsight.sourceFeature(), firstInsight.insightId().toString(), "activation", null, firstInsight.generatedAt());
            seoAnalyticsService.markFirstInsightGenerated(user, firstInsight.generatedAt());
        }
        if (activationEnabled && available && !featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.FIRST_INSIGHT_AVAILABLE)) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.FIRST_INSIGHT_AVAILABLE, FeatureUsageEventType.USER_ACTION, readiness.bestAccountId(), "TRADING_ACCOUNT", readiness.bestAccountId().toString(), "activation", metadata("tradeCount", readiness.bestTradeCount()));
        }
        UserFeaturePreview preview = previewService.ensureWeeklyCoachPreviewUnlocked(user, insightGenerated);
        if (preview == null) {
            preview = previewRepository.findByUserIdAndFeatureKey(user.getId(), UserFeaturePreviewService.WEEKLY_COACH_PREVIEW).orElse(null);
        }
        BillingUsageResponse usage = usageLimitService.getUsage(user);
        boolean allowanceLow = usage.getMonthlyAiCredits() != null
                && usage.getMonthlyAiCredits() > 0
                && usage.getMonthlyAiCreditsRemaining() != null
                && usage.getMonthlyAiCreditsRemaining() <= Math.ceil(usage.getMonthlyAiCredits() * 0.3);
        if (allowanceLow && !featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.AI_ALLOWANCE_LOW)) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.AI_ALLOWANCE_LOW, FeatureUsageEventType.USER_ACTION, null, null, null, "activation", metadata("remaining", usage.getMonthlyAiCreditsRemaining()));
        }
        boolean secondAiFeatureUsed = hasSecondAiFeature(user);
        if (activationEnabled && secondAiFeatureUsed && !featureUsageRepository.existsByUserIdAndAction(user.getId(), FeatureUsageAction.SECOND_AI_FEATURE_USED)) {
            featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.SECOND_AI_FEATURE_USED, FeatureUsageEventType.AI_GENERATION, null, null, null, "activation", null);
        }
        return new ActivationStatusResponse(
                accountCount > 0,
                tradeCount > 0,
                accountCount,
                tradeCount,
                imported,
                firstImport == null ? null : firstImport.getImportedAt(),
                available || tradeCount > 0,
                insightGenerated,
                firstInsight == null ? null : firstInsight.generatedAt(),
                imported && insightGenerated,
                readiness.bestAccountId(),
                nextAction(accountCount, imported, insightGenerated, available, preview, user, secondAiFeatureUsed),
                MIN_TRADES_FOR_LEAK_DETECTION,
                Math.max(0, MIN_TRADES_FOR_LEAK_DETECTION - readiness.bestTradeCount()),
                usage.getMonthlyAiCreditsRemaining(),
                usage.getMonthlyAiCredits(),
                usage.getMonthlyAiCreditsUsed(),
                usage.getPurchasedAiCreditsBalance(),
                usage.getTotalAiCreditsAvailable(),
                allowanceLow,
                usageLimitService.getAiFeatureCreditCosts(),
                toPreviewStatus(preview, user),
                firstInsight
        );
    }

    private String nextAction(long accountCount, boolean imported, boolean insightGenerated, boolean leakAvailable, UserFeaturePreview preview, User user, boolean secondAiFeatureUsed) {
        if (accountCount == 0) return "CREATE_TRADING_ACCOUNT";
        if (!imported) return "IMPORT_TRADES";
        if (!insightGenerated) {
            return leakAvailable && featureAccessService.hasFeature(user, Feature.AI_LEAK_DETECTOR)
                    ? "GENERATE_FIRST_LEAK"
                    : "REVIEW_FIRST_TRADE";
        }
        if (preview != null && preview.getStatus() == UserFeaturePreviewStatus.AVAILABLE) {
            return "GENERATE_PRO_PREVIEW_WEEKLY_COACH";
        }
        return secondAiFeatureUsed ? "VIEW_UPGRADE_CONTEXT" : "USE_SECOND_AI_FEATURE";
    }

    private boolean hasSecondAiFeature(User user) {
        return featureUsageRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId()).stream()
                .filter(event -> event.getEventType() == FeatureUsageEventType.AI_GENERATION)
                .map(FeatureUsageEvent::getFeature)
                .distinct()
                .limit(2)
                .count() >= 2;
    }

    private ActivationStatusResponse.ProPreviewStatus toPreviewStatus(UserFeaturePreview preview, User user) {
        boolean eligible = previewService.isEligibleFreeUser(user);
        if (preview == null) {
            return new ActivationStatusResponse.ProPreviewStatus("WEEKLY_COACH", false, false, false, eligible, null, null);
        }
        return new ActivationStatusResponse.ProPreviewStatus(
                "WEEKLY_COACH",
                preview.getStatus() == UserFeaturePreviewStatus.AVAILABLE,
                preview.getStatus() == UserFeaturePreviewStatus.CONSUMED,
                preview.getStatus() == UserFeaturePreviewStatus.RESERVED,
                eligible,
                preview.getGrantedAt(),
                preview.getConsumedAt()
        );
    }

    private AccountReadiness accountReadiness(User user) {
        return tradeRepository.countByAccountForUser(user).stream()
                .map(row -> new AccountReadiness((Long) row[0], ((Number) row[1]).longValue()))
                .findFirst()
                .orElse(new AccountReadiness(null, 0));
    }

    private ActivationStatusResponse.FirstInsightSummary firstInsight(User user) {
        ActivationStatusResponse.FirstInsightSummary leak = firstLeakInsight(user);
        ActivationStatusResponse.FirstInsightSummary review = firstTradeReviewInsight(user);
        if (leak == null) return review;
        if (review == null) return leak;
        return leak.generatedAt().isBefore(review.generatedAt()) ? leak : review;
    }

    private ActivationStatusResponse.FirstInsightSummary firstLeakInsight(User user) {
        return leakSnapshotRepository.findByUserId(user.getId()).stream()
                .filter(snapshot -> snapshot.getTotalTrades() != null && snapshot.getTotalTrades() >= MIN_TRADES_FOR_LEAK_DETECTION)
                .map(this::toLeakInsight)
                .filter(item -> item != null)
                .min(Comparator.comparing(ActivationStatusResponse.FirstInsightSummary::generatedAt))
                .orElse(null);
    }

    private ActivationStatusResponse.FirstInsightSummary toLeakInsight(AiLeakDetectorSnapshot snapshot) {
        try {
            List<LeakResultDto> leaks = objectMapper.readValue(snapshot.getResultJson(), new TypeReference<>() {});
            if (leaks.isEmpty()) return null;
            LeakResultDto top = leaks.get(0);
            return new ActivationStatusResponse.FirstInsightSummary(
                    "AI_LEAK_DETECTOR",
                    snapshot.getId(),
                    snapshot.getAccountId(),
                    top.getTitle(),
                    top.getTotalPnl(),
                    top.getTradeCount(),
                    top.getAvgPnl(),
                    top.getRecommendedFix(),
                    snapshot.getCreatedAt()
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private ActivationStatusResponse.FirstInsightSummary firstTradeReviewInsight(User user) {
        return aiTradeReviewRepository.findByUserId(user.getId()).stream()
                .filter(review -> "COMPLETED".equals(review.getStatus()))
                .filter(review -> review.getSummary() != null && !review.getSummary().isBlank())
                .min(Comparator.comparing(AiTradeReview::getCreatedAt))
                .map(review -> new ActivationStatusResponse.FirstInsightSummary(
                        "AI_REVIEW",
                        review.getId(),
                        review.getAccountId(),
                        "AI Trade Review",
                        null,
                        1,
                        null,
                        firstNonBlank(review.getSummary(), "Review one execution adjustment before your next session."),
                        LocalDateTime.ofInstant(review.getCreatedAt(), ZoneId.systemDefault())
                ))
                .orElse(null);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String metadata(String key, Object value) {
        return "{\"" + key + "\":" + value + "}";
    }

    private record AccountReadiness(Long bestAccountId, long bestTradeCount) {
    }
}
