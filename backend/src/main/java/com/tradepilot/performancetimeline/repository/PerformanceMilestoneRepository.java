package com.tradepilot.performancetimeline.repository;

import com.tradepilot.performancetimeline.entity.MilestoneType;
import com.tradepilot.performancetimeline.entity.PerformanceMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceMilestoneRepository extends JpaRepository<PerformanceMilestone, Long> {
    List<PerformanceMilestone> findByUserIdAndAccountIdOrderByEarnedDateDesc(Long userId, Long accountId);

    Optional<PerformanceMilestone> findByUserIdAndAccountIdAndMilestoneType(Long userId, Long accountId, MilestoneType milestoneType);
}
