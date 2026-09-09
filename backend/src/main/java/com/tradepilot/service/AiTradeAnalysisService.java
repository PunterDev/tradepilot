package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.strategy.StrategyContext;
import com.tradepilot.ai.strategy.StrategyContextService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.dto.ai.AiTradeAnalysisJobResponse;
import com.tradepilot.dto.ai.AiTradeAnalysisRequest;
import com.tradepilot.entity.AiTradeAnalysis;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.AiTradeAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

@Service
public class AiTradeAnalysisService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiTradeAnalysisService.class);
    private final ObjectMapper objectMapper;
    private final AiTradeAnalysisRepository repository;
    private final AiTradeAnalysisMapper mapper;
    private final AiTradeAnalysisHashService hashService;
    private final AiTradeAnalysisAsyncWorker asyncWorker;
    private final TradeService tradeService;
    private final UsageLimitService usageLimitService;
    private final StrategyContextService strategyContextService;

    public AiTradeAnalysisJobResponse startAsync(User user, String requestJson, MultipartFile chartImage) {
        try {
            Long userId = requireUserId(user);
            AiTradeAnalysisRequest request = objectMapper.readValue(requestJson, AiTradeAnalysisRequest.class);
            Trade trade = validateOwnedTrade(user, request.getTradeId());
            applyStrategyContext(userId, request, trade);
            byte[] imageBytes = chartImage == null || chartImage.isEmpty() ? null : chartImage.getBytes();
            String mimeType = chartImage == null ? null : chartImage.getContentType();
            String reviewVersion = request.getReviewVersion() == null || request.getReviewVersion().isBlank() ? "v1" : request.getReviewVersion();
            request.setReviewVersion(reviewVersion);
            String reviewHash = hashService.hash(request, imageBytes);
            return repository.findFirstByUserIdAndTradeIdAndReviewVersionAndReviewHashAndStatus(userId, request.getTradeId(), reviewVersion, reviewHash, "COMPLETED").map(cached -> {
                log.info("AI trade analysis cache hit. tradeId={}, analysisId={}", request.getTradeId(), cached.getId());
                return AiTradeAnalysisJobResponse.builder().analysisId(cached.getId()).tradeId(request.getTradeId()).status("COMPLETED").analysis(mapper.toResponse(cached)).message("Cached AI trade analysis returned.").build();
            }).orElseGet(() -> {
                usageLimitService.enforceAiUsage(user, UsageType.AI_TRADE_REVIEW);
                AiTradeAnalysisJobResponse job = createPendingJob(userId, request, reviewHash, imageBytes, mimeType);
                usageLimitService.recordAiUsage(user, UsageType.AI_TRADE_REVIEW, job.getAnalysisId());
                return job;
            });
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Failed to start AI trade analysis: " + e.getMessage(), e);
        }
    }

    public AiTradeAnalysisJobResponse getStatus(User user, Long analysisId) {
        Long userId = requireUserId(user);
        AiTradeAnalysis entity = repository.findByIdAndUserId(analysisId, userId).orElseThrow(() -> new ResourceNotFoundException("AI trade analysis not found"));
        return AiTradeAnalysisJobResponse.builder().analysisId(entity.getId()).tradeId(entity.getTradeId()).status(entity.getStatus()).analysis("COMPLETED".equals(entity.getStatus()) ? mapper.toResponse(entity) : null).message(entity.getErrorMessage()).build();
    }

    private AiTradeAnalysisJobResponse createPendingJob(Long userId, AiTradeAnalysisRequest request, String reviewHash, byte[] imageBytes, String mimeType) {
        log.info("AI trade analysis cache miss. tradeId={}, hasChartImage={}", request.getTradeId(), imageBytes != null);
        AiTradeAnalysis entity = new AiTradeAnalysis();
        entity.setUserId(userId);
        entity.setTradeId(request.getTradeId());
        entity.setReviewVersion(request.getReviewVersion());
        entity.setReviewHash(reviewHash);
        entity.setStatus("PENDING");
        entity.setHasChartImage(imageBytes != null && imageBytes.length > 0);
        if (request.getTrade() != null) {
            entity.setSymbol(request.getTrade().getSymbol());
            if (request.getTrade().getPnl() != null) {
                entity.setPnl(BigDecimal.valueOf(request.getTrade().getPnl()));
            }
        }
        AiTradeAnalysis saved = repository.save(entity);
        asyncWorker.generateAsync(saved.getId(), request, imageBytes, mimeType);
        return AiTradeAnalysisJobResponse.builder().analysisId(saved.getId()).tradeId(request.getTradeId()).status(saved.getStatus()).message("AI trade analysis started.").build();
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user.getId();
    }

    private Trade validateOwnedTrade(User user, Long tradeId) {
        if (tradeId == null) {
            throw new IllegalArgumentException("tradeId is required");
        }
        return tradeService.getOwnedTrade(user, tradeId);
    }

    private void applyStrategyContext(Long userId, AiTradeAnalysisRequest request, Trade trade) {
        StrategyContext context = strategyContextService.resolve(userId, trade, request.getJournalText());
        request.setStrategyContext(context);
        if (request.getTrade() != null && request.getTrade().getSetup() == null && trade.getSetup() != null) {
            request.getTrade().setSetup(trade.getSetup().name());
        }
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisService(final ObjectMapper objectMapper, final AiTradeAnalysisRepository repository, final AiTradeAnalysisMapper mapper, final AiTradeAnalysisHashService hashService, final AiTradeAnalysisAsyncWorker asyncWorker, final TradeService tradeService, final UsageLimitService usageLimitService, final StrategyContextService strategyContextService) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.mapper = mapper;
        this.hashService = hashService;
        this.asyncWorker = asyncWorker;
        this.tradeService = tradeService;
        this.usageLimitService = usageLimitService;
        this.strategyContextService = strategyContextService;
    }
}
