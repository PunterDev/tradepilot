package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AiCreditUsageRepository extends JpaRepository<AiCreditUsage, Long> {
    @Query("""
            select coalesce(sum(u.credits), 0)
            from AiCreditUsage u
            where u.userId = :userId and u.createdAt >= :start and u.createdAt < :end
            """)
    long sumCreditsByUserBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select coalesce(sum(u.credits), 0)
            from AiCreditUsage u
            where u.userId = :userId and u.feature = :feature and u.createdAt >= :start and u.createdAt < :end
            """)
    long sumCreditsByUserAndFeatureBetween(
            @Param("userId") Long userId,
            @Param("feature") Feature feature,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    void deleteByUserId(Long userId);
}
