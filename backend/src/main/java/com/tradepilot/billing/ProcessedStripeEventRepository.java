package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, Long> {
    boolean existsByEventId(String eventId);
}
