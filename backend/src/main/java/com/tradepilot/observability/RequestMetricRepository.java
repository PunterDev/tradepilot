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

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start order by r.durationMs")
    List<Long> durationsSince(@Param("start") LocalDateTime start);

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start order by r.durationMs asc")
    List<Long> durationPageSince(@Param("start") LocalDateTime start, Pageable pageable);

    @Query("select r.durationMs from RequestMetric r where r.createdAt >= :start and r.createdAt < :end order by r.durationMs asc")
    List<Long> durationPageBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

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
