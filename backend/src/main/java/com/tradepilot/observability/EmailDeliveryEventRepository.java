package com.tradepilot.observability;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailDeliveryEventRepository extends JpaRepository<EmailDeliveryEvent, Long> {
    @Query("select count(e) from EmailDeliveryEvent e where e.createdAt >= :start and e.success = true")
    long successesSince(@Param("start") LocalDateTime start);

    @Query("select count(e) from EmailDeliveryEvent e where e.createdAt >= :start and e.createdAt < :end and e.success = true")
    long successesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(e) from EmailDeliveryEvent e where e.createdAt >= :start and e.success = false")
    long failuresSince(@Param("start") LocalDateTime start);

    @Query("select count(e) from EmailDeliveryEvent e where e.createdAt >= :start and e.createdAt < :end and e.success = false")
    long failuresBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<EmailDeliveryEvent> findBySuccessFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("select e from EmailDeliveryEvent e where e.success = false and e.createdAt >= :start and e.createdAt < :end order by e.createdAt desc")
    List<EmailDeliveryEvent> findFailuresBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from EmailDeliveryEvent e where e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
