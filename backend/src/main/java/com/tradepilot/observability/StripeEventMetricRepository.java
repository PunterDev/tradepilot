package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StripeEventMetricRepository extends JpaRepository<StripeEventMetric, Long> {
    @Query("select count(s) from StripeEventMetric s where s.createdAt >= :start and s.success = true")
    long successesSince(@Param("start") LocalDateTime start);

    @Query("select count(s) from StripeEventMetric s where s.createdAt >= :start and s.createdAt < :end and s.success = true")
    long successesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(s) from StripeEventMetric s where s.createdAt >= :start and s.success = false")
    long failuresSince(@Param("start") LocalDateTime start);

    @Query("select count(s) from StripeEventMetric s where s.createdAt >= :start and s.createdAt < :end and s.success = false")
    long failuresBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<StripeEventMetric> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select s from StripeEventMetric s where s.createdAt >= :start and s.createdAt < :end order by s.createdAt desc")
    List<StripeEventMetric> findBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from StripeEventMetric s where s.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
