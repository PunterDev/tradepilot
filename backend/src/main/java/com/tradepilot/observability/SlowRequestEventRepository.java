package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SlowRequestEventRepository extends JpaRepository<SlowRequestEvent, Long> {
    List<SlowRequestEvent> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select s from SlowRequestEvent s where s.createdAt >= :start and s.createdAt < :end order by s.createdAt desc")
    List<SlowRequestEvent> findBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Modifying
    @Query("delete from SlowRequestEvent s where s.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
