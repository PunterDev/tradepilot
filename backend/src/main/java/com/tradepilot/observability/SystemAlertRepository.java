package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SystemAlertRepository extends JpaRepository<SystemAlert, Long> {
    List<SystemAlert> findByResolvedFalseOrderByCreatedAtDesc(Pageable pageable);

    List<SystemAlert> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    boolean existsByTypeAndResolvedFalse(String type);

    boolean existsByTypeAndStatus(String type, String status);

    @Query("select a from SystemAlert a where a.createdAt >= :start and a.createdAt < :end order by a.createdAt desc")
    List<SystemAlert> findBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("select a from SystemAlert a where a.status = :status and a.createdAt >= :start and a.createdAt < :end order by a.createdAt desc")
    List<SystemAlert> findByStatusBetween(@Param("status") String status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Modifying
    @Query("delete from SystemAlert a where a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
