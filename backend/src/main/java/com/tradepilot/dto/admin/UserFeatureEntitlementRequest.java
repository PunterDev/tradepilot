package com.tradepilot.dto.admin;

import com.tradepilot.entitlement.FeatureEntitlementKey;

public record UserFeatureEntitlementRequest(
        FeatureEntitlementKey entitlementKey,
        Boolean enabled,
        String reason
) {
}
