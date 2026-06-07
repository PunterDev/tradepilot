package com.tradepilot.billing;

public class StripeNotConfiguredException extends RuntimeException {
    public StripeNotConfiguredException() {
        super("Stripe is not configured.");
    }
}
