package com.tradepilot.seoanalytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAcquisitionAttributionRepository extends JpaRepository<UserAcquisitionAttribution, Long> {
    Optional<UserAcquisitionAttribution> findByUserId(Long userId);
}
