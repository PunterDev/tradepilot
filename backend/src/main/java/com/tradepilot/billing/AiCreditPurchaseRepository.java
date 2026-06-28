package com.tradepilot.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiCreditPurchaseRepository extends JpaRepository<AiCreditPurchase, Long> {
    boolean existsByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<AiCreditPurchase> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}
