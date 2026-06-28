package com.tradepilot.coach.repository;

import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiCoachSnapshotRepository extends JpaRepository<AiCoachSnapshot, Long> {

    Optional<AiCoachSnapshot> findTopByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            Long userId,
            Long accountId,
            CoachSnapshotType snapshotType,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    Optional<AiCoachSnapshot> findTopByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartAndPeriodEndAndStatusOrderByCreatedAtDesc(
            Long userId,
            Long accountId,
            CoachSnapshotType snapshotType,
            LocalDate periodStart,
            LocalDate periodEnd,
            CoachSnapshotStatus status
    );

    List<AiCoachSnapshot> findTop20ByUserIdAndAccountIdAndSnapshotTypeOrderByPeriodStartDesc(
            Long userId,
            Long accountId,
            CoachSnapshotType snapshotType
    );

    List<AiCoachSnapshot> findByUserIdAndAccountIdAndPeriodStartGreaterThanEqualOrderByPeriodStartDesc(
            Long userId,
            Long accountId,
            LocalDate periodStart
    );

    List<AiCoachSnapshot> findByUserIdAndPeriodStartGreaterThanEqualOrderByPeriodStartDesc(
            Long userId,
            LocalDate periodStart
    );

    List<AiCoachSnapshot> findByUserId(Long userId);

    long countByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            Long userId,
            Long accountId,
            CoachSnapshotType snapshotType,
            LocalDate endDate,
            LocalDate startDate
    );

    long deleteByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            Long userId,
            Long accountId,
            CoachSnapshotType snapshotType,
            LocalDate endDate,
            LocalDate startDate
    );

    void deleteByUserId(Long userId);
}
