package com.tradepilot.dto.admin;

import com.tradepilot.entitlement.FeatureEntitlementKey;
import java.time.LocalDateTime;

public record UserFeatureEntitlementResponse(
        FeatureEntitlementKey entitlementKey,
        boolean enabled,
        String reason,
        LocalDateTime updatedAt
) {
}
