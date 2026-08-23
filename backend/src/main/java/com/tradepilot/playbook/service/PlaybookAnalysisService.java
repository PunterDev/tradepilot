package com.tradepilot.playbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.entity.User;
import com.tradepilot.playbook.dto.*;
import com.tradepilot.playbook.entity.PlaybookAnalysisSnapshot;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.PlaybookStatusRecommendation;
import com.tradepilot.playbook.exception.PlaybookAiReviewIneligibleException;
import com.tradepilot.playbook.repository.PlaybookAnalysisSnapshotRepository;
import com.tradepilot.playbook.repository.PlaybookChecklistItemRepository;
import com.tradepilot.playbook.repository.PlaybookSetupRepository;
import com.tradepilot.playbook.repository.TradePlaybookAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PlaybookAnalysisService {
    private final FeatureAccessService featureAccessService;
    private final UsageLimitService usageLimitService;
    private final PlaybookSetupRepository setupRepository;
    private final PlaybookChecklistItemRepository checklistRepository;
    private final TradePlaybookAssignmentRepository assignmentRepository;
    private final PlaybookAnalysisSnapshotRepository snapshotRepository;
    private final PlaybookMetricsService metricsService;
    private final PlaybookHealthScoreService healthScoreService;
    private final PlaybookBehaviorComparisonService behaviorComparisonService;
    private final PlaybookEligibilityService eligibilityService;
    private final PlaybookHashService hashService;
    private final ObjectMapper objectMapper;

    public PlaybookAnalysisService(FeatureAccessService featureAccessService, UsageLimitService usageLimitService, PlaybookSetupRepository setupRepository, PlaybookChecklistItemRepository checklistRepository, TradePlaybookAssignmentRepository assignmentRepository, PlaybookAnalysisSnapshotRepository snapshotRepository, PlaybookMetricsService metricsService, PlaybookHealthScoreService healthScoreService, PlaybookBehaviorComparisonService behaviorComparisonService, PlaybookEligibilityService eligibilityService, PlaybookHashService hashService, ObjectMapper objectMapper) {
        this.featureAccessService = featureAccessService;
        this.usageLimitService = usageLimitService;
        this.setupRepository = setupRepository;
        this.checklistRepository = checklistRepository;
        this.assignmentRepository = assignmentRepository;
        this.snapshotRepository = snapshotRepository;
        this.metricsService = metricsService;
        this.healthScoreService = healthScoreService;
        this.behaviorComparisonService = behaviorComparisonService;
        this.eligibilityService = eligibilityService;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlaybookAnalysisResponse getAnalysis(User user, Long setupId) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        PlaybookSetup setup = setup(user, setupId);
        return build(user, setup, false);
    }

    @Transactional
    public PlaybookAnalysisResponse regenerate(User user, Long setupId) {
        featureAccessService.requireFeature(user, Feature.PLAYBOOK);
        PlaybookSetup setup = setup(user, setupId);
        List<PlaybookChecklistItem> items = checklistRepository.findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(setupId);
        List<TradePlaybookAssignment> assignments = assignmentRepository.findBySetup_IdOrderByTrade_TradeDateDescTrade_EntryTimeDescIdDesc(setupId);
        PlaybookEligibilityResponse eligibility = eligibilityService.evaluate(setup, items, assignments);
        if (!eligibility.aiReviewEligible()) {
            throw new PlaybookAiReviewIneligibleException(
                    "Link and review at least " + eligibility.aiReviewMinimumTrades() + " trades before generating an AI performance review.",
                    eligibility.aiReviewMinimumTrades(),
                    eligibility.reviewedTradeCount()
            );
        }
        usageLimitService.consumeCredits(user, Feature.PLAYBOOK, 3, setupId, "PLAYBOOK_AI_REVIEW");
        PlaybookAnalysisResponse deterministic = build(user, setup, false);
        PlaybookAiReviewResponse review = fallbackAiReview(setup, deterministic);
        saveSnapshot(user, setup, deterministic, review);
        return new PlaybookAnalysisResponse(setupId, deterministic.metrics(), deterministic.healthScore(), deterministic.behaviorComparisons(), review, false, false, 3, deterministic.dataHash(), LocalDateTime.now(), deterministic.eligibility());
    }

    public PlaybookAnalysisResponse build(User user, PlaybookSetup setup, boolean includeAiReview) {
        List<PlaybookChecklistItem> items = checklistRepository.findBySetup_IdOrderByChecklistTypeAscDisplayOrderAscIdAsc(setup.getId());
        List<TradePlaybookAssignment> assignments = assignmentRepository.findBySetup_IdOrderByTrade_TradeDateDescTrade_EntryTimeDescIdDesc(setup.getId());
        PlaybookMetricsResponse metrics = metricsService.calculate(assignments);
        PlaybookHealthScoreResponse health = healthScoreService.score(setup, items, assignments, metrics);
        List<PlaybookBehaviorComparisonResponse> comparisons = behaviorComparisonService.compare(items, assignments);
        PlaybookEligibilityResponse eligibility = eligibilityService.evaluate(setup, items, assignments);
        String hash = hashService.hash(setup, items, assignments);
        Optional<PlaybookAnalysisSnapshot> cached = snapshotRepository.findBySetup_IdAndUser_Id(setup.getId(), user.getId());
        PlaybookAiReviewResponse aiReview = cached.filter(s -> hash.equals(s.getDataHash())).map(this::readReview).orElse(null);
        boolean stale = cached.isPresent() && aiReview == null && cached.get().getAiReviewJson() != null;
        if (cached.isEmpty() || !hash.equals(cached.get().getDataHash())) {
            saveSnapshot(user, setup, new PlaybookAnalysisResponse(setup.getId(), metrics, health, comparisons, null, false, stale, 0, hash, LocalDateTime.now(), eligibility), null);
        }
        return new PlaybookAnalysisResponse(setup.getId(), metrics, health, comparisons, includeAiReview ? aiReview : aiReview, cached.isPresent() && !stale, stale, 0, hash, cached.map(PlaybookAnalysisSnapshot::getGeneratedAt).orElse(LocalDateTime.now()), eligibility);
    }

    private void saveSnapshot(User user, PlaybookSetup setup, PlaybookAnalysisResponse analysis, PlaybookAiReviewResponse review) {
        PlaybookAnalysisSnapshot snapshot = snapshotRepository.findBySetup_IdAndUser_Id(setup.getId(), user.getId()).orElseGet(PlaybookAnalysisSnapshot::new);
        snapshot.setUser(user);
        snapshot.setAccountId(setup.getAccount() == null ? null : setup.getAccount().getId());
        snapshot.setSetup(setup);
        snapshot.setMetricsJson(write(analysis.metrics()));
        snapshot.setBehaviorComparisonJson(write(analysis.behaviorComparisons()));
        snapshot.setHealthScore(analysis.healthScore().score());
        snapshot.setHealthScoreConfidence(com.tradepilot.playbook.enums.PlaybookConfidence.valueOf(analysis.healthScore().confidence()));
        if (review != null) snapshot.setAiReviewJson(write(review));
        snapshot.setPromptVersion(PlaybookHashService.PROMPT_VERSION);
        snapshot.setCalculationVersion(PlaybookHashService.CALCULATION_VERSION);
        snapshot.setDataHash(analysis.dataHash());
        snapshot.setGeneratedAt(LocalDateTime.now());
        snapshotRepository.save(snapshot);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize playbook analysis.", ex);
        }
    }

    private PlaybookAiReviewResponse readReview(PlaybookAnalysisSnapshot snapshot) {
        if (snapshot.getAiReviewJson() == null) return null;
        try {
            return objectMapper.readValue(snapshot.getAiReviewJson(), PlaybookAiReviewResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private PlaybookAiReviewResponse fallbackAiReview(PlaybookSetup setup, PlaybookAnalysisResponse analysis) {
        String setupName = setup.getName();
        String headline = setupName + " playbook review";
        String summary = "This review uses documented setup rules and linked trade outcomes. Metrics are calculated from your trade history before coaching is generated.";
        String mostBroken = analysis.behaviorComparisons().stream()
                .filter(c -> c.notMetTradeCount() > 0)
                .findFirst()
                .map(PlaybookBehaviorComparisonResponse::rule)
                .orElse("No reviewed required-rule break stands out yet.");
        PlaybookStatusRecommendation recommendation = analysis.metrics().totalLinkedTrades() < 10
                ? PlaybookStatusRecommendation.NEEDS_MORE_DATA
                : analysis.healthScore().score() >= 75 ? PlaybookStatusRecommendation.CONTINUE_TESTING : PlaybookStatusRecommendation.CONTINUE_TESTING;
        return new PlaybookAiReviewResponse(
                headline,
                summary,
                "Review compliant trades to identify the setup conditions worth protecting.",
                "Improve the lowest checklist-compliance area before treating this setup as validated.",
                "Compare execution grades against compliant setup trades to separate setup quality from trade management.",
                mostBroken,
                "Compliant and noncompliant trades are compared deterministically when sample size is large enough.",
                "Performance metrics are historical observations only and do not prove future profitability.",
                "Recent trend requires enough linked trades across multiple dates.",
                "Review the most frequently missed required rule and update the checklist or execution process.",
                recommendation,
                "Status changes are recommendations only and must be applied manually.",
                "Keep the setup in testing until the documented process and actual behavior align.",
                "Confidence is based on linked trade count and checklist review completeness.",
                List.of("Linked trades: " + analysis.metrics().totalLinkedTrades(), "Health score: " + analysis.healthScore().score() + "/100"),
                List.of("No chart facts are inferred unless they were captured in trade or checklist data.")
        );
    }

    private PlaybookSetup setup(User user, Long setupId) {
        return setupRepository.findByIdAndUser_Id(setupId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Playbook setup not found."));
    }
}
