package com.tradepilot.featureusage;

import com.tradepilot.billing.PlanType;
import com.tradepilot.enums.UserRole;
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

    List<FeatureUsageEvent> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    boolean existsByUserIdAndAction(Long userId, FeatureUsageAction action);

    @Query("""
            select e
            from FeatureUsageEvent e
            join User u on u.id = e.userId
            where coalesce(e.occurredAt, e.createdAt) >= :start
              and coalesce(e.occurredAt, e.createdAt) < :end
              and (:plan is null or u.plan = :plan)
              and (:excludeAdmins = false or u.role <> :adminRole)
              and (:includeTestUsers = true or (
                    lower(u.email) not like '%test%'
                and lower(u.email) not like '%demo%'
                and lower(u.email) not like '%sample%'
                and lower(u.email) not like '%fake%'
                and lower(u.email) not like '%qa%'
                and lower(u.email) not like '%dev%'
                and lower(u.email) not like '%example%'
              ))
            order by e.userId asc, coalesce(e.occurredAt, e.createdAt) asc, e.id asc
            """)
    List<FeatureUsageEvent> findFunnelEventsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("plan") PlanType plan,
            @Param("excludeAdmins") boolean excludeAdmins,
            @Param("includeTestUsers") boolean includeTestUsers,
            @Param("adminRole") UserRole adminRole
    );

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

    @Query("""
            select count(distinct u.id)
            from User u
            where (:plan is null or u.plan = :plan)
              and (:excludeAdmins = false or u.role <> :adminRole)
            """)
    long countEligibleUsers(
            @Param("plan") PlanType plan,
            @Param("excludeAdmins") boolean excludeAdmins,
            @Param("adminRole") UserRole adminRole
    );

    @Query("""
            select e.feature,
                   e.userId,
                   count(e),
                   sum(case when e.eventType in (com.tradepilot.featureusage.FeatureUsageEventType.PAGE_VIEW, com.tradepilot.featureusage.FeatureUsageEventType.ANALYTICS_VIEW, com.tradepilot.featureusage.FeatureUsageEventType.AI_CACHED_READ) then 1 else 0 end),
                   sum(case when e.eventType = com.tradepilot.featureusage.FeatureUsageEventType.AI_GENERATION then 1 else 0 end),
                   min(e.createdAt),
                   max(e.createdAt),
                   count(distinct cast(e.createdAt as LocalDate))
            from FeatureUsageEvent e
            join User u on u.id = e.userId
            where e.createdAt >= :start and e.createdAt < :end
              and (:plan is null or u.plan = :plan)
              and (:eventType is null or e.eventType = :eventType)
              and (:excludeAdmins = false or u.role <> :adminRole)
            group by e.feature, e.userId
            """)
    List<Object[]> countByFeatureAndUserBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("plan") PlanType plan,
            @Param("eventType") FeatureUsageEventType eventType,
            @Param("excludeAdmins") boolean excludeAdmins,
            @Param("adminRole") UserRole adminRole
    );

    @Query("""
            select cast(e.createdAt as LocalDate), e.feature, count(e)
            from FeatureUsageEvent e
            join User u on u.id = e.userId
            where e.createdAt >= :start and e.createdAt < :end
              and (:plan is null or u.plan = :plan)
              and (:eventType is null or e.eventType = :eventType)
              and (:excludeAdmins = false or u.role <> :adminRole)
            group by cast(e.createdAt as LocalDate), e.feature
            order by cast(e.createdAt as LocalDate)
            """)
    List<Object[]> countByDateAndFeatureBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("plan") PlanType plan,
            @Param("eventType") FeatureUsageEventType eventType,
            @Param("excludeAdmins") boolean excludeAdmins,
            @Param("adminRole") UserRole adminRole
    );
}
