package com.tradepilot.activation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserFeaturePreviewRepository extends JpaRepository<UserFeaturePreview, Long> {
    Optional<UserFeaturePreview> findByUserIdAndFeatureKey(Long userId, String featureKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserFeaturePreview> findByUserIdAndFeatureKeyAndStatus(Long userId, String featureKey, UserFeaturePreviewStatus status);

    void deleteByUserId(Long userId);
}
