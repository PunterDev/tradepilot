package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.AiTradeReviewResponse;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.repository.AiTradeReviewRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiTradeReviewAsyncWorker {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeReviewAsyncWorker.class);
    private static final String REVIEW_VERSION = "trade-review-v1";
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final AiTradeReviewGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    @Async("aiTaskExecutor")
    public void generateAsync(Long reviewId, AiTradeReviewRequest request, String reviewHash) {
        try {
            log.info("Async AI trade review started. reviewId={}, tradeId={}", reviewId, request.getTradeId());
            AiTradeReview review = aiTradeReviewRepository.findById(reviewId).orElseThrow(() -> new IllegalStateException("AI review job not found: " + reviewId));
            AiTradeReviewResponse response = generatorService.generateFreshReview(request, review.getUserId());
            fillReviewEntity(review, request, response, reviewHash);
            review.setStatus("COMPLETED");
            review.setErrorMessage(null);
            aiTradeReviewRepository.save(review);
            log.info("Async AI trade review completed. reviewId={}, tradeId={}, grade={}", reviewId, request.getTradeId(), response.getGrade());
        } catch (Exception e) {
            log.error("Async AI trade review failed. reviewId={}, tradeId={}", reviewId, request.getTradeId(), e);
            aiTradeReviewRepository.findById(reviewId).ifPresent(review -> {
                review.setStatus("FAILED");
                review.setErrorMessage(e.getMessage());
                aiTradeReviewRepository.save(review);
            });
        }
    }

    private void fillReviewEntity(AiTradeReview review, AiTradeReviewRequest request, AiTradeReviewResponse response, String reviewHash) throws Exception {
        var trade = request.getTrade();
        review.setTradeId(request.getTradeId());
        review.setReviewVersion(REVIEW_VERSION);
        review.setReviewHash(reviewHash);
        review.setSymbol(trade != null ? trade.getSymbol() : null);
        review.setPnl(trade != null ? trade.getPnl() : null);
        review.setGrade(response.getGrade());
        review.setSummary(response.getSummary());
        review.setMistakesJson(objectMapper.writeValueAsString(response.getMistakes() == null ? List.of() : response.getMistakes()));
        review.setStrengthsJson(objectMapper.writeValueAsString(response.getStrengths() == null ? List.of() : response.getStrengths()));
        review.setImprovementPlanJson(objectMapper.writeValueAsString(response.getImprovementPlan() == null ? List.of() : response.getImprovementPlan()));
        review.setTagsJson(objectMapper.writeValueAsString(response.getTags() == null ? List.of() : response.getTags()));
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewAsyncWorker(final AiTradeReviewRepository aiTradeReviewRepository, final AiTradeReviewGeneratorService generatorService, final ObjectMapper objectMapper) {
        this.aiTradeReviewRepository = aiTradeReviewRepository;
        this.generatorService = generatorService;
        this.objectMapper = objectMapper;
    }
}
