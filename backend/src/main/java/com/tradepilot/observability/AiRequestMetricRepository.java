package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AiRequestMetricRepository extends JpaRepository<AiRequestMetric, Long> {
    long countByCreatedAtGreaterThanEqual(LocalDateTime start);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(a.totalTokens), 0) from AiRequestMetric a where a.createdAt >= :start")
    long totalTokensSince(@Param("start") LocalDateTime start);

    @Query("select coalesce(sum(a.totalTokens), 0) from AiRequestMetric a where a.createdAt >= :start and a.createdAt < :end")
    long totalTokensBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(a.estimatedCost), 0) from AiRequestMetric a where a.createdAt >= :start")
    BigDecimal totalCostSince(@Param("start") LocalDateTime start);

    @Query("select coalesce(sum(a.estimatedCost), 0) from AiRequestMetric a where a.createdAt >= :start and a.createdAt < :end")
    BigDecimal totalCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select coalesce(avg(a.durationMs), 0) from AiRequestMetric a where a.createdAt >= :start")
    double averageDurationSince(@Param("start") LocalDateTime start);

    @Query("select coalesce(avg(a.durationMs), 0) from AiRequestMetric a where a.createdAt >= :start and a.createdAt < :end")
    double averageDurationBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select a.feature, count(a), coalesce(sum(a.totalTokens), 0), coalesce(sum(a.estimatedCost), 0)
            from AiRequestMetric a
            where a.createdAt >= :start
            group by a.feature
            order by coalesce(sum(a.estimatedCost), 0) desc
            """)
    List<Object[]> featureCostsSince(@Param("start") LocalDateTime start, Pageable pageable);

    @Query("""
            select a.feature, count(a), coalesce(sum(a.totalTokens), 0), coalesce(sum(a.estimatedCost), 0)
            from AiRequestMetric a
            where a.createdAt >= :start and a.createdAt < :end
            group by a.feature
            order by coalesce(sum(a.estimatedCost), 0) desc
            """)
    List<Object[]> featureCostsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select a.model, count(a)
            from AiRequestMetric a
            where a.createdAt >= :start
            group by a.model
            order by count(a) desc
            """)
    List<Object[]> modelUsageSince(@Param("start") LocalDateTime start, Pageable pageable);

    @Query("""
            select a.model, count(a)
            from AiRequestMetric a
            where a.createdAt >= :start and a.createdAt < :end
            group by a.model
            order by count(a) desc
            """)
    List<Object[]> modelUsageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select cast(a.createdAt as LocalDate), coalesce(sum(a.totalTokens), 0), coalesce(sum(a.estimatedCost), 0)
            from AiRequestMetric a
            where a.createdAt >= :start
            group by cast(a.createdAt as LocalDate)
            order by cast(a.createdAt as LocalDate)
            """)
    List<Object[]> dailyAiStats(@Param("start") LocalDateTime start);

    @Query("""
            select cast(a.createdAt as LocalDate), coalesce(sum(a.totalTokens), 0), coalesce(sum(a.estimatedCost), 0)
            from AiRequestMetric a
            where a.createdAt >= :start and a.createdAt < :end
            group by cast(a.createdAt as LocalDate)
            order by cast(a.createdAt as LocalDate)
            """)
    List<Object[]> dailyAiStatsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from AiRequestMetric a where a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
