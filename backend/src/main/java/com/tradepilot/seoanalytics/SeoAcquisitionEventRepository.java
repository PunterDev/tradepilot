package com.tradepilot.seoanalytics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeoAcquisitionEventRepository extends JpaRepository<SeoAcquisitionEvent, Long> {
    boolean existsByEventId(String eventId);
}
