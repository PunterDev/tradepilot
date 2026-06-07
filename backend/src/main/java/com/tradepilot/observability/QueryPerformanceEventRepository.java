package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QueryPerformanceEventRepository extends JpaRepository<QueryPerformanceEvent, Long> {
    List<QueryPerformanceEvent> findByOrderByCreatedAtDesc(Pageable pageable);

    long countByQueryName(String queryName);

    @Modifying
    @Query("delete from QueryPerformanceEvent q where q.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
