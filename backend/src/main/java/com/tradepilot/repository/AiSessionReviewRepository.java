package com.tradepilot.repository;

import com.tradepilot.entity.AiSessionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiSessionReviewRepository extends JpaRepository<AiSessionReview, Long> {
    Optional<AiSessionReview> findByUserIdAndAccountIdAndSessionDate(
            Long userId,
            Long accountId,
            LocalDate sessionDate
    );

    Optional<AiSessionReview> findTopByUserIdAndAccountIdAndSessionDateOrderByCreatedAtDesc(
            Long userId,
            Long accountId,
            LocalDate sessionDate
    );

    Optional<AiSessionReview> findTopByUserIdAndAccountIdAndSessionDateAndReviewHashOrderByCreatedAtDesc(
            Long userId,
            Long accountId,
            LocalDate sessionDate,
            String reviewHash
    );

    List<AiSessionReview> findByUserId(Long userId);

    long countByUserIdAndAccountIdAndSessionDateBetween(Long userId, Long accountId, LocalDate startDate, LocalDate endDate);

    long deleteByUserIdAndAccountIdAndSessionDateBetween(Long userId, Long accountId, LocalDate startDate, LocalDate endDate);

    void deleteByUserId(Long userId);
}
