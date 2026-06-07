package com.tradepilot.featureusage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FeatureUsageRepository extends JpaRepository<FeatureUsageEvent, Long> {
    long countByFeatureAndCreatedAtGreaterThanEqual(FeatureKey feature, LocalDateTime start);

    long countDistinctByUserIdAndCreatedAtGreaterThanEqual(Long userId, LocalDateTime start);

    @Query("select count(distinct e.userId) from FeatureUsageEvent e where e.createdAt >= :start")
    long countActiveUsersSince(@Param("start") LocalDateTime start);

    @Query("select count(distinct e.userId) from FeatureUsageEvent e where e.createdAt >= :start and e.createdAt < :end")
    long countActiveUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(distinct e.userId) from FeatureUsageEvent e where e.feature = :feature and e.createdAt >= :start")
    long countUniqueUsersByFeatureSince(@Param("feature") FeatureKey feature, @Param("start") LocalDateTime start);

    List<FeatureUsageEvent> findByCreatedAtGreaterThanEqual(LocalDateTime start);

    List<FeatureUsageEvent> findByUserIdInAndCreatedAtGreaterThanEqual(List<Long> userIds, LocalDateTime start);

    void deleteByUserId(Long userId);

    @Query("""
            select e.userId, count(e)
            from FeatureUsageEvent e
            where e.createdAt >= :start
            group by e.userId
            order by count(e) desc
            """)
    List<Object[]> countByUserSince(@Param("start") LocalDateTime start);

    @Query("""
            select e.feature, count(e), count(distinct e.userId)
            from FeatureUsageEvent e
            where e.createdAt >= :start
            group by e.feature
            """)
    List<Object[]> countByFeatureSince(@Param("start") LocalDateTime start);

    @Query("""
            select cast(e.createdAt as LocalDate), e.feature, count(e)
            from FeatureUsageEvent e
            where e.createdAt >= :start
            group by cast(e.createdAt as LocalDate), e.feature
            order by cast(e.createdAt as LocalDate)
            """)
    List<Object[]> countByDateAndFeatureSince(@Param("start") LocalDateTime start);

    @Query("""
            select cast(e.createdAt as LocalDate), e.userId
            from FeatureUsageEvent e
            where e.createdAt >= :start and e.createdAt < :end
            group by cast(e.createdAt as LocalDate), e.userId
            order by cast(e.createdAt as LocalDate)
            """)
    List<Object[]> distinctUserActivityDays(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select e.userId, max(e.createdAt), count(e)
            from FeatureUsageEvent e
            where e.createdAt >= :start
            group by e.userId
            """)
    List<Object[]> userActivitySummariesSince(@Param("start") LocalDateTime start);
}
