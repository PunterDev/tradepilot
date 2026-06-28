package com.tradepilot.repository;

import com.tradepilot.entity.AiTradeReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AiTradeReviewRepository extends JpaRepository<AiTradeReview, Long> {

    Optional<AiTradeReview> findByUserIdAndTradeIdAndReviewVersionAndReviewHash(
            Long userId,
            Long tradeId,
            String reviewVersion,
            String reviewHash
    );

    Optional<AiTradeReview> findByIdAndUserId(Long id, Long userId);

    Optional<AiTradeReview> findTopByUserIdAndTradeIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            Long tradeId,
            String status
    );

    List<AiTradeReview> findByUserIdAndAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            Long accountId,
            Instant start,
            Instant end
    );

    List<AiTradeReview> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId,
            Instant start,
            Instant end
    );

    long countByStatus(String status);

    long countByUserId(Long userId);

    @Query("""
            select r.userId, count(r)
            from AiTradeReview r
            where r.userId in :userIds
            group by r.userId
            """)
    List<Object[]> countByUserIds(@Param("userIds") List<Long> userIds);

    List<AiTradeReview> findByUserId(Long userId);

    long countByUserIdAndTradeIdIn(Long userId, List<Long> tradeIds);

    long deleteByUserIdAndTradeIdIn(Long userId, List<Long> tradeIds);

    void deleteByUserId(Long userId);
}
