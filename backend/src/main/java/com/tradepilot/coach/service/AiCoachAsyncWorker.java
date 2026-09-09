package com.tradepilot.coach.service;

import com.tradepilot.activation.UserFeaturePreviewService;
import com.tradepilot.coach.dto.CoachAiResponse;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiCoachAsyncWorker {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiCoachAsyncWorker.class);
    private final AiCoachSnapshotRepository repository;
    private final CoachAnalyticsAggregator analyticsAggregator;
    private final CoachAiGeneratorService aiGeneratorService;
    private final CoachHashService hashService;
    private final CoachOutputConsistencyService consistencyService;
    private final UserFeaturePreviewService previewService;

    @Async("aiTaskExecutor")
    public void generateAsync(Long snapshotId) {
        AiCoachSnapshot snapshot = repository.findById(snapshotId).orElseThrow(() -> new IllegalArgumentException("Coach snapshot not found: " + snapshotId));
        try {
            log.info("AI Coach generation started. snapshotId={}, type={}, accountId={}", snapshot.getId(), snapshot.getSnapshotType(), snapshot.getAccountId());
            snapshot.setStatus(CoachSnapshotStatus.PROCESSING);
            repository.save(snapshot);
            CoachDeterministicSummary summary = analyticsAggregator.aggregate(snapshot.getUserId(), snapshot.getAccountId(), snapshot.getSnapshotType(), snapshot.getPeriodStart(), snapshot.getPeriodEnd());
            String reviewHash = hashService.hash(summary);
            CoachAiResponse aiResponse = aiGeneratorService.generate(snapshot.getSnapshotType(), summary, snapshot.getUserId());
            aiResponse = consistencyService.enforce(snapshot.getSnapshotType(), summary, aiResponse, snapshot.getUserId(), snapshot.getAccountId(), snapshot.getPeriodStart());
            snapshot.setTotalTrades(summary.getTotalTrades());
            snapshot.setNetPnl(summary.getNetPnl());
            snapshot.setWinRate(summary.getWinRate());
            snapshot.setProfitFactor(summary.getProfitFactor());
            snapshot.setBestSetup(summary.getBestSetup());
            snapshot.setWorstSetup(summary.getWorstSetup());
            snapshot.setBestSession(summary.getBestSession());
            snapshot.setWorstSession(summary.getWorstSession());
            snapshot.setBiggestMistake(summary.getBiggestMistake());
            snapshot.setDeterministicSummaryJson(hashService.toJson(summary));
            snapshot.setAiResponseJson(hashService.toJson(aiResponse));
            snapshot.setReviewHash(reviewHash);
            snapshot.setStatus(CoachSnapshotStatus.COMPLETED);
            snapshot.setErrorMessage(null);
            repository.save(snapshot);
            previewService.consumeReservedWeeklyCoachPreview(snapshot.getUserId(), snapshot.getId());
            log.info("AI Coach generation completed. snapshotId={}", snapshot.getId());
        } catch (Exception e) {
            log.error("AI Coach generation failed. snapshotId={}", snapshotId, e);
            snapshot.setStatus(CoachSnapshotStatus.FAILED);
            snapshot.setErrorMessage(e.getMessage());
            repository.save(snapshot);
            previewService.releaseReservedWeeklyCoachPreview(snapshot.getUserId(), snapshot.getId());
        }
    }

    @java.lang.SuppressWarnings("all")
    public AiCoachAsyncWorker(final AiCoachSnapshotRepository repository, final CoachAnalyticsAggregator analyticsAggregator, final CoachAiGeneratorService aiGeneratorService, final CoachHashService hashService, final CoachOutputConsistencyService consistencyService, final UserFeaturePreviewService previewService) {
        this.repository = repository;
        this.analyticsAggregator = analyticsAggregator;
        this.aiGeneratorService = aiGeneratorService;
        this.hashService = hashService;
        this.consistencyService = consistencyService;
        this.previewService = previewService;
    }
}
