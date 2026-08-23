package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestMetricRepository extends JpaRepository<RequestMetric, Long> {
    long countByCreatedAtGreaterThanEqual(LocalDateTime start);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(avg(r.durationMs), 0) from RequestMetric r where r.createdAt >= :start")
    double averageDurationSince(@Param("start") LocalDateTime start);

    @Query("select coalesce(avg(r.durationMs), 0) from RequestMetric r where r.createdAt >= :start and r.createdAt < :end")
    double averageDurationBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(r) from RequestMetric r where r.createdAt >= :start and r.status >= 500")
    long countErrorsSince(@Param("start") LocalDateTime start);

    @Query("select count(r) from RequestMetric r where r.createdAt >= :start and r.createdAt < :end and r.status >= 500")
    long countErrorsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(r) from RequestMetric r where r.createdAt >= :start and r.createdAt < :end and r.status >= 400")
    long countClientAndServerErrorsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select r.status, count(r)
            from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end and r.status in :statuses
            group by r.status
            order by r.status
            """)
    List<Object[]> errorsByStatusBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("statuses") List<Integer> statuses);

    @Query("""
            select r
            from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end and r.status >= 400
            order by r.createdAt desc
            """)
    List<RequestMetric> recentErrorsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select r
            from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
            order by r.createdAt desc
            """)
    List<RequestMetric> recentBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start order by r.durationMs")
    List<Long> durationsSince(@Param("start") LocalDateTime start);

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start order by r.durationMs asc")
    List<Long> durationPageSince(@Param("start") LocalDateTime start, Pageable pageable);

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start and r.createdAt < :end order by r.durationMs asc")
    List<Long> durationPageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select count(r) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and lower(r.endpoint) not like '%/ai/%'
              and lower(r.endpoint) not like '%/coach/%'
              and lower(r.endpoint) not like '%/sessions/review%'
              and lower(r.endpoint) not like '%/admin/%'
              and lower(r.endpoint) not like '%/analytics/%'
              and lower(r.endpoint) not like '%/stats/%'
              and lower(r.endpoint) not like '%/calendar%'
              and lower(r.endpoint) not like '%/stripe/webhook%'
              and lower(r.endpoint) not like '%/health%'
              and lower(r.endpoint) not like '%/dev/%'
            """)
    long countStandardBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(avg(r.durationMs), 0) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and lower(r.endpoint) not like '%/ai/%'
              and lower(r.endpoint) not like '%/coach/%'
              and lower(r.endpoint) not like '%/sessions/review%'
              and lower(r.endpoint) not like '%/admin/%'
              and lower(r.endpoint) not like '%/analytics/%'
              and lower(r.endpoint) not like '%/stats/%'
              and lower(r.endpoint) not like '%/calendar%'
              and lower(r.endpoint) not like '%/stripe/webhook%'
              and lower(r.endpoint) not like '%/health%'
              and lower(r.endpoint) not like '%/dev/%'
            """)
    double averageStandardBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select r.durationMs from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and lower(r.endpoint) not like '%/ai/%'
              and lower(r.endpoint) not like '%/coach/%'
              and lower(r.endpoint) not like '%/sessions/review%'
              and lower(r.endpoint) not like '%/admin/%'
              and lower(r.endpoint) not like '%/analytics/%'
              and lower(r.endpoint) not like '%/stats/%'
              and lower(r.endpoint) not like '%/calendar%'
              and lower(r.endpoint) not like '%/stripe/webhook%'
              and lower(r.endpoint) not like '%/health%'
              and lower(r.endpoint) not like '%/dev/%'
            order by r.durationMs asc
            """)
    List<Long> durationPageStandardBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select count(r) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/admin/%'
                or lower(r.endpoint) like '%/analytics/%'
                or lower(r.endpoint) like '%/stats/%'
                or lower(r.endpoint) like '%/calendar%')
            """)
    long countAdminReportingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(avg(r.durationMs), 0) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/admin/%'
                or lower(r.endpoint) like '%/analytics/%'
                or lower(r.endpoint) like '%/stats/%'
                or lower(r.endpoint) like '%/calendar%')
            """)
    double averageAdminReportingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select r.durationMs from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/admin/%'
                or lower(r.endpoint) like '%/analytics/%'
                or lower(r.endpoint) like '%/stats/%'
                or lower(r.endpoint) like '%/calendar%')
            order by r.durationMs asc
            """)
    List<Long> durationPageAdminReportingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select count(r) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/stripe/webhook%'
                or lower(r.endpoint) like '%/health%'
                or lower(r.endpoint) like '%/dev/%')
            """)
    long countWebhookSystemBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(avg(r.durationMs), 0) from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/stripe/webhook%'
                or lower(r.endpoint) like '%/health%'
                or lower(r.endpoint) like '%/dev/%')
            """)
    double averageWebhookSystemBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select r.durationMs from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
              and (lower(r.endpoint) like '%/stripe/webhook%'
                or lower(r.endpoint) like '%/health%'
                or lower(r.endpoint) like '%/dev/%')
            order by r.durationMs asc
            """)
    List<Long> durationPageWebhookSystemBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select r.endpoint, count(r), avg(r.durationMs), max(r.durationMs)
            from RequestMetric r
            where r.createdAt >= :start
            group by r.endpoint
            order by max(r.durationMs) desc
            """)
    List<Object[]> slowestEndpoints(@Param("start") LocalDateTime start, Pageable pageable);

    @Query("""
            select r.endpoint, count(r), avg(r.durationMs), max(r.durationMs)
            from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
            group by r.endpoint
            order by max(r.durationMs) desc
            """)
    List<Object[]> slowestEndpointsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select cast(r.createdAt as LocalDate), count(r), avg(r.durationMs),
                   sum(case when r.status >= 500 then 1 else 0 end)
            from RequestMetric r
            where r.createdAt >= :start
            group by cast(r.createdAt as LocalDate)
            order by cast(r.createdAt as LocalDate)
            """)
    List<Object[]> dailyApiStats(@Param("start") LocalDateTime start);

    @Query("""
            select cast(r.createdAt as LocalDate), count(r), avg(r.durationMs),
                   sum(case when r.status >= 500 then 1 else 0 end)
            from RequestMetric r
            where r.createdAt >= :start and r.createdAt < :end
            group by cast(r.createdAt as LocalDate)
            order by cast(r.createdAt as LocalDate)
            """)
    List<Object[]> dailyApiStatsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from RequestMetric r where r.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
