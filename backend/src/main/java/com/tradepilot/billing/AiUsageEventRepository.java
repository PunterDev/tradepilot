package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, Long> {
    long countByUserIdAndUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            UsageType usageType,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByUsageTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UsageType usageType,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
            select e.usageType, count(e)
            from AiUsageEvent e
            where e.createdAt >= :start and e.createdAt < :end
            group by e.usageType
            """)
    List<Object[]> countByUsageTypeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select e.userId, e.usageType, count(e)
            from AiUsageEvent e
            where e.userId in :userIds and e.createdAt >= :start and e.createdAt < :end
            group by e.userId, e.usageType
            """)
    List<Object[]> countByUserIdsAndUsageTypeBetween(
            @Param("userIds") List<Long> userIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    void deleteByUserId(Long userId);
}
