package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.dto.TradingMemoryBackfillRequest;
import com.tradepilot.ai.memory.dto.TradingMemoryBackfillResponse;
import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.repository.AiTradeReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TradingMemoryBackfillService {
    private static final int MAX_BATCH = 250;
    private final AiTradeReviewRepository reviewRepository;
    private final TradeMemoryRepository memoryRepository;
    private final TradeMemoryIndexService indexService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TradingMemoryBackfillService(AiTradeReviewRepository reviewRepository, TradeMemoryRepository memoryRepository, TradeMemoryIndexService indexService) {
        this.reviewRepository = reviewRepository;
        this.memoryRepository = memoryRepository;
        this.indexService = indexService;
    }

    public TradingMemoryBackfillResponse backfill(TradingMemoryBackfillRequest request) {
        boolean dryRun = request == null || !Boolean.FALSE.equals(request.dryRun());
        if (!dryRun && !running.compareAndSet(false, true)) {
            return new TradingMemoryBackfillResponse(0, 0, 0, 0, 1, 1, null, false);
        }
        try {
            return doBackfill(request, dryRun);
        } finally {
            if (!dryRun) running.set(false);
        }
    }

    private TradingMemoryBackfillResponse doBackfill(TradingMemoryBackfillRequest request, boolean dryRun) {
        int limit = request == null || request.limit() == null ? 100 : Math.min(Math.max(request.limit(), 1), MAX_BATCH);
        List<AiTradeReview> reviews = reviewRepository.findCompletedReviewedTrades(PageRequest.of(0, limit));
        long alreadyIndexed = 0;
        long stale = 0;
        long queued = 0;
        long failed = 0;
        long failedToQueue = 0;
        for (AiTradeReview review : reviews) {
            var memory = memoryRepository.findByTradeIdAndUserId(review.getTradeId(), review.getUserId());
            if (memory.isPresent() && memory.get().getEmbeddingStatus() == EmbeddingStatus.INDEXED) {
                alreadyIndexed++;
                continue;
            }
            if (memory.isPresent() && memory.get().getEmbeddingStatus() == EmbeddingStatus.STALE) {
                stale++;
            }
            if (!dryRun) {
                try {
                    var indexed = indexService.indexTradeMemory(review.getUserId(), review.getTradeId(), false);
                    if (indexed != null && indexed.getEmbeddingStatus() == EmbeddingStatus.FAILED) failed++;
                } catch (RuntimeException ex) {
                    failedToQueue++;
                }
            }
            queued++;
        }
        String nextCursor = reviews.size() == limit ? String.valueOf(limit) : null;
        return new TradingMemoryBackfillResponse(reviews.size(), alreadyIndexed, stale, queued, failed, failedToQueue, nextCursor, dryRun);
    }
}
