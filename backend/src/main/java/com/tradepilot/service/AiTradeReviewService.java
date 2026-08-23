package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.AiCreditLimitExceededException;
import com.tradepilot.billing.FeatureAccessException;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.ai.memory.service.TradeMemoryIndexPublisher;
import com.tradepilot.dto.ai.AiTradeReviewJobResponse;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.AiTradeReviewResponse;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.AiTradeReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTradeReviewService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeReviewService.class);
    private static final String REVIEW_VERSION = "trade-review-v1";
    private final ObjectMapper objectMapper;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final AiTradeReviewGeneratorService generatorService;
    private final AiTradeReviewAsyncWorker asyncWorker;
    private final TradeService tradeService;
    private final UsageLimitService usageLimitService;
    private final ObservabilityService observabilityService;
    private final TradeMemoryIndexPublisher tradeMemoryIndexPublisher;

    public AiTradeReviewResponse reviewTrade(User user, AiTradeReviewRequest request) {
        Long userId = requireUserId(user);
        Trade ownedTrade = resolveOwnedTrade(user, request.getTradeId());
        Instant start = Instant.now();
        Long tradeId = request.getTradeId();
        String reviewHash = buildReviewHash(request);
        log.info("AI trade review requested. tradeId={}, symbol={}", tradeId, request.getTrade() != null ? request.getTrade().getSymbol() : null);
        if (tradeId != null && !Boolean.TRUE.equals(request.getForceRefresh())) {
            var existingReview = aiTradeReviewRepository.findByUserIdAndTradeIdAndReviewVersionAndReviewHash(userId, tradeId, REVIEW_VERSION, reviewHash);
            if (existingReview.isPresent()) {
                observabilityService.recordCacheEvent("AI Reviews", true);
                long elapsedMs = Duration.between(start, Instant.now()).toMillis();
                log.info("AI trade review cache hit. tradeId={}, reviewVersion={}, reviewHash={}, elapsedMs={}", tradeId, REVIEW_VERSION, reviewHash, elapsedMs);
                return toResponse(existingReview.get());
            }
            log.info("AI trade review cache miss. tradeId={}, reviewVersion={}, reviewHash={}", tradeId, REVIEW_VERSION, reviewHash);
            observabilityService.recordCacheEvent("AI Reviews", false);
        }
        try {
            usageLimitService.enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
            AiTradeReviewResponse aiResponse = generatorService.generateFreshReview(request, userId);
            AiTradeReview savedReview = saveReview(userId, ownedTrade, request, aiResponse, reviewHash);
            if (savedReview != null) {
                tradeMemoryIndexPublisher.requestIndexAfterCommit(userId, savedReview.getTradeId());
            }
            usageLimitService.recordAiUsage(user, UsageType.AI_TRADE_REVIEW, tradeId);
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            log.info("AI trade review completed. tradeId={}, grade={}, elapsedMs={}", tradeId, aiResponse.getGrade(), elapsedMs);
            return aiResponse;
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException && (runtimeException instanceof FeatureAccessException || runtimeException instanceof AiCreditLimitExceededException)) {
                throw runtimeException;
            }
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            log.error("AI trade review failed. tradeId={}, elapsedMs={}", tradeId, elapsedMs, e);
            return fallbackReview("AI review failed. Using fallback review.");
        }
    }

    public AiTradeReviewJobResponse startAsyncReview(User user, AiTradeReviewRequest request) {
        Long userId = requireUserId(user);
        Trade ownedTrade = resolveOwnedTrade(user, request.getTradeId());
        Long tradeId = request.getTradeId();
        String reviewHash = buildReviewHash(request);
        var existingReview = Boolean.TRUE.equals(request.getForceRefresh()) ? java.util.Optional.<AiTradeReview>empty() : aiTradeReviewRepository.findByUserIdAndTradeIdAndReviewVersionAndReviewHash(userId, tradeId, REVIEW_VERSION, reviewHash);
        if (existingReview.isPresent()) {
            return toJobResponse(existingReview.get(), "Existing AI review found.");
        }
        usageLimitService.enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
        var trade = request.getTrade();
        AiTradeReview pendingReview = AiTradeReview.builder().userId(userId).tradeId(tradeId).accountId(ownedTrade == null || ownedTrade.getAccount() == null ? null : ownedTrade.getAccount().getId()).reviewVersion(REVIEW_VERSION).reviewHash(reviewHash).symbol(trade != null ? trade.getSymbol() : null).pnl(trade != null ? trade.getPnl() : null).status("PENDING").createdAt(Instant.now()).build();
        AiTradeReview saved;
        try {
            saved = aiTradeReviewRepository.saveAndFlush(pendingReview);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent AI trade review cache insert detected. userId={}, tradeId={}, reviewVersion={}, reviewHash={}", userId, tradeId, REVIEW_VERSION, reviewHash);
            return aiTradeReviewRepository.findByUserIdAndTradeIdAndReviewVersionAndReviewHash(userId, tradeId, REVIEW_VERSION, reviewHash).map(review -> toJobResponse(review, "Existing AI review found.")).orElseThrow(() -> ex);
        }
        usageLimitService.recordAiUsage(user, UsageType.AI_TRADE_REVIEW, saved.getId());
        asyncWorker.generateAsync(saved.getId(), request, reviewHash);
        return AiTradeReviewJobResponse.builder().reviewId(saved.getId()).tradeId(tradeId).status("PENDING").message("AI review generation started.").build();
    }

    private AiTradeReviewJobResponse toJobResponse(AiTradeReview review, String message) {
        return AiTradeReviewJobResponse.builder().reviewId(review.getId()).tradeId(review.getTradeId()).status(review.getStatus()).review("COMPLETED".equals(review.getStatus()) ? toResponse(review) : null).message(message).build();
    }

    public AiTradeReviewJobResponse getReviewStatus(User user, Long reviewId) {
        Long userId = requireUserId(user);
        AiTradeReview review = aiTradeReviewRepository.findByIdAndUserId(reviewId, userId).orElseThrow(() -> new ResourceNotFoundException("AI review not found"));
        return AiTradeReviewJobResponse.builder().reviewId(review.getId()).tradeId(review.getTradeId()).status(review.getStatus()).review("COMPLETED".equals(review.getStatus()) ? toResponse(review) : null).message(review.getErrorMessage()).build();
    }

    public AiTradeReviewResponse getLatestCompletedReviewForTrade(User user, Long tradeId) {
        Long userId = requireUserId(user);
        resolveOwnedTrade(user, tradeId);
        return aiTradeReviewRepository.findTopByUserIdAndTradeIdAndStatusOrderByCreatedAtDesc(userId, tradeId, "COMPLETED").map(this::toResponse).orElse(null);
    }

    public void fillReviewEntity(AiTradeReview review, AiTradeReviewRequest request, AiTradeReviewResponse response, String reviewHash) throws Exception {
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

    private AiTradeReview saveReview(Long userId, Trade ownedTrade, AiTradeReviewRequest request, AiTradeReviewResponse response, String reviewHash) {
        try {
            AiTradeReview review = AiTradeReview.builder().userId(userId).accountId(ownedTrade == null || ownedTrade.getAccount() == null ? null : ownedTrade.getAccount().getId()).status("COMPLETED").createdAt(Instant.now()).build();
            fillReviewEntity(review, request, response, reviewHash);
            AiTradeReview saved = aiTradeReviewRepository.save(review);
            log.info("AI trade review saved. tradeId={}, grade={}", request.getTradeId(), response.getGrade());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save AI trade review. tradeId={}", request.getTradeId(), e);
            return null;
        }
    }

    private AiTradeReviewResponse toResponse(AiTradeReview review) {
        try {
            return AiTradeReviewResponse.builder().grade(review.getGrade()).summary(review.getSummary()).mistakes(objectMapper.readValue(safeJsonArray(review.getMistakesJson()), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))).strengths(objectMapper.readValue(safeJsonArray(review.getStrengthsJson()), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))).improvementPlan(objectMapper.readValue(safeJsonArray(review.getImprovementPlanJson()), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))).tags(objectMapper.readValue(safeJsonArray(review.getTagsJson()), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))).build();
        } catch (Exception e) {
            log.error("Failed to parse saved AI trade review. reviewId={}, tradeId={}", review.getId(), review.getTradeId(), e);
            return fallbackReview("Saved AI review could not be parsed.");
        }
    }

    private String safeJsonArray(String json) {
        return json == null || json.isBlank() ? "[]" : json;
    }

    private AiTradeReviewResponse fallbackReview(String reason) {
        return AiTradeReviewResponse.builder().grade("B-").summary(reason).mistakes(List.of("Possible impatience or FOMO based on journal notes.", "Setup quality may not have been strong enough.")).strengths(List.of("You documented the trade clearly.", "You identified the emotional issue after the trade.")).improvementPlan(List.of("Wait for clear confirmation before entering.", "Avoid trading when mood is anxious.", "Only take A or A+ setups during discipline reset.")).tags(List.of("Poor Patience", "FOMO")).build();
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user.getId();
    }

    private Trade resolveOwnedTrade(User user, Long tradeId) {
        if (tradeId == null) {
            throw new IllegalArgumentException("tradeId is required for AI Trade Review.");
        }
        Trade trade = tradeService.getOwnedTrade(user, tradeId);
        validateReviewableTrade(trade);
        return trade;
    }

    private void validateReviewableTrade(Trade trade) {
        if (trade == null) {
            throw new ResourceNotFoundException("Trade not found");
        }
        if (trade.getSymbol() == null || trade.getSymbol().isBlank() || trade.getTradeDate() == null || trade.getPnlAmount() == null) {
            log.info("AI skipped for Trade Review: insufficient trade data. tradeId={}, symbol={}, tradeDate={}, pnlPresent={}", trade.getId(), trade.getSymbol(), trade.getTradeDate(), trade.getPnlAmount() != null);
            throw new IllegalArgumentException("Trade has insufficient data for AI review.");
        }
    }

    private String buildReviewHash(AiTradeReviewRequest request) {
        try {
            Map<String, Object> hashInput = new HashMap<>();
            hashInput.put("trade", request.getTrade());
            hashInput.put("journalText", request.getJournalText() == null ? "" : request.getJournalText());
            hashInput.put("reviewVersion", REVIEW_VERSION);
            String raw = objectMapper.writeValueAsString(hashInput);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Failed to build AI review hash. tradeId={}", request.getTradeId(), e);
            return "hash-error";
        }
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewService(final ObjectMapper objectMapper, final AiTradeReviewRepository aiTradeReviewRepository, final AiTradeReviewGeneratorService generatorService, final AiTradeReviewAsyncWorker asyncWorker, final TradeService tradeService, final UsageLimitService usageLimitService, final ObservabilityService observabilityService, final TradeMemoryIndexPublisher tradeMemoryIndexPublisher) {
        this.objectMapper = objectMapper;
        this.aiTradeReviewRepository = aiTradeReviewRepository;
        this.generatorService = generatorService;
        this.asyncWorker = asyncWorker;
        this.tradeService = tradeService;
        this.usageLimitService = usageLimitService;
        this.observabilityService = observabilityService;
        this.tradeMemoryIndexPublisher = tradeMemoryIndexPublisher;
    }
}
