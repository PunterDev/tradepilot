package com.tradepilot.billing;

import com.tradepilot.entity.User;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {
    private final UsageLimitService usageLimitService;
    private final StripeBillingService stripeBillingService;

    @GetMapping("/usage")
    public BillingUsageResponse usage(@AuthenticationPrincipal User user) {
        return usageLimitService.getUsage(user);
    }

    @PostMapping("/create-checkout-session")
    public CheckoutSessionResponse createCheckoutSession(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "monthly") String billingCycle
    ) throws StripeException {
        return new CheckoutSessionResponse(stripeBillingService.createCheckoutSession(user, billingCycle));
    }

    @PostMapping("/create-portal-session")
    public PortalSessionResponse createPortalSession(@AuthenticationPrincipal User user) throws StripeException {
        return new PortalSessionResponse(stripeBillingService.createPortalSession(user));
    }
}
