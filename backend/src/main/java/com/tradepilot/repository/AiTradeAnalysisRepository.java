package com.tradepilot.repository;

import com.tradepilot.entity.AiTradeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiTradeAnalysisRepository extends JpaRepository<AiTradeAnalysis, Long> {
    Optional<AiTradeAnalysis> findFirstByUserIdAndTradeIdAndReviewVersionAndReviewHashAndStatus(
            Long userId,
            Long tradeId,
            String reviewVersion,
            String reviewHash,
            String status
    );

    Optional<AiTradeAnalysis> findByIdAndUserId(Long id, Long userId);

    List<AiTradeAnalysis> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            Instant start,
            Instant end
    );

    List<AiTradeAnalysis> findByUserIdAndTradeIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            List<Long> tradeIds,
            Instant start,
            Instant end
    );

    void deleteByUserId(Long userId);
}
