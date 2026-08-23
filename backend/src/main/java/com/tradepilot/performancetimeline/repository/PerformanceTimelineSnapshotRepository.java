package com.tradepilot.performancetimeline.repository;

import com.tradepilot.performancetimeline.entity.PerformanceTimelineSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PerformanceTimelineSnapshotRepository extends JpaRepository<PerformanceTimelineSnapshot, Long> {
    Optional<PerformanceTimelineSnapshot> findByUserIdAndAccountIdAndPeriodTypeAndPeriodStart(
            Long userId,
            Long accountId,
            String periodType,
            LocalDate periodStart
    );

    List<PerformanceTimelineSnapshot> findByUserIdAndAccountIdAndPeriodTypeAndPeriodStartBetweenOrderByPeriodStartDesc(
            Long userId,
            Long accountId,
            String periodType,
            LocalDate start,
            LocalDate end
    );
}
