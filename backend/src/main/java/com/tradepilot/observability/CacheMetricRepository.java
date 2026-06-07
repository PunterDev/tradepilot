package com.tradepilot.observability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CacheMetricRepository extends JpaRepository<CacheMetric, Long> {
    @Query("select count(c) from CacheMetric c where c.createdAt >= :start and c.hit = true")
    long hitsSince(@Param("start") LocalDateTime start);

    @Query("select count(c) from CacheMetric c where c.createdAt >= :start and c.createdAt < :end and c.hit = true")
    long hitsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(c) from CacheMetric c where c.createdAt >= :start and c.hit = false")
    long missesSince(@Param("start") LocalDateTime start);

    @Query("select count(c) from CacheMetric c where c.createdAt >= :start and c.createdAt < :end and c.hit = false")
    long missesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCacheNameAndHit(String cacheName, boolean hit);

    @Query("""
            select c.cacheName, sum(case when c.hit = true then 1 else 0 end), sum(case when c.hit = false then 1 else 0 end)
            from CacheMetric c
            where c.createdAt >= :start
            group by c.cacheName
            order by c.cacheName
            """)
    List<Object[]> cacheBreakdownSince(@Param("start") LocalDateTime start);

    @Query("""
            select c.cacheName, sum(case when c.hit = true then 1 else 0 end), sum(case when c.hit = false then 1 else 0 end)
            from CacheMetric c
            where c.createdAt >= :start and c.createdAt < :end
            group by c.cacheName
            order by c.cacheName
            """)
    List<Object[]> cacheBreakdownBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from CacheMetric c where c.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
