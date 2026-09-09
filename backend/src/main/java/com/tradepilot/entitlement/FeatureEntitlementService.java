package com.tradepilot.entitlement;

import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureEntitlementService {
    private final UserFeatureEntitlementRepository entitlementRepository;
    private final UserRepository userRepository;

    public boolean hasEntitlement(User user, FeatureEntitlementKey key) {
        return user != null && entitlementRepository.existsByUserAndEntitlementKeyAndEnabledTrue(user, key);
    }

    public void requireEntitlement(User user, FeatureEntitlementKey key) {
        if (!hasEntitlement(user, key)) {
            throw new AccessDeniedException("Feature entitlement required: " + key.name());
        }
    }

    @Transactional
    public UserFeatureEntitlement setEntitlement(Long targetUserId, FeatureEntitlementKey key, boolean enabled, String reason) {
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserFeatureEntitlement entitlement = entitlementRepository.findByUserAndEntitlementKey(target, key)
                .orElseGet(() -> {
                    UserFeatureEntitlement created = new UserFeatureEntitlement();
                    created.setUser(target);
                    created.setEntitlementKey(key);
                    return created;
                });
        entitlement.setEnabled(enabled);
        entitlement.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        return entitlementRepository.save(entitlement);
    }

    public FeatureEntitlementService(UserFeatureEntitlementRepository entitlementRepository, UserRepository userRepository) {
        this.entitlementRepository = entitlementRepository;
        this.userRepository = userRepository;
    }
}
