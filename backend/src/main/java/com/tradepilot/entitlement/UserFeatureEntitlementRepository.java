package com.tradepilot.entitlement;

import com.tradepilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserFeatureEntitlementRepository extends JpaRepository<UserFeatureEntitlement, Long> {
    boolean existsByUserAndEntitlementKeyAndEnabledTrue(User user, FeatureEntitlementKey entitlementKey);
    Optional<UserFeatureEntitlement> findByUserAndEntitlementKey(User user, FeatureEntitlementKey entitlementKey);
    List<UserFeatureEntitlement> findByUserOrderByEntitlementKeyAsc(User user);
}
